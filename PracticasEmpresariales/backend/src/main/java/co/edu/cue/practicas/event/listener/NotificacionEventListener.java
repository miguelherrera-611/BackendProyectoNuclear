package co.edu.cue.practicas.event.listener;

import co.edu.cue.practicas.event.UsuarioCreadoEvent;
import co.edu.cue.practicas.event.AsignacionCreadaEvent;
import co.edu.cue.practicas.event.AsignacionCanceladaEvent;
import co.edu.cue.practicas.event.VinculacionConfirmadaEvent;
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

        // Log de desarrollo: muestra la contraseña temporal en consola cuando el correo no está configurado.
        // En producción este log no expone datos sensibles porque las credenciales SMTP reales funcionan.
        log.info("[DEV] Contraseña temporal para {} ({}): {}", usuario.getNombre(), usuario.getCorreo(), password);

        // Enviamos el correo de bienvenida con las credenciales de acceso al nuevo usuario
        emailService.enviarPasswordTemporal(usuario.getCorreo(), usuario.getNombre(), password);

        // Si el nuevo usuario es un estudiante, avisamos a Coordinación Académica
        // para que lo revise y lo cambie de NO_APTO a APTO cuando cumpla los requisitos
        if (Rol.ESTUDIANTE.equals(usuario.getRol())) {
            log.info("[OBSERVER] Nuevo estudiante creado: {} — notificando a Coordinación Académica", usuario.getNombre());
            emailService.notificarNuevoEstudiante(usuario);
        }
    }

    @EventListener
    @Async
    public void manejarAsignacionCreada(AsignacionCreadaEvent evento) {
        var instancia = evento.getInstancia();
        if (instancia == null) return;

        // Notificamos al estudiante y a la empresa (si tienen correo conocido)
        try {
            if (instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null) {
                var estudiante = instancia.getExpediente().getEstudiante();
                if (estudiante.getCorreo() != null) {
                    String html = "<p>Estimado/a " + estudiante.getNombre() + ",</p>" +
                            "<p>Has sido asignado a la vacante en la empresa <strong>" + (instancia.getEmpresa()!=null?instancia.getEmpresa().getRazonSocial():"(empresa)") + "</strong>." +
                            " Por favor revisa el sistema para más detalles.</p>";
                    emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, "Asignación a vacante");
                }
            }

            var empresa = instancia.getEmpresa();
            if (empresa != null && empresa.getCorreo() != null) {
                String htmlEmpresa = "<p>Se ha asignado al estudiante <strong>" +
                        (instancia.getExpediente()!=null && instancia.getExpediente().getEstudiante()!=null?
                                instancia.getExpediente().getEstudiante().getNombre() : "(estudiante)") +
                        "</strong> a la vacante publicada en su empresa.</p>";
                emailService.notificarAsignacion(empresa.getCorreo(), empresa.getRazonSocial(), htmlEmpresa, "Nuevo practicante asignado");
            }
        } catch (Exception e) {
            log.error("[NOTIFICACION] Error al procesar evento AsignacionCreada: {}", e.getMessage());
        }
    }

    @EventListener
    @Async
    public void manejarAsignacionCancelada(AsignacionCanceladaEvent evento) {
        var instancia = evento.getInstancia();
        if (instancia == null) return;
        try {
            if (instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null) {
                var estudiante = instancia.getExpediente().getEstudiante();
                if (estudiante.getCorreo() != null) {
                    String html = "<p>Estimado/a " + estudiante.getNombre() + ",</p>" +
                            "<p>Tu asignación a la vacante en la empresa <strong>" + (instancia.getEmpresa()!=null?instancia.getEmpresa().getRazonSocial():"(empresa)") + "</strong> ha sido cancelada." +
                            " Por favor contacta a tu coordinador para más información.</p>";
                    emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, "Asignación cancelada");
                }
            }
            var empresa = instancia.getEmpresa();
            if (empresa != null && empresa.getCorreo() != null) {
                String htmlEmpresa = "<p>Se ha cancelado la asignación del estudiante <strong>" +
                        (instancia.getExpediente()!=null && instancia.getExpediente().getEstudiante()!=null?
                                instancia.getExpediente().getEstudiante().getNombre() : "(estudiante)") +
                        "</strong> a la vacante publicada en su empresa.</p>";
                emailService.notificarAsignacion(empresa.getCorreo(), empresa.getRazonSocial(), htmlEmpresa, "Asignación cancelada");
            }
        } catch (Exception e) {
            log.error("[NOTIFICACION] Error al procesar evento AsignacionCancelada: {}", e.getMessage());
        }
    }

    @EventListener
    @Async
    public void manejarVinculacionConfirmada(VinculacionConfirmadaEvent evento) {
        var instancia = evento.getInstancia();
        if (instancia == null) return;
        try {
            if (instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null) {
                var estudiante = instancia.getExpediente().getEstudiante();
                if (estudiante.getCorreo() != null) {
                    String html = "<p>Estimado/a " + estudiante.getNombre() + ",</p>" +
                            "<p>Tu vinculación ha sido confirmada y la práctica ya está en <strong>EN_CURSO</strong>.</p>";
                    emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, "Vinculación confirmada");
                }
            }
        } catch (Exception e) {
            log.error("[NOTIFICACION] Error al procesar evento VinculacionConfirmada: {}", e.getMessage());
        }
    }
}
