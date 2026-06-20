package co.edu.cue.practicas.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginPendienteResponse {
    private String correo;
    private String mensaje;
    private int expiresInSeconds;

    /**
     * Código 2FA en claro, solo presente cuando app.test.expose-codigo-2fa=true.
     * Permite automatizar el login en pruebas de integración (Postman) sin
     * depender del correo real. Debe permanecer en false en producción.
     */
    private String codigoDebug;
}
