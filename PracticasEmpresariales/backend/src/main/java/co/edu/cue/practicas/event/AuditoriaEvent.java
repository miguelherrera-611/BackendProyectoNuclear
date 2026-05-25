package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import org.springframework.context.ApplicationEvent;

/**
 * PATRON OBSERVER — GPE-139
 *
 * Evento publicado cada vez que se registra una entrada en la bitácora.
 * Cualquier componente suscrito (Observer) reacciona automáticamente
 * sin que el módulo origen lo sepa.
 */
public class AuditoriaEvent extends ApplicationEvent {

    private final BitacoraAuditoria entrada;

    public AuditoriaEvent(Object source, BitacoraAuditoria entrada) {
        super(source);
        this.entrada = entrada;
    }

    public BitacoraAuditoria getEntrada() {
        return entrada;
    }
}
