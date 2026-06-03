package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class RegistrarNotaFinalRequest {
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private double notaFinal;
    private String observaciones;
}
