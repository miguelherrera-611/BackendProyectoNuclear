package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import org.springframework.context.ApplicationEvent;

/**
 * PATRÓN OBSERVER — GPE-145, GPE-147
 *
 * Publicado por EstudianteService cuando cambia el estado de aptitud de un estudiante
 * (NO_APTO → APTO) o cuando un conjunto de APTOS es enviado al proceso de práctica.
 *
 * Lo escucha:
 *   - AptitudCambiadaListener: notifica al Coordinador de Prácticas cuando
 *     estudiantes APTOS son enviados al proceso, indicando que tiene nuevos candidatos.
 */
public class AptitudCambiadaEvent extends ApplicationEvent {

    private final Usuario estudiante;
    private final EstadoEstudiante nuevoEstado;
    private final boolean enviadoAlProceso;

    public AptitudCambiadaEvent(Object source, Usuario estudiante,
                                 EstadoEstudiante nuevoEstado, boolean enviadoAlProceso) {
        super(source);
        this.estudiante = estudiante;
        this.nuevoEstado = nuevoEstado;
        this.enviadoAlProceso = enviadoAlProceso;
    }

    public Usuario getEstudiante() { return estudiante; }
    public EstadoEstudiante getNuevoEstado() { return nuevoEstado; }
    public boolean isEnviadoAlProceso() { return enviadoAlProceso; }
}
