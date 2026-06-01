package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.InstanciaPractica;
import org.springframework.context.ApplicationEvent;

/** Evento publicado cuando se crea una asignación (InstanciaPractica) */
public class AsignacionCreadaEvent extends ApplicationEvent {

	private final InstanciaPractica instancia;

	public AsignacionCreadaEvent(Object source, InstanciaPractica instancia) {
		super(source);
		this.instancia = instancia;
	}

	public InstanciaPractica getInstancia() { return instancia; }
}


