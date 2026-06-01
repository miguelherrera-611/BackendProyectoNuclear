package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPlanRequest {

    @NotBlank(message = "Los objetivos son obligatorios.")
    private String objetivos;

    @NotBlank(message = "El cronograma es obligatorio.")
    private String cronograma;
}
