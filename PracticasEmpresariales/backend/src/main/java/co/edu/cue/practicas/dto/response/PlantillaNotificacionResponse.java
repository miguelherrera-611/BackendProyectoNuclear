package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.PlantillaNotificacion;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlantillaNotificacionResponse {
    private Long id;
    private TipoEventoNotificacion tipoEvento;
    private String asunto;
    private String cuerpo;
    private String rolesReceptores;
    private int frecuenciaRecordatorioDias;

    public static PlantillaNotificacionResponse desde(PlantillaNotificacion p) {
        return PlantillaNotificacionResponse.builder()
                .id(p.getId())
                .tipoEvento(p.getTipoEvento())
                .asunto(p.getAsunto())
                .cuerpo(p.getCuerpo())
                .rolesReceptores(p.getRolesReceptores())
                .frecuenciaRecordatorioDias(p.getFrecuenciaRecordatorioDias())
                .build();
    }
}
