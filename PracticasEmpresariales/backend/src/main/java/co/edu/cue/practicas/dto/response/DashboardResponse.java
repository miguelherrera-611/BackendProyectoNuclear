package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    private Rol rol;
    private EtiquetaCargo etiquetaCargo;
    private String nombreUsuario;
    private String titulo;
    private boolean soloLectura;
    private List<Map<String, Object>> secciones;
}
