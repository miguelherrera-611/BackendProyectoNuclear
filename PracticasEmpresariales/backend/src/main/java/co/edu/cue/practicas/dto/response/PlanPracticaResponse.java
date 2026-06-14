package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.PlanPractica;
import co.edu.cue.practicas.model.enums.EstadoPlan;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanPracticaResponse {

    private Long id;
    private Long instanciaPracticaId;
    private String objetivos;
    private String cronograma;
    private String documentoNombre;
    private EstadoPlan estado;
    private Long cargadoPorId;
    private LocalDateTime aprobadoPorTutorEn;
    private LocalDateTime aprobadoPorDocenteEn;
    private String motivoRechazo;
    private Long rechazadoPorId;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static PlanPracticaResponse desde(PlanPractica p) {
        return PlanPracticaResponse.builder()
                .id(p.getId())
                .instanciaPracticaId(p.getInstanciaPractica() != null ? p.getInstanciaPractica().getId() : null)
                .objetivos(p.getObjetivos())
                .cronograma(p.getCronograma())
                .documentoNombre(p.getDocumentoNombre())
                .estado(p.getEstado())
                .cargadoPorId(p.getCargadoPorId())
                .aprobadoPorTutorEn(p.getAprobadoPorTutorEn())
                .aprobadoPorDocenteEn(p.getAprobadoPorDocenteEn())
                .motivoRechazo(p.getMotivoRechazo())
                .rechazadoPorId(p.getRechazadoPorId())
                .creadoEn(p.getCreadoEn())
                .actualizadoEn(p.getActualizadoEn())
                .build();
    }
}
