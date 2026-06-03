package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ResponderEncuestaRequest {
    @NotEmpty
    private List<String> respuestas;
}
