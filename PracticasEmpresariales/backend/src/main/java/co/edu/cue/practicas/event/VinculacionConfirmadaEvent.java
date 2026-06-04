package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.InstanciaPractica;
import org.springframework.context.ApplicationEvent;

public class VinculacionConfirmadaEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient InstanciaPractica instancia;

    public VinculacionConfirmadaEvent(Object source, InstanciaPractica instancia) {
        super(source);
        this.instancia = instancia;
    }

    public InstanciaPractica getInstancia() { return instancia; }
}