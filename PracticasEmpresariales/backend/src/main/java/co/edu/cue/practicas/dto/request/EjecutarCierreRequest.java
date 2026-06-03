package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class EjecutarCierreRequest {
    @AssertTrue(message = "Debe confirmar explicitamente el cierre irreversible.")
    private boolean confirmarCierreIrreversible;
}
