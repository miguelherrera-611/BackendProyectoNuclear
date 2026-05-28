package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EstadoHojaDeVida;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HojaDeVidaResponse {

    private Long id;
    private Long estudianteId;
    private int version;
    private LocalDate fechaCarga;
    private String urlArchivo;
    private EstadoHojaDeVida estado;
    private Long validadoPor;
    private LocalDate fechaValidacion;
    private String motivoRechazo;
    private LocalDateTime creadoEn;
}
