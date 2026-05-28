package co.edu.cue.practicas.event.listener;

import co.edu.cue.practicas.event.AptitudCambiadaEvent;
import co.edu.cue.practicas.event.UsuarioCreadoEvent;
import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * PATRÓN OBSERVER — GPE-143, GPE-147
 *
 * Observador que reacciona a eventos del ciclo de vida del estudiante:
 *
 * 1. UsuarioCreadoEvent (rol = ESTUDIANTE):
 *    Crea automáticamente el expediente vacío del estudiante
 *    sin que UsuarioService deba conocer a ExpedienteService.
 *
 * 2. AptitudCambiadaEvent (enviadoAlProceso = true):
 *    Notifica en log que el Coordinador de Prácticas tiene nuevos candidatos.
 *    TODO Sprint 3: integrar con NotificacionService.
 *
 * SOLID — SRP: solo gestiona el ciclo de vida del expediente.
 * SOLID — OCP: nuevos eventos de estudiante → nuevos @EventListener, sin tocar esto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpedienteEventListener {

    private final ExpedienteEstudianteRepository expedienteRepository;

    /**
     * GPE-143 — Al crear un usuario estudiante, genera su expediente vacío.
     * Criterio de Aceptación: "Al crearse se genera automáticamente su expediente de prácticas vacío."
     */
    @EventListener
    @Transactional
    public void onEstudianteCreado(UsuarioCreadoEvent evento) {
        var usuario = evento.getUsuario();
        if (!Rol.ESTUDIANTE.equals(usuario.getRol())) return;
        if (expedienteRepository.existsByEstudiante_Id(usuario.getId())) return;

        ExpedienteEstudiante expediente = ExpedienteEstudiante.builder()
                .estudiante(usuario)
                .creadoEn(LocalDateTime.now())
                .build();
        expedienteRepository.save(expediente);

        log.info("[GPE-143] Expediente vacío creado para estudiante: {} (id: {})",
                usuario.getNombre(), usuario.getId());
    }

    /**
     * GPE-147 — Al enviar APTOS al proceso, notifica al Coordinador de Prácticas.
     */
    @EventListener
    @Async
    public void onAptitudCambiada(AptitudCambiadaEvent evento) {
        if (evento.isEnviadoAlProceso()
                && EstadoEstudiante.APTO.equals(evento.getNuevoEstado())) {
            log.info("[Observer][GPE-147] Estudiante {} enviado al proceso de práctica — " +
                     "Coordinador de Prácticas notificado.",
                    evento.getEstudiante().getNombre());
        }
    }
}
