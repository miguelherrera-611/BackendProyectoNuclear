package co.edu.cue.practicas.dto.request;

import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlantillaNotificacionRequest {
    @NotNull
    private TipoEventoNotificacion tipoEvento;
    @NotBlank
    private String asunto;
    @NotBlank
    private String cuerpo;
    private String rolesReceptores;
    @Min(1)
    private int frecuenciaRecordatorioDias;
}
