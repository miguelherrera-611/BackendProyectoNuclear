package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditarUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;

    private String fotoPerfil;
}
