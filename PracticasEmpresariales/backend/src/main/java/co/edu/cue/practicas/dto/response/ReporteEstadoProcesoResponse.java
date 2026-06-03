package co.edu.cue.practicas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ReporteEstadoProcesoResponse {
    private Map<String, Long> estados;
    private long total;
    private String exportacion;
    private String nombreArchivo;
    private String contentType;
}
