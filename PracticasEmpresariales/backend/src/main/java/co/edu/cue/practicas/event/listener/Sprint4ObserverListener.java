package co.edu.cue.practicas.event.listener;

import co.edu.cue.practicas.event.Sprint4DomainEvent;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.evaluacion.NotaFinalCoordinadorRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.service.notificacion.NotificacionConfigurableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class Sprint4ObserverListener {

    private final NotificacionConfigurableService notificacionService;
    private final InstanciaPracticaRepository instanciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotaFinalCoordinadorRepository notaRepository;

    // SPRINT 4 - Observer: reacciona a eventos de dominio y dispara las notificaciones
    // que los servicios originadores no gestionan directamente (SRP).
    @EventListener
    public void onSprint4Event(Sprint4DomainEvent event) {
        log.info("[SPRINT4][OBSERVER] Evento {} para practica {}.",
                event.tipoEvento(), event.instanciaPracticaId());
        switch (event.tipoEvento()) {
            case EVALUACION_DOCENTE_COMPLETADA, EVALUACION_TUTOR_COMPLETADA ->
                // Avisa al coordinador de practicas que puede continuar con el siguiente paso.
                notificarCoordinadoresPorPrograma(event.instanciaPracticaId(), event.tipoEvento());
            case NOTA_FINAL_REGISTRADA ->
                // Informa al estudiante, docente y tutor que la nota definitiva fue registrada.
                notificarActoresNotaFinal(event.instanciaPracticaId());
            case ENCUESTA_COMPLETADA ->
                // Avisa al coordinador que la encuesta fue diligenciada y el checklist avanzó.
                notificarCoordinadoresPorPrograma(event.instanciaPracticaId(), event.tipoEvento());
            default -> {
                // CIERRE_FORMAL_EJECUTADO, COORDINACION_ACADEMICA_RESULTADO,
                // ENCUESTA_TUTOR_ENVIADA y ENCUESTA_ESTUDIANTE_ENVIADA ya envian
                // su correo directamente desde el servicio que los origina.
            }
        }
    }

    private void notificarCoordinadoresPorPrograma(Long instanciaId, TipoEventoNotificacion tipo) {
        instanciaRepository.findById(instanciaId).ifPresent(instancia -> {
            Long programaId = instancia.getExpediente().getEstudiante().getPrograma().getId();
            Map<String, String> vars = buildVars(instancia, "");
            usuarioRepository.findByRolAndPrograma_IdAndActivoTrue(Rol.COORDINADOR_PRACTICAS, programaId)
                    .forEach(coord -> notificacionService.enviar(
                            tipo, coord.getId(), coord.getCorreo(), coord.getNombre(), vars));
        });
    }

    private void notificarActoresNotaFinal(Long instanciaId) {
        instanciaRepository.findById(instanciaId).ifPresent(instancia -> {
            var notaOpt = notaRepository.findByInstanciaPractica_Id(instanciaId);
            Map<String, String> vars = Map.of(
                    "nombre_estudiante", instancia.getExpediente().getEstudiante().getNombre(),
                    "empresa",           instancia.getEmpresa() != null ? instancia.getEmpresa().getRazonSocial() : "",
                    "nombre_practica",   instancia.getNombre(),
                    "enlace_encuesta",   "",
                    "resultado",         notaOpt.map(n -> n.getResultado().name()).orElse(""),
                    "nota_final",        notaOpt.map(n -> String.valueOf(n.getNotaFinal())).orElse("")
            );
            var estudiante = instancia.getExpediente().getEstudiante();
            notificacionService.enviar(TipoEventoNotificacion.NOTA_FINAL_REGISTRADA,
                    estudiante.getId(), estudiante.getCorreo(), estudiante.getNombre(), vars);
            if (instancia.getDocenteAsesor() != null) {
                notificacionService.enviar(TipoEventoNotificacion.NOTA_FINAL_REGISTRADA,
                        instancia.getDocenteAsesor().getId(),
                        instancia.getDocenteAsesor().getCorreo(),
                        instancia.getDocenteAsesor().getNombre(), vars);
            }
            if (instancia.getTutorEmpresarial() != null) {
                notificacionService.enviar(TipoEventoNotificacion.NOTA_FINAL_REGISTRADA,
                        instancia.getTutorEmpresarial().getId(),
                        instancia.getTutorEmpresarial().getCorreo(),
                        instancia.getTutorEmpresarial().getNombre(), vars);
            }
        });
    }

    private Map<String, String> buildVars(InstanciaPractica instancia, String enlace) {
        return Map.of(
                "nombre_estudiante", instancia.getExpediente().getEstudiante().getNombre(),
                "empresa",           instancia.getEmpresa() != null ? instancia.getEmpresa().getRazonSocial() : "",
                "nombre_practica",   instancia.getNombre(),
                "enlace_encuesta",   enlace,
                "resultado",         "",
                "nota_final",        ""
        );
    }
}
