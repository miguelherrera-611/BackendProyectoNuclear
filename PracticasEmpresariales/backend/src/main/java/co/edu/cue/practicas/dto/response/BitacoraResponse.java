package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BitacoraResponse {

    private Long id;
    private Long usuarioId;
    private String nombreUsuario;
    private Rol rolUsuario;
    private EtiquetaCargo etiquetaCargoUsuario;
    private LocalDateTime fechaHora;
    private String modulo;
    private TipoAccion tipoAccion;
    private Long registroAfectadoId;
    private String registroAfectadoTipo;
    private String valoresAnteriores;
    private String valoresNuevos;
    private String ipOrigen;
    private boolean exitoso;
    private String motivoFallo;

    public static BitacoraResponse desde(BitacoraAuditoria b) {
        return BitacoraResponse.builder()
                .id(b.getId())
                .usuarioId(b.getUsuario() != null ? b.getUsuario().getId() : null)
                .nombreUsuario(b.getNombreUsuario())
                .rolUsuario(b.getRolUsuario())
                .etiquetaCargoUsuario(b.getEtiquetaCargoUsuario())
                .fechaHora(b.getFechaHora())
                .modulo(b.getModulo())
                .tipoAccion(b.getTipoAccion())
                .registroAfectadoId(b.getRegistroAfectadoId())
                .registroAfectadoTipo(b.getRegistroAfectadoTipo())
                .valoresAnteriores(b.getValoresAnteriores())
                .valoresNuevos(b.getValoresNuevos())
                .ipOrigen(b.getIpOrigen())
                .exitoso(b.isExitoso())
                .motivoFallo(b.getMotivoFallo())
                .build();
    }
}
