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
public class CancelarAsignacionRequest {

    @NotBlank(message = "El motivo de cancelación es obligatorio.")
    private String motivo;
}
