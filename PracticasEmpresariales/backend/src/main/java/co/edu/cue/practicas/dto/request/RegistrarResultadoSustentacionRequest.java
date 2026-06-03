package co.edu.cue.practicas.dto.request;

import co.edu.cue.practicas.model.enums.ResultadoSustentacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrarResultadoSustentacionRequest {
    @NotNull
    private ResultadoSustentacion resultado;
    @NotBlank
    private String actaUrl;
    private boolean actaFirmada;
}
