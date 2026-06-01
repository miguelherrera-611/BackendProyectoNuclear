package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.PracticaDocumento;
import co.edu.cue.practicas.model.enums.TipoDocumento;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PracticaDocumentoResponse {

    private Long id;
    private Long instanciaPracticaId;
    private TipoDocumento tipo;
    private String nombreOriginal;
    private String rutaArchivo;
    private String mimeType;
    private long tamanoBytes;
    private LocalDateTime creadoEn;

    public static PracticaDocumentoResponse desde(PracticaDocumento documento) {
        return PracticaDocumentoResponse.builder()
                .id(documento.getId())
                .instanciaPracticaId(documento.getInstanciaPractica() != null ? documento.getInstanciaPractica().getId() : null)
                .tipo(documento.getTipo())
                .nombreOriginal(documento.getNombreOriginal())
                .rutaArchivo(documento.getRutaArchivo())
                .mimeType(documento.getMimeType())
                .tamanoBytes(documento.getTamanoBytes())
                .creadoEn(documento.getCreadoEn())
                .build();
    }
}

