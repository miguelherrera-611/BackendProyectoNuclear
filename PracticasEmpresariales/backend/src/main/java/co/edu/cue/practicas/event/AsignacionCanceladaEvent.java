package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.InstanciaPractica;
import org.springframework.context.ApplicationEvent;

public class AsignacionCanceladaEvent extends ApplicationEvent {

    private final InstanciaPractica instancia;

    public AsignacionCanceladaEvent(Object source, InstanciaPractica instancia) {
        super(source);
        this.instancia = instancia;
    }

    public InstanciaPractica getInstancia() { return instancia; }
}

