package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ActualizarCatalogoPracticaRequest(

        @NotBlank(message = "El nombre de la práctica es obligatorio")
        String nombre,

        @NotBlank(message = "La materia núcleo es obligatoria")
        String materiaNucleo,

        @NotBlank(message = "El código de la materia es obligatorio")
        String codigoMateria,

        @Min(value = 1, message = "El número de cortes debe ser al menos 1")
        int numCortes,

        @Min(value = 1, message = "La duración en semanas debe ser al menos 1")
        int duracionSemanas,

        String documentosRequeridos
) {}
