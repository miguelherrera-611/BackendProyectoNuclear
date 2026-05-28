package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * GPE-145 — Request para marcar un estudiante como APTO.
 * El catálogoPracticaId indica con qué plantilla del catálogo se creará
 * la instancia de práctica en el expediente del estudiante.
 */
public record MarcarAptoRequest(

        @NotNull(message = "El ID del catálogo de práctica es obligatorio")
        Long catalogoPracticaId
) {}
