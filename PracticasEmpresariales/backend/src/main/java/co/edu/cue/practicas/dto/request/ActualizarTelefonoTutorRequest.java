package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarTelefonoTutorRequest(
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        @Pattern(regexp = "^[+\\d\\s\\-()]+$", message = "Formato de teléfono inválido")
        String telefono
) {}
