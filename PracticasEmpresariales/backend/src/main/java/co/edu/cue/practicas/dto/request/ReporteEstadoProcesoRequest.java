package co.edu.cue.practicas.dto.request;

import co.edu.cue.practicas.model.enums.TipoExportacionReporte;
import lombok.Data;

@Data
public class ReporteEstadoProcesoRequest {
    private Long facultadId;
    private Long programaId;
    private String semestreAcademico;
    private TipoExportacionReporte formato;
}
