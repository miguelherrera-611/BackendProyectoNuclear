package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearVacanteRequest(
        @NotNull(message = "El ID de empresa es obligatorio") Long empresaId,
        @NotBlank(message = "El área es obligatoria") String area,
        @Min(value = 1, message = "Los cupos deben ser al menos 1") int cuposTotales
) {}
