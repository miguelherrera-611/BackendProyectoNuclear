package co.edu.cue.practicas.config.singleton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PATRON SINGLETON — GPE-136, GPE-140
 *
 * Configuración global del sistema leída de application.properties.
 * Spring gestiona esta clase como bean singleton por defecto,
 * garantizando una única instancia en todo el contexto de la aplicación.
 *
 * Centraliza los parámetros del sistema para que cualquier módulo
 * los consulte sin acceder directamente a application.properties.
 *
 * Valores configurables en application.properties:
 *   app.nombre            → nombre del sistema mostrado en correos y UI
 *   app.universidad       → nombre de la institución
 *   app.jwt.expiration-ms → duración del token JWT en milisegundos
 *   app.mail.from.name    → nombre visible del remitente en los correos
 *   app.mail.from.address → dirección de correo del remitente (debe ser
 *                           el remitente verificado en SendGrid)
 *   sendgrid.api-key      → API Key de SendGrid (permiso "Mail Send")
 */
@Component
public class SystemConfig {

    // Nombre del sistema tal como aparece en los correos y encabezados (ej. "Sistema de Gestión de Prácticas")
    @Value("${app.nombre}")
    private String nombreSistema;

    // Nombre completo de la universidad para los correos de bienvenida
    @Value("${app.universidad}")
    private String nombreUniversidad;

    // Tiempo de expiración del JWT en milisegundos (86400000 = 24 horas)
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // Nombre del remitente que verá el destinatario en los correos enviados por el sistema
    @Value("${app.mail.from.name}")
    private String mailFromName;

    // Dirección de correo desde la que se envían las notificaciones del sistema
    // (debe coincidir con el remitente verificado en SendGrid)
    @Value("${app.mail.from.address}")
    private String mailFromAddress;

    // Número de reintentos para envío de correo (por defecto 3)
    @Value("${app.mail.retry.attempts:3}")
    private int mailRetryAttempts;

    // Retraso entre reintentos en milisegundos (por defecto 120000 = 2 minutos)
    @Value("${app.mail.retry.delay-ms:120000}")
    private long mailRetryDelayMs;

    // API Key de SendGrid usada para autenticar las peticiones HTTP a /v3/mail/send
    @Value("${sendgrid.api-key:}")
    private String sendgridApiKey;

    public String getNombreSistema() { return nombreSistema; }
    public String getNombreUniversidad() { return nombreUniversidad; }
    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public String getMailFromName() { return mailFromName; }
    public String getMailFromAddress() { return mailFromAddress; }
    public int getMailRetryAttempts() { return mailRetryAttempts; }
    public long getMailRetryDelayMs() { return mailRetryDelayMs; }
    public String getSendgridApiKey() { return sendgridApiKey; }
}