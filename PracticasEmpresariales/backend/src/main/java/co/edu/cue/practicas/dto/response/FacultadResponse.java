package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.Facultad;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FacultadResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activa;
    private int numeroProgramas;
    private LocalDateTime creadaEn;

    public static FacultadResponse desde(Facultad f) {
        return FacultadResponse.builder()
                .id(f.getId())
                .nombre(f.getNombre())
                .descripcion(f.getDescripcion())
                .activa(f.isActiva())
                .numeroProgramas(f.getProgramas().size())
                .creadaEn(f.getCreadaEn())
                .build();
    }
}
