package co.edu.cue.practicas.service.cierre;

import co.edu.cue.practicas.dto.request.EjecutarCierreRequest;
import co.edu.cue.practicas.dto.response.CierreFormalResponse;
import co.edu.cue.practicas.event.Sprint4DomainEvent;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.NotaFinalCoordinador;
import co.edu.cue.practicas.model.entity.PazYSalvo;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.ResultadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.cierre.PazYSalvoRepository;
import co.edu.cue.practicas.repository.evaluacion.NotaFinalCoordinadorRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.notificacion.NotificacionConfigurableService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CierreFormalFacade {

    private final InstanciaPracticaRepository instanciaRepository;
    private final NotaFinalCoordinadorRepository notaRepository;
    private final PazYSalvoRepository pazYSalvoRepository;
    private final ChecklistCierreService checklistService;
    private final NotificacionConfigurableService notificacionService;
    private final ApplicationEventPublisher eventPublisher;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public CierreFormalResponse ejecutar(Long instanciaId, EjecutarCierreRequest req, CustomUserDetails actor) {
        // SPRINT 4 - Facade: orquesta validar checklist -> finalizar estado -> paz y salvo -> notificar actores.
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador puede ejecutar el cierre formal.");
        }
        if (!req.isConfirmarCierreIrreversible()) {
            throw new OperacionNoPermitidaException("El cierre requiere confirmacion explicita.");
        }
        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));
        validarScope(instancia, actor);
        if (instancia.getEstado() != EstadoPractica.EN_CURSO) {
            throw new OperacionNoPermitidaException("Solo una practica EN_CURSO puede cerrarse formalmente.");
        }
        if (!checklistService.generar(instanciaId, actor).isPuedeEjecutarCierre()) {
            throw new OperacionNoPermitidaException("No se puede ejecutar cierre: checklist incompleto.");
        }
        NotaFinalCoordinador nota = notaRepository.findByInstanciaPractica_Id(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nota final no registrada."));
        // SPRINT 4 - State: EN_CURSO -> FINALIZADA con resultado irreversible.
        instancia.finalizarConResultado(nota.getResultado());
        instanciaRepository.save(instancia);

        String codigoPazYSalvo = null;
        String contenidoPazYSalvo = null;
        if (nota.getResultado() == ResultadoPractica.APROBADO) {
            contenidoPazYSalvo = construirPazYSalvo(instancia, nota);
            String contenidoGenerado = contenidoPazYSalvo;
            PazYSalvo pazYSalvo = pazYSalvoRepository.findByInstanciaPractica_Id(instanciaId)
                    .orElseGet(() -> PazYSalvo.builder()
                            .instanciaPractica(instancia)
                            .codigo("PYS-" + instanciaId + "-" + System.currentTimeMillis())
                            .contenido(contenidoGenerado)
                            .build());
            pazYSalvo.setContenido(contenidoPazYSalvo);
            codigoPazYSalvo = pazYSalvoRepository.save(pazYSalvo).getCodigo();
        }

        // SPRINT 4 - Mediator: coordina notificaciones a estudiante, docente, tutor y Coordinacion Academica.
        notificarActores(instancia, nota);
        // SPRINT 4 - Observer: cierre formal dispara refresco de dashboard/reportes en consultas posteriores.
        eventPublisher.publishEvent(new Sprint4DomainEvent(instanciaId, TipoEventoNotificacion.CIERRE_FORMAL_EJECUTADO));
        return CierreFormalResponse.builder()
                .instanciaPracticaId(instanciaId)
                .estado(instancia.getEstado())
                .resultado(nota.getResultado())
                .notaFinal(nota.getNotaFinal())
                .codigoPazYSalvo(codigoPazYSalvo)
                .pazYSalvo(contenidoPazYSalvo)
                .build();
    }

    private void validarScope(InstanciaPractica instancia, CustomUserDetails actor) {
        Long programaId = instancia.getExpediente().getEstudiante().getPrograma().getId();
        if (actor.getProgramaId() != null && !actor.getProgramaId().equals(programaId)) {
            throw new AccesoNoAutorizadoException("No puedes cerrar practicas de otro programa.");
        }
    }

    private void notificarActores(InstanciaPractica instancia, NotaFinalCoordinador nota) {
        Map<String, String> vars = Map.of(
                "nombre_estudiante", instancia.getExpediente().getEstudiante().getNombre(),
                "empresa", instancia.getEmpresa() != null ? instancia.getEmpresa().getRazonSocial() : "",
                "nombre_practica", instancia.getNombre(),
                "enlace_encuesta", "",
                "resultado", nota.getResultado().name(),
                "nota_final", String.valueOf(nota.getNotaFinal())
        );
        var estudiante = instancia.getExpediente().getEstudiante();
        notificacionService.enviar(TipoEventoNotificacion.CIERRE_FORMAL_EJECUTADO, estudiante.getId(), estudiante.getCorreo(), estudiante.getNombre(), vars);
        if (instancia.getDocenteAsesor() != null) {
            notificacionService.enviar(TipoEventoNotificacion.CIERRE_FORMAL_EJECUTADO, instancia.getDocenteAsesor().getId(),
                    instancia.getDocenteAsesor().getCorreo(), instancia.getDocenteAsesor().getNombre(), vars);
        }
        if (instancia.getTutorEmpresarial() != null) {
            notificacionService.enviar(TipoEventoNotificacion.CIERRE_FORMAL_EJECUTADO, instancia.getTutorEmpresarial().getId(),
                    instancia.getTutorEmpresarial().getCorreo(), instancia.getTutorEmpresarial().getNombre(), vars);
        }
        notificarCoordinacionAcademica(instancia, nota, vars);
    }

    private void notificarCoordinacionAcademica(InstanciaPractica instancia, NotaFinalCoordinador nota, Map<String, String> vars) {
        var estudiante = instancia.getExpediente().getEstudiante();
        if (estudiante.getPrograma() != null && estudiante.getPrograma().getFacultad() != null) {
            Long facultadId = estudiante.getPrograma().getFacultad().getId();
            usuarioRepository.findByRolAndFacultad_IdAndActivoTrue(Rol.COORDINACION_ACADEMICA, facultadId)
                    .forEach(coordinacion -> notificacionService.enviar(
                            TipoEventoNotificacion.COORDINACION_ACADEMICA_RESULTADO,
                            coordinacion.getId(),
                            coordinacion.getCorreo(),
                            coordinacion.getNombre(),
                            vars));
        }
    }

    private String construirPazYSalvo(InstanciaPractica instancia, NotaFinalCoordinador nota) {
        String estudiante = instancia.getExpediente().getEstudiante().getNombre();
        String empresa = instancia.getEmpresa() != null ? instancia.getEmpresa().getRazonSocial() : "empresa registrada";
        return "PAZ Y SALVO PRACTICA EMPRESARIAL\n"
                + "Estudiante: " + estudiante + "\n"
                + "Practica: " + instancia.getNombre() + "\n"
                + "Empresa: " + empresa + "\n"
                + "Resultado: " + nota.getResultado() + "\n"
                + "Nota final: " + nota.getNotaFinal() + "\n"
                + "Fecha cierre: " + instancia.getFechaCierre() + "\n"
                + "Se certifica cierre satisfactorio del expediente de practica.";
    }
}
