package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import org.springframework.context.ApplicationEvent;

/**
 * PATRON OBSERVER — GPE-139
 *
 * Evento publicado cada vez que se registra una entrada en la bitácora.
 * Cualquier componente suscrito (Observer) reacciona automáticamente
 * sin que el módulo origen lo sepa.
 *
 * Lo publica AuditoriaLogger después de guardar cada acción en la BD.
 * Lo escucha AuditoriaEventListener para generar alertas de seguridad
 * en caso de accesos no autorizados o logins fallidos.
 *
 * Al extender ApplicationEvent de Spring, el framework se encarga
 * de distribuir el evento a todos los listeners registrados.
 */
public class AuditoriaEvent extends ApplicationEvent {

    // Datos completos de la entrada que acaba de registrarse en la bitácora
    private final BitacoraAuditoria entrada;

    /**
     * Crea el evento con la entrada de auditoría recién guardada.
     *
     * @param source   objeto que publicó el evento (generalmente AuditoriaLogger)
     * @param entrada  registro completo de la acción auditada
     */
    public AuditoriaEvent(Object source, BitacoraAuditoria entrada) {
        super(source);
        this.entrada = entrada;
    }

    /**
     * Retorna el registro de auditoría asociado a este evento.
     * Los listeners lo usan para decidir si deben generar una alerta.
     */
    public BitacoraAuditoria getEntrada() {
        return entrada;
    }
}
