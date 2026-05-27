package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.Usuario;
import org.springframework.context.ApplicationEvent;

/**
 * PATRON OBSERVER — GPE-136, GPE-139
 *
 * Publicado cuando el DTI crea un nuevo usuario en el sistema.
 * La Coordinación Académica es notificada automáticamente
 * sin acoplamiento directo entre módulos.
 *
 * Lo publica UsuarioService justo después de persistir el usuario en la BD.
 * Lo escucha NotificacionEventListener, que se encarga de:
 *   1. Enviar el correo con la contraseña temporal al nuevo usuario.
 *   2. Notificar a Coordinación Académica si el nuevo usuario es un ESTUDIANTE.
 *
 * Al extender ApplicationEvent de Spring, el framework distribuye el evento
 * a todos los listeners de forma desacoplada (UsuarioService no conoce a EmailService).
 */
public class UsuarioCreadoEvent extends ApplicationEvent {

    // Objeto usuario recién creado con todos sus datos
    private final Usuario usuario;

    // Contraseña temporal en texto plano, necesaria para incluirla en el correo de bienvenida
    // Solo existe en memoria durante este evento; en BD ya está guardada como hash
    private final String passwordTemporal;

    /**
     * Crea el evento con los datos del usuario recién creado.
     *
     * @param source            objeto que publicó el evento (UsuarioService)
     * @param usuario           usuario creado y ya persistido en la BD
     * @param passwordTemporal  contraseña en texto plano para enviar por correo
     */
    public UsuarioCreadoEvent(Object source, Usuario usuario, String passwordTemporal) {
        super(source);
        this.usuario = usuario;
        this.passwordTemporal = passwordTemporal;
    }

    /** Retorna el usuario creado para que el listener pueda acceder a su correo y nombre. */
    public Usuario getUsuario() { return usuario; }

    /** Retorna la contraseña temporal en texto plano para incluirla en el correo de bienvenida. */
    public String getPasswordTemporal() { return passwordTemporal; }
}
