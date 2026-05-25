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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionEventListener {

    private final EmailService emailService;

    @EventListener
    @Async
    public void manejarUsuarioCreado(UsuarioCreadoEvent evento) {
        var usuario = evento.getUsuario();
        var password = evento.getPasswordTemporal();

        // Enviar contraseña temporal al nuevo usuario
        emailService.enviarPasswordTemporal(usuario.getCorreo(), usuario.getNombre(), password);

        // Si es estudiante, notificar a Coordinación Académica
        if (Rol.ESTUDIANTE.equals(usuario.getRol())) {
            log.info("[OBSERVER] Nuevo estudiante creado: {} — notificando a Coordinación Académica", usuario.getNombre());
            emailService.notificarNuevoEstudiante(usuario);
        }
    }
}
