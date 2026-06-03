package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.ResultadoPractica;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CierreFormalResponse {
    private Long instanciaPracticaId;
    private EstadoPractica estado;
    private ResultadoPractica resultado;
    private double notaFinal;
    private String codigoPazYSalvo;
    private String pazYSalvo;
}
