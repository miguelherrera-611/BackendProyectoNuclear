package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EnviarEncuestaRequest {
    @NotBlank
    private String titulo;
    @NotEmpty
    private List<String> preguntas;
    private Long tutorEmpresarialId;
}
