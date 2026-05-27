package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tipo;
    private Long usuarioId;
    private String nombre;
    private String correo;
    private Rol rol;
    private EtiquetaCargo etiquetaCargo;
    private boolean primerIngreso;
    private Long facultadId;
    private Long programaId;
}
