package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrearFacultadRequest {

    @NotBlank(message = "El nombre de la facultad es obligatorio")
    private String nombre;

    private String descripcion;
}
