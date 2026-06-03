package co.edu.cue.practicas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RegistrarEvaluacionFinalRequest {
    @Valid
    @NotEmpty
    private List<CriterioEvaluacionRequest> criterios;
    private String observaciones;
}
