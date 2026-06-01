package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearSeguimientoRequest {

    @Min(value = 1, message = "La semana debe ser mayor a cero.")
    private int semana;

    @NotBlank(message = "Las actividades son obligatorias.")
    private String actividades;

    @NotBlank(message = "Los logros son obligatorios.")
    private String logros;

    private String dificultades;

    /** URLs de evidencias separadas por coma */
    private String evidencias;
}
