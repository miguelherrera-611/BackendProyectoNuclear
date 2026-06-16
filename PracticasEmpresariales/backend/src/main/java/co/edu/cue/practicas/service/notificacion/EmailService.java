package co.edu.cue.practicas.service.notificacion;

import co.edu.cue.practicas.config.singleton.SystemConfig;
import co.edu.cue.practicas.model.entity.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Servicio de envío de correos vía la API HTTP de SendGrid (v3 Mail Send).
 *
 * Reemplaza el envío por SMTP: Railway (y la mayoría de plataformas con
 * plan gratuito/Hobby) bloquea los puertos SMTP salientes (25/465/587),
 * pero las peticiones HTTPS normales como esta no se ven afectadas por
 * esa restricción.
 *
 * Variable de entorno necesaria: SENDGRID_API_KEY (mapea a sendgrid.api-key
 * en SystemConfig). El remitente (app.mail.from.address) debe coincidir
 * exactamente con el correo verificado como Sender Identity en SendGrid.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    private final SystemConfig systemConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private void enviarRaw(String destinatario, String asunto, String html) throws Exception {
        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of(
                        "to", List.of(Map.of("email", destinatario))
                )),
                "from", Map.of(
                        "email", systemConfig.getMailFromAddress(),
                        "name", systemConfig.getMailFromName()
                ),
                "subject", asunto,
                "content", List.of(Map.of(
                        "type", "text/html",
                        "value", html
                ))
        );

        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SENDGRID_URL))
                .header("Authorization", "Bearer " + systemConfig.getSendgridApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("SendGrid respondió " + response.statusCode() + ": " + response.body());
        }
    }

    private void enviar(String destinatario, String asunto, String html) {
        try {
            enviarRaw(destinatario, asunto, html);
            log.info("[EMAIL] Correo enviado a: {}", destinatario);
        } catch (Exception e) {
            log.error("[EMAIL] Error enviando correo a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarPasswordTemporal(String destinatario, String nombre, String passwordTemporal) {
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
        enviar(destinatario, "Acceso al " + systemConfig.getNombreSistema(), html);
    }

    @Async
    public void enviarCodigoVerificacionCorreo(String destinatario, String nombre, String codigo) {
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
        enviar(destinatario, "Código de verificación — " + systemConfig.getNombreSistema(), html);
    }

    @Async
    public void enviarCodigoLogin(String destinatario, String nombre, String codigo) {
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
        enviar(destinatario, "Código de acceso — " + systemConfig.getNombreSistema(), html);
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
        String asuntoFinal = asunto != null ? asunto : "Notificación de asignación - " + systemConfig.getNombreSistema();

        for (int i = 1; i <= attempts; i++) {
            try {
                enviarRaw(destinatario, asuntoFinal, mensajeHtml);
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