package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * GPE-147 — Request para enviar uno o varios estudiantes APTOS al proceso de práctica.
 * Solo estudiantes en estado APTO pueden enviarse al proceso.
 * Una vez enviados, el Coordinador de Prácticas puede verlos.
 */
public record EnviarAlProcesoRequest(

        @NotEmpty(message = "Debe seleccionar al menos un estudiante")
        List<Long> estudianteIds
) {}
