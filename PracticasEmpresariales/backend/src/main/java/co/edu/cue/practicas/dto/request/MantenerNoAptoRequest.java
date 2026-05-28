package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * GPE-145 — Request para mantener un estudiante en NO_APTO con motivo obligatorio.
 * OCL: si el estudiante no cumple requisitos, el motivo del rechazo es obligatorio.
 */
public record MantenerNoAptoRequest(

        @NotBlank(message = "El motivo es obligatorio cuando el estudiante no cumple los requisitos")
        String motivo
) {}
