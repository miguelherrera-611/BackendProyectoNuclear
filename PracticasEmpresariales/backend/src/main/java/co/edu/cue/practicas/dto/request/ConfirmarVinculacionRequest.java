package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ConfirmarVinculacionRequest(
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        @NotNull Boolean firmaTutor,
        @NotNull Boolean firmaDocente,
        @NotNull Boolean firmaEstudiante,
        Long docenteAsesorId
) {}

