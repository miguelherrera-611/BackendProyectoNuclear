package co.edu.cue.practicas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChecklistCierreResponse {
    private Long instanciaPracticaId;
    private boolean puedeEjecutarCierre;
    private List<ChecklistItemResponse> items;
}
