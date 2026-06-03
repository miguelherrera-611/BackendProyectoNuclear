package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.CriterioEvaluacion;
import co.edu.cue.practicas.model.entity.EvaluacionFinal;
import co.edu.cue.practicas.model.enums.EstadoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EvaluacionFinalResponse {
    private Long id;
    private Long instanciaPracticaId;
    private TipoEvaluacionFinal tipo;
    private Long evaluadorId;
    private String evaluadorNombre;
    private List<CriterioEvaluacion> criterios;
    private double promedioFinal;
    private String observaciones;
    private EstadoEvaluacionFinal estado;
    private LocalDateTime fecha;

    public static EvaluacionFinalResponse desde(EvaluacionFinal e) {
        return EvaluacionFinalResponse.builder()
                .id(e.getId())
                .instanciaPracticaId(e.getInstanciaPractica().getId())
                .tipo(e.getTipo())
                .evaluadorId(e.getEvaluadorId())
                .evaluadorNombre(e.getEvaluadorNombre())
                .criterios(e.getCriterios())
                .promedioFinal(e.getPromedioFinal())
                .observaciones(e.getObservaciones())
                .estado(e.getEstado())
                .fecha(e.getFecha())
                .build();
    }
}
