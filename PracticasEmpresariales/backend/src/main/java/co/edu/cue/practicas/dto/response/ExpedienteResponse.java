package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpedienteResponse {

    private Long expedienteId;
    private Long estudianteId;
    private String nombreEstudiante;
    private String identificacion;
    private String programa;
    private Integer semestre;
    private EstadoEstudiante estadoEstudiante;

    /** Versión más reciente de la HV */
    private HojaDeVidaResponse hvActual;

    /** Historial completo de versiones de HV (ordenado de más reciente a más antiguo) */
    private List<HojaDeVidaResponse> historialHv;

    /** Historial completo de prácticas del estudiante */
    private List<InstanciaPracticaResponse> practicas;

    private LocalDateTime creadoEn;
}
