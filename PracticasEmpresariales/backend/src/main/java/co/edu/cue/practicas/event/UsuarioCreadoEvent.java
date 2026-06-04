package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.Usuario;
import org.springframework.context.ApplicationEvent;

public class UsuarioCreadoEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient Usuario usuario;
    private final String passwordTemporal;

    public UsuarioCreadoEvent(Object source, Usuario usuario, String passwordTemporal) {
        super(source);
        this.usuario = usuario;
        this.passwordTemporal = passwordTemporal;
    }

    public Usuario getUsuario() { return usuario; }
    public String getPasswordTemporal() { return passwordTemporal; }
}