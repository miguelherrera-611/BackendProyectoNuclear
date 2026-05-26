package co.edu.cue.practicas.event.listener;

import co.edu.cue.practicas.event.UsuarioCreadoEvent;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.service.notificacion.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * PATRON OBSERVER — GPE-136, GPE-139
 *
 * Observador que reacciona al evento de creación de usuario:
 * 1. Envía correo con contraseña temporal al nuevo usuario.
 * 2. Si el usuario es ESTUDIANTE, notifica a la Coordinación Académica
 *    que hay un nuevo estudiante pendiente de validación.
 *
 * Gracias al patrón Observer, UsuarioService no necesita conocer a EmailService.
 * Simplemente publica el evento y este listener lo procesa de forma desacoplada.
 *
 * Se ejecuta de forma asíncrona (@Async) para no hacer esperar al DTI
 * mientras se envía el correo. El hilo principal responde de inmediato
 * y el correo se envía en segundo plano.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionEventListener {

    // Servicio encargado de enviar los correos a través de Gmail SMTP
    private final EmailService emailService;

    /**
     * Maneja el evento de usuario creado publicado por UsuarioService.
     *
     * Siempre envía el correo con la contraseña temporal al nuevo usuario.
     * Adicionalmente, si el usuario creado es un ESTUDIANTE, notifica
     * a la Coordinación Académica para que lo revise y lo valide (cambie a APTO).
     *
     * @param evento  evento con el usuario creado y su contraseña temporal en texto plano
     */
    @EventListener
    @Async
    public void manejarUsuarioCreado(UsuarioCreadoEvent evento) {
        var usuario = evento.getUsuario();
        var password = evento.getPasswordTemporal();

        // Enviamos el correo de bienvenida con las credenciales de acceso al nuevo usuario
        emailService.enviarPasswordTemporal(usuario.getCorreo(), usuario.getNombre(), password);

        // Si el nuevo usuario es un estudiante, avisamos a Coordinación Académica
        // para que lo revise y lo cambie de NO_APTO a APTO cuando cumpla los requisitos
        if (Rol.ESTUDIANTE.equals(usuario.getRol())) {
            log.info("[OBSERVER] Nuevo estudiante creado: {} — notificando a Coordinación Académica", usuario.getNombre());
            emailService.notificarNuevoEstudiante(usuario);
        }
    }
}
