package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AgendarSustentacionDocenteRequest {

    @NotNull(message = "La fecha de sustentacion es obligatoria.")
    private LocalDate fecha;
}
