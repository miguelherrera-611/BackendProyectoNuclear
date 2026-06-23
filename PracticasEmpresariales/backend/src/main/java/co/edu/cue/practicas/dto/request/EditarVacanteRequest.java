package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record EditarVacanteRequest(
        @NotBlank(message = "El área es obligatoria") String area,
        @Min(value = 1, message = "Los cupos deben ser al menos 1") int cuposTotales
) {}
