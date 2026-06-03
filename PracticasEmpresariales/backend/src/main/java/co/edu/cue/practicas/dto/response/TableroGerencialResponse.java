package co.edu.cue.practicas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TableroGerencialResponse {
    private Map<String, Long> practicantesEnCursoPorPrograma;
    private double tasaAprobacionGlobal;
    private long empresasActivas;
}
