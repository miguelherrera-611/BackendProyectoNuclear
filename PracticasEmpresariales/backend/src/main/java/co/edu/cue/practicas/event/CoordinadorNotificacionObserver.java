package co.edu.cue.practicas.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PATRÓN OBSERVER — Observador concreto: notificación al coordinador.
 *
 * SOLID — OCP: se agrega este nuevo observer sin modificar Empresa
 *              ni EmpresaService. Solo se registra como @Component
 *              y Spring lo inyecta automáticamente en la lista.
 *
 * SOLID — SRP: responsabilidad única → notificar al coordinador.
 *
 * TODO Sprint 3: integrar con el módulo de Comunicación/Notificaciones.
 */
@Slf4j
@Component
public class CoordinadorNotificacionObserver implements EmpresaObserver {

    @Override
    public void onEmpresaEvento(Long empresaId, String evento) {
        log.info("[Observer] Notificando coordinador → empresa: {}, evento: {}", empresaId, evento);
        // TODO: NotificacionService.enviar(empresaId, evento)
    }
}
