package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditarProgramaRequest {

    @NotBlank(message = "El nombre del programa es obligatorio")
    private String nombre;

    private String descripcion;

    @Min(value = 1, message = "Debe tener al menos 1 práctica")
    private int numeroTotalPracticas = 1;

    private double promedioMinimoGeneral = 3.0;
}
