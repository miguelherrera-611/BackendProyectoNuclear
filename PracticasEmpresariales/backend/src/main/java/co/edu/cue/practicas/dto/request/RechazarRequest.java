package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RechazarRequest(
        @NotBlank(message = "El motivo de rechazo es obligatorio") String motivo
) {}
