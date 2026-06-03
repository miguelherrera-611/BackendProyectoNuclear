package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegistrarSustentacionRequest {
    @NotNull
    @FutureOrPresent
    private LocalDate fecha;
    @NotEmpty
    private List<String> jurados;
}
