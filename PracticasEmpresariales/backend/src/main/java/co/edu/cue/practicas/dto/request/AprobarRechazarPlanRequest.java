package co.edu.cue.practicas.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprobarRechazarPlanRequest {

    /** Motivo obligatorio cuando la acción es rechazar */
    private String motivo;
}
