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
 */
@Slf4j
@Component
public class AuditoriaEventListener {

    @EventListener
    @Async
    public void manejarEventoAuditoria(AuditoriaEvent evento) {
        var entrada = evento.getEntrada();

        // Alerta especial para accesos no autorizados
        if (TipoAccion.ACCESO_NO_AUTORIZADO.equals(entrada.getTipoAccion())) {
            log.warn("[ALERTA-SEGURIDAD] Intento de acceso no autorizado detectado: usuario={} módulo={} ip={}",
                    entrada.getNombreUsuario(),
                    entrada.getModulo(),
                    entrada.getIpOrigen());
        }

        // Alerta para logins fallidos
        if (TipoAccion.LOGIN_FALLIDO.equals(entrada.getTipoAccion())) {
            log.warn("[ALERTA-AUTH] Login fallido: usuario={} ip={}",
                    entrada.getNombreUsuario(),
                    entrada.getIpOrigen());
        }
    }
}
