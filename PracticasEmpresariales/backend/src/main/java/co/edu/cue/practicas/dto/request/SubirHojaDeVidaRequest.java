package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * GPE-146 — Request para subir una nueva versión de la Hoja de Vida.
 * Cada envío crea una nueva versión; la anterior se conserva en el historial.
 */
public record SubirHojaDeVidaRequest(

        @NotBlank(message = "La URL del archivo es obligatoria")
        String urlArchivo
) {}
