package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.NotaFinalCoordinador;
import co.edu.cue.practicas.model.enums.ResultadoPractica;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotaFinalResponse {
    private Long id;
    private Long instanciaPracticaId;
    private double notaFinal;
    private double notaMinimaAplicada;
    private ResultadoPractica resultado;
    private String observaciones;
    private LocalDateTime fecha;

    public static NotaFinalResponse desde(NotaFinalCoordinador n) {
        return NotaFinalResponse.builder()
                .id(n.getId())
                .instanciaPracticaId(n.getInstanciaPractica().getId())
                .notaFinal(n.getNotaFinal())
                .notaMinimaAplicada(n.getNotaMinimaAplicada())
                .resultado(n.getResultado())
                .observaciones(n.getObservaciones())
                .fecha(n.getFecha())
                .build();
    }
}
