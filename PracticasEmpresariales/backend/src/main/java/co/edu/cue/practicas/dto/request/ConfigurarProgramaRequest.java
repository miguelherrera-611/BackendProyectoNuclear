package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ConfigurarProgramaRequest {
    @Min(1)
    private int numeroPracticas;
    @Min(1)
    private int semanasSeguimiento;
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private double notaMinimaAprobacion;
    private String requisitosCierre;
    @Min(1)
    private int umbralInactividadDias;
}
