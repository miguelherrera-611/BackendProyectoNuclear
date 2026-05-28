package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * GPE-141 — Request para crear una entrada en el catálogo de prácticas.
 * El programaId se obtiene del JWT del usuario autenticado en el service,
 * pero también puede venir explícito para que el DTI pueda configurar
 * catálogos de cualquier programa.
 */
public record CrearCatalogoPracticaRequest(

        @NotNull(message = "El ID del programa es obligatorio")
        Long programaId,

        @Min(value = 1, message = "El número de práctica debe ser al menos 1")
        int numeroPractica,

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
