package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.ProgramaConfiguracion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProgramaConfiguracionResponse {
    private Long id;
    private Long programaId;
    private int numeroPracticas;
    private int semanasSeguimiento;
    private double notaMinimaAprobacion;
    private String requisitosCierre;
    private int umbralInactividadDias;
    private boolean vigente;

    public static ProgramaConfiguracionResponse desde(ProgramaConfiguracion c) {
        return ProgramaConfiguracionResponse.builder()
                .id(c.getId())
                .programaId(c.getPrograma().getId())
                .numeroPracticas(c.getNumeroPracticas())
                .semanasSeguimiento(c.getSemanasSeguimiento())
                .notaMinimaAprobacion(c.getNotaMinimaAprobacion())
                .requisitosCierre(c.getRequisitosCierre())
                .umbralInactividadDias(c.getUmbralInactividadDias())
                .vigente(c.isVigente())
                .build();
    }
}
