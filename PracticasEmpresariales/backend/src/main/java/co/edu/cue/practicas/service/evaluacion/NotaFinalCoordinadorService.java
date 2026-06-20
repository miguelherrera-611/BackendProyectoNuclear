package co.edu.cue.practicas.service.evaluacion;

import co.edu.cue.practicas.dto.request.RegistrarNotaFinalRequest;
import co.edu.cue.practicas.dto.response.EvaluacionFinalResponse;
import co.edu.cue.practicas.dto.response.NotaFinalResponse;
import co.edu.cue.practicas.event.Sprint4DomainEvent;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.NotaFinalCoordinador;
import co.edu.cue.practicas.model.enums.EstadoSeguimiento;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.evaluacion.NotaFinalCoordinadorRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.SeguimientoSemanalRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.configuracion.ProgramaConfiguracionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotaFinalCoordinadorService {

    private final NotaFinalCoordinadorRepository notaRepository;
    private final EvaluacionFinalRepository evaluacionRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final SeguimientoSemanalRepository seguimientoRepository;
    private final ProgramaConfiguracionService configuracionService;
    private final ApplicationEventPublisher eventPublisher;
    private final ReglasNotaFinal reglasNotaFinal;

    @Transactional
    public NotaFinalResponse registrar(Long instanciaId, RegistrarNotaFinalRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador registra la nota final.");
        }
        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));
        validarInstanciaEnFacultadDelCoordinador(instancia, actor);
        if (instancia.esInmutable()) {
            throw new OperacionNoPermitidaException("La practica esta cerrada e inmutable.");
        }
        long revisados = seguimientoRepository.countByInstanciaPractica_IdAndEstado(instanciaId, EstadoSeguimiento.REVISADO);
        reglasNotaFinal.validarSeguimientosMinimos(revisados);
        // SPRINT 4 - Strategy: la nota minima se resuelve por programa, permitiendo reglas distintas.
        double minima = configuracionService.notaMinima(instancia.getExpediente().getEstudiante().getPrograma().getId());
        NotaFinalCoordinador nota = notaRepository.findByInstanciaPractica_Id(instanciaId)
                .orElseGet(() -> NotaFinalCoordinador.builder()
                        .instanciaPractica(instancia)
                        .coordinador(actor.getUsuario())
                        .build());
        nota.actualizar(req.getNotaFinal(), minima, req.getObservaciones());
        NotaFinalCoordinador guardada = notaRepository.save(nota);
        // SPRINT 4 - Observer: la nota final dispara actualizacion de indicadores y checklist por evento.
        eventPublisher.publishEvent(new Sprint4DomainEvent(instanciaId, TipoEventoNotificacion.NOTA_FINAL_REGISTRADA));
        return NotaFinalResponse.desde(guardada);
    }

    public Map<String, Object> referencias(Long instanciaId, CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador consulta referencias de cierre.");
        }
        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));
        validarInstanciaEnFacultadDelCoordinador(instancia, actor);
        return Map.of(
                "docente", evaluacionRepository.findByInstanciaPractica_IdAndTipo(instanciaId, TipoEvaluacionFinal.DOCENTE_ASESOR)
                        .map(EvaluacionFinalResponse::desde).orElse(null),
                "tutor", evaluacionRepository.findByInstanciaPractica_IdAndTipo(instanciaId, TipoEvaluacionFinal.TUTOR_EMPRESARIAL)
                        .map(EvaluacionFinalResponse::desde).orElse(null)
        );
    }

    private void validarInstanciaEnFacultadDelCoordinador(InstanciaPractica instancia, CustomUserDetails actor) {
        var estudiante = instancia.getExpediente() != null ? instancia.getExpediente().getEstudiante() : null;
        Long facultadEstudiante = estudiante != null
                && estudiante.getPrograma() != null
                && estudiante.getPrograma().getFacultad() != null
                ? estudiante.getPrograma().getFacultad().getId()
                : null;

        if (actor.getFacultadId() == null || !actor.getFacultadId().equals(facultadEstudiante)) {
            throw new AccesoNoAutorizadoException("No tiene acceso a practicas de otra facultad.");
        }
    }

}
