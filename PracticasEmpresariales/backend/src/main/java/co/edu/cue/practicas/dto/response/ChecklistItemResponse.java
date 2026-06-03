package co.edu.cue.practicas.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChecklistItemResponse {
    private String codigo;
    private String nombre;
    private boolean completo;
    private String estadoVisual;
    private String accionRequerida;
}
