package co.edu.cue.practicas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatalogoPracticaResponse {

    private Long id;
    private Long programaId;
    private String programaNombre;
    private int numeroPractica;
    private String nombre;
    private String materiaNucleo;
    private String codigoMateria;
    private int numCortes;
    private int duracionSemanas;
    private String documentosRequeridos;
    private boolean activo;
    private LocalDateTime creadoEn;
}
