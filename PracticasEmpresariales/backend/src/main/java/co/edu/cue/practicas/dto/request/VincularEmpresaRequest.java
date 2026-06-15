package co.edu.cue.practicas.dto.request;

import lombok.Data;

@Data
public class VincularEmpresaRequest {
    /** ID de la empresa a vincular. Null para desvincular. */
    private Long empresaId;
}
