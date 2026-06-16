package co.edu.cue.practicas.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginPendienteResponse {
    private String correo;
    private String mensaje;
    private int expiresInSeconds;
}
