package co.edu.cue.practicas.event.listener;

import co.edu.cue.practicas.event.AuditoriaEvent;
import co.edu.cue.practicas.model.enums.TipoAccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * PATRON OBSERVER — GPE-139
 *
 * Observador suscrito a todos los eventos de auditoría.
 * Reacciona automáticamente ante cualquier cambio de estado crítico
 * sin que el módulo que generó el evento lo sepa.
 *
 * Escucha AuditoriaEvent, que es publicado por AuditoriaLogger cada vez
 * que se registra una acción en la bitácora.
 *
 * Actualmente genera alertas especiales en el log para dos tipos de eventos:
 *   - ACCESO_NO_AUTORIZADO → alerta de seguridad con usuario, módulo e IP
 *   - LOGIN_FALLIDO        → alerta de autenticación con usuario e IP
 *
 * Se ejecuta de forma asíncrona (@Async) para no bloquear la petición HTTP
 * que originó el evento. El hilo principal termina antes de que este listener ejecute.
 * El hilo asíncrono es gestionado por el pool definido en AsyncConfig.
 */
@Slf4j
@Component
public class AuditoriaEventListener {

    /**
     * Maneja el evento de auditoría publicado por AuditoriaLogger.
     * Analiza el tipo de acción y genera alertas adicionales si es necesario.
     *
     * @param evento  evento con los datos del registro de auditoría recién guardado
     */
    @EventListener
    @Async
    public void manejarEventoAuditoria(AuditoriaEvent evento) {
        var entrada = evento.getEntrada();

        // Alerta especial para intentos de acceso no autorizados.
        // Registramos usuario, módulo e IP para detectar posibles ataques.
        if (TipoAccion.ACCESO_NO_AUTORIZADO.equals(entrada.getTipoAccion())) {
            log.warn("[ALERTA-SEGURIDAD] Intento de acceso no autorizado detectado: usuario={} módulo={} ip={}",
                    entrada.getNombreUsuario(),
                    entrada.getModulo(),
                    entrada.getIpOrigen());
        }

        // Alerta para logins fallidos.
        // En sprints futuros se puede usar para bloquear IPs con muchos intentos fallidos.
        if (TipoAccion.LOGIN_FALLIDO.equals(entrada.getTipoAccion())) {
            log.warn("[ALERTA-AUTH] Login fallido: usuario={} ip={}",
                    entrada.getNombreUsuario(),
                    entrada.getIpOrigen());
        }
    }
}
