package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CriterioEvaluacionRequest {
    @NotBlank
    private String nombre;
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double peso;
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private double puntaje;
}
