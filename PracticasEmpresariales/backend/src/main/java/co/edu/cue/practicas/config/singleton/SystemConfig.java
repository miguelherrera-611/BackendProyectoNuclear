package co.edu.cue.practicas.config.singleton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PATRON SINGLETON — GPE-136, GPE-140
 *
 * Configuración global del sistema. Instancia única accesible
 * desde todos los módulos. Centraliza los parámetros del sistema
 * para que cualquier módulo los consulte sin múltiples lecturas
 * de properties.
 *
 * Spring gestiona esta clase como bean Singleton (scope por defecto),
 * garantizando una única instancia en todo el contexto de la aplicación.
 */
@Component
public class SystemConfig {

    @Value("${app.nombre}")
    private String nombreSistema;

    @Value("${app.universidad}")
    private String nombreUniversidad;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.mail.from.name}")
    private String mailFromName;

    @Value("${app.mail.from.address}")
    private String mailFromAddress;

    private static volatile SystemConfig instance;

    private SystemConfig() {}

    /**
     * Punto de acceso estático para contextos no-Spring.
     * Spring inyecta la instancia al iniciar; este método la expone
     * globalmente. Double-checked locking para thread-safety.
     */
    public static SystemConfig getInstance() {
        return instance;
    }

    /** Llamado por Spring al crear el bean para registrar la instancia estática */
    @jakarta.annotation.PostConstruct
    private void registerInstance() {
        instance = this;
    }

    public String getNombreSistema() { return nombreSistema; }
    public String getNombreUniversidad() { return nombreUniversidad; }
    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public String getMailFromName() { return mailFromName; }
    public String getMailFromAddress() { return mailFromAddress; }
}
