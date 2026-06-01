package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.SeguimientoSemanal;
import co.edu.cue.practicas.model.enums.EstadoSeguimiento;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeguimientoSemanalResponse {

    private Long id;
    private Long instanciaPracticaId;
    private int semana;
    private String actividades;
    private String logros;
    private String dificultades;
    private String evidencias;
    private String observacionesDocente;
    private EstadoSeguimiento estado;
    private Long creadoPorId;
    private Long revisadoPorId;
    private LocalDateTime revisadoEn;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static SeguimientoSemanalResponse desde(SeguimientoSemanal s) {
        return SeguimientoSemanalResponse.builder()
                .id(s.getId())
                .instanciaPracticaId(s.getInstanciaPractica() != null ? s.getInstanciaPractica().getId() : null)
                .semana(s.getSemana())
                .actividades(s.getActividades())
                .logros(s.getLogros())
                .dificultades(s.getDificultades())
                .evidencias(s.getEvidencias())
                .observacionesDocente(s.getObservacionesDocente())
                .estado(s.getEstado())
                .creadoPorId(s.getCreadoPorId())
                .revisadoPorId(s.getRevisadoPorId())
                .revisadoEn(s.getRevisadoEn())
                .creadoEn(s.getCreadoEn())
                .actualizadoEn(s.getActualizadoEn())
                .build();
    }
}
