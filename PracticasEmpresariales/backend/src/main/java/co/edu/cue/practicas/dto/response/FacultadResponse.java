package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.Facultad;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FacultadResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activa;
    private int numeroProgramas;
    private boolean tieneProgramasActivos;
    private List<ProgramaInfo> programas;
    private LocalDateTime creadaEn;

    public record ProgramaInfo(Long id, String nombre, boolean activo,
                               int numeroTotalPracticas, double promedioMinimoGeneral) {}

    public static FacultadResponse desde(Facultad f) {
        List<ProgramaInfo> programas = f.getProgramas().stream()
                .map(p -> new ProgramaInfo(
                        p.getId(), p.getNombre(), p.isActivo(),
                        p.getNumeroTotalPracticas(), p.getPromedioMinimoGeneral()))
                .toList();

        return FacultadResponse.builder()
                .id(f.getId())
                .nombre(f.getNombre())
                .descripcion(f.getDescripcion())
                .activa(f.isActiva())
                .numeroProgramas(programas.size())
                .tieneProgramasActivos(f.tieneRecursosActivos())
                .programas(programas)
                .creadaEn(f.getCreadaEn())
                .build();
    }
}
