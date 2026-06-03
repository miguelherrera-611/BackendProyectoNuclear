package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.SustentacionPractica;
import co.edu.cue.practicas.model.enums.ResultadoSustentacion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SustentacionResponse {
    private Long id;
    private Long instanciaPracticaId;
    private LocalDate fecha;
    private List<String> jurados;
    private String actaUrl;
    private boolean actaFirmada;
    private ResultadoSustentacion resultado;
    private boolean completa;

    public static SustentacionResponse desde(SustentacionPractica s) {
        return SustentacionResponse.builder()
                .id(s.getId())
                .instanciaPracticaId(s.getInstanciaPractica().getId())
                .fecha(s.getFecha())
                .jurados(s.getJurados())
                .actaUrl(s.getActaUrl())
                .actaFirmada(s.isActaFirmada())
                .resultado(s.getResultado())
                .completa(s.estaCompleta())
                .build();
    }
}
