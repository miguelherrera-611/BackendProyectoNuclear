package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.RequisitosPractica;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProgramaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Long facultadId;
    private String facultadNombre;
    private int numeroTotalPracticas;
    private double promedioMinimoGeneral;
    private boolean activo;
    private List<RequisitoResponse> requisitos;
    private LocalDateTime creadoEn;

    @Data
    @Builder
    public static class RequisitoResponse {
        private Long id;
        private int numeroPractica;
        private int creditosMinimos;
        private double promedioMinimo;
        private boolean requierePracticaAnteriorAprobada;
        private String documentosRequeridos;

        public static RequisitoResponse desde(RequisitosPractica r) {
            return RequisitoResponse.builder()
                    .id(r.getId())
                    .numeroPractica(r.getNumeroPractica())
                    .creditosMinimos(r.getCreditosMinimos())
                    .promedioMinimo(r.getPromedioMinimo())
                    .requierePracticaAnteriorAprobada(r.isRequierePracticaAnteriorAprobada())
                    .documentosRequeridos(r.getDocumentosRequeridos())
                    .build();
        }
    }

    public static ProgramaResponse desde(Programa p) {
        return ProgramaResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .facultadId(p.getFacultad().getId())
                .facultadNombre(p.getFacultad().getNombre())
                .numeroTotalPracticas(p.getNumeroTotalPracticas())
                .promedioMinimoGeneral(p.getPromedioMinimoGeneral())
                .activo(p.isActivo())
                .creadoEn(p.getCreadoEn())
                .requisitos(p.getRequisitos().stream().map(RequisitoResponse::desde).toList())
                .build();
    }
}
