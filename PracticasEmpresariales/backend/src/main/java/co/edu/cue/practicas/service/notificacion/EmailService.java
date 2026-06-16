package co.edu.cue.practicas.service.notificacion;

import co.edu.cue.practicas.config.singleton.SystemConfig;
import co.edu.cue.practicas.model.entity.Usuario;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correos vía Gmail SMTP con contraseña de aplicación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SystemConfig systemConfig;

    @Async
    public void enviarPasswordTemporal(String destinatario, String nombre, String passwordTemporal) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(systemConfig.getMailFromAddress(), systemConfig.getMailFromName());
            helper.setTo(destinatario);
            helper.setSubject("Acceso al " + systemConfig.getNombreSistema());

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2 style="color: #1a365d;">%s</h2>
                        <p>Estimado/a <strong>%s</strong>,</p>
                        <p>Se ha creado tu cuenta en el <strong>%s</strong> de la <strong>%s</strong>.</p>
                        <div style="background: #f7fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 20px 0;">
                            <p style="margin: 0;"><strong>Correo:</strong> %s</p>
                            <p style="margin: 8px 0 0 0;"><strong>Contraseña temporal:</strong> <code style="background: #edf2f7; padding: 4px 8px; border-radius: 4px; font-size: 16px;">%s</code></p>
                        </div>
                        <p style="color: #e53e3e;"><strong>⚠ Debes cambiar tu contraseña en el primer inicio de sesión.</strong></p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                        <p style="color: #718096; font-size: 12px;">Este es un mensaje automático. No respondas a este correo.</p>
                    </div>
                    """.formatted(
                            systemConfig.getNombreSistema(),
                            nombre,
                            systemConfig.getNombreSistema(),
                            systemConfig.getNombreUniversidad(),
                            destinatario,
                            passwordTemporal
                    );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("[EMAIL] Contraseña temporal enviada a: {}", destinatario);

        } catch (Exception e) {
            log.error("[EMAIL] Error enviando contraseña temporal a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarCodigoVerificacionCorreo(String destinatario, String nombre, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(systemConfig.getMailFromAddress(), systemConfig.getMailFromName());
            helper.setTo(destinatario);
            helper.setSubject("Código de verificación — " + systemConfig.getNombreSistema());

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2 style="color: #1a365d;">%s</h2>
                        <p>Estimado/a <strong>%s</strong>,</p>
                        <p>Se ha solicitado un cambio de correo electrónico en tu cuenta. Usa el siguiente código para confirmar la operación:</p>
                        <div style="background: #f7fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; margin: 20px 0; text-align: center;">
                            <p style="margin: 0; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #1a365d;">%s</p>
                        </div>
                        <p style="color: #718096; font-size: 13px;">Este código es válido por <strong>10 minutos</strong>. Si no solicitaste este cambio, ignora este mensaje.</p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                        <p style="color: #718096; font-size: 12px;">Este es un mensaje automático. No respondas a este correo.</p>
                    </div>
                    """.formatted(systemConfig.getNombreSistema(), nombre, codigo);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("[EMAIL] Código de verificación de correo enviado a: {}", destinatario);

        } catch (Exception e) {
            log.error("[EMAIL] Error enviando código de verificación a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarCodigoLogin(String destinatario, String nombre, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(systemConfig.getMailFromAddress(), systemConfig.getMailFromName());
            helper.setTo(destinatario);
            helper.setSubject("Código de acceso — " + systemConfig.getNombreSistema());

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2 style="color: #1a365d;">%s</h2>
                        <p>Estimado/a <strong>%s</strong>,</p>
                        <p>Se ha solicitado un inicio de sesión en tu cuenta. Usa el siguiente código para completar el acceso:</p>
                        <div style="background: #ebf8ff; border: 2px solid #3182ce; border-radius: 12px; padding: 28px; margin: 24px 0; text-align: center;">
                            <p style="margin: 0 0 8px 0; font-size: 13px; color: #2b6cb0; font-weight: bold; text-transform: uppercase; letter-spacing: 2px;">Código de verificación</p>
                            <p style="margin: 0; font-size: 40px; font-weight: bold; letter-spacing: 12px; color: #1a365d;">%s</p>
                        </div>
                        <p style="color: #718096; font-size: 13px;">Este código es válido por <strong>10 minutos</strong>. Si no intentaste iniciar sesión, ignora este mensaje y considera cambiar tu contraseña.</p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                        <p style="color: #718096; font-size: 12px;">Este es un mensaje automático. No respondas a este correo.</p>
                    </div>
                    """.formatted(systemConfig.getNombreSistema(), nombre, codigo);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("[EMAIL] Código de acceso 2FA enviado a: {}", destinatario);

        } catch (Exception e) {
            log.error("[EMAIL] Error enviando código de acceso 2FA a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void notificarNuevoEstudiante(Usuario estudiante) {
        log.info("[EMAIL] Notificación de nuevo estudiante pendiente: {} → Coordinación Académica", estudiante.getNombre());
        // En Sprint 2 se implementa la consulta de coordinadores por facultad para notificarlos
    }

    @Async
    public void notificarAsignacion(String destinatario, String nombreDestinatario, String mensajeHtml, String asunto) {
        int attempts = systemConfig.getMailRetryAttempts();
        long delayMs = systemConfig.getMailRetryDelayMs();
        for (int i = 1; i <= attempts; i++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(systemConfig.getMailFromAddress(), systemConfig.getMailFromName());
                helper.setTo(destinatario);
                helper.setSubject(asunto != null ? asunto : "Notificación de asignación - " + systemConfig.getNombreSistema());
                helper.setText(mensajeHtml, true);
                mailSender.send(message);
                log.info("[EMAIL] Notificación de asignación enviada a: {} (intento {}/{})", destinatario, i, attempts);
                return;
            } catch (Exception e) {
                log.error("[EMAIL] Error enviando notificación de asignación a {} en intento {}/{}: {}", destinatario, i, attempts, e.getMessage());
                if (i < attempts) {
                    try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }
}
