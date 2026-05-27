package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EstadoVacante;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VacanteResponse(
        Long id, Long empresaId, String razonSocialEmpresa,
        String area, int cuposTotales, int cuposOcupados,
        EstadoVacante estado, LocalDate fechaPublicacion,
        String motivoRechazo, LocalDateTime creadoEn
) {}
