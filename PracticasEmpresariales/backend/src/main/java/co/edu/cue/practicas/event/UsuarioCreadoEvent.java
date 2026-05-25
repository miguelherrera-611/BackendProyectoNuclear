package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.Usuario;
import org.springframework.context.ApplicationEvent;

/**
 * PATRON OBSERVER — GPE-136, GPE-139
 *
 * Publicado cuando el DTI crea un nuevo estudiante.
 * La Coordinación Académica es notificada automáticamente
 * sin acoplamiento directo entre módulos.
 */
public class UsuarioCreadoEvent extends ApplicationEvent {

    private final Usuario usuario;
    private final String passwordTemporal;

    public UsuarioCreadoEvent(Object source, Usuario usuario, String passwordTemporal) {
        super(source);
        this.usuario = usuario;
        this.passwordTemporal = passwordTemporal;
    }

    public Usuario getUsuario() { return usuario; }
    public String getPasswordTemporal() { return passwordTemporal; }
}
