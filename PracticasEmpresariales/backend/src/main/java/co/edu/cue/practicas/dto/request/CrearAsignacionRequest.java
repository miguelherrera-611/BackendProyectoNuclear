package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request usado por el Coordinador de Prácticas para asignar un estudiante a una vacante.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearAsignacionRequest {

    @NotNull
    private Long estudianteId;

    @NotNull
    private Long vacanteId;

    /** Opcional: si se desea usar un catálogo concreto. Si es null se buscará el catálogo activo apropiado */
    private Long catalogoPracticaId;

    /** Opcional: docente asesor a asignar en el momento de crear la instancia */
    private Long docenteAsesorId;

    /** Opcional: tutor empresarial a asignar manualmente */
    private Long tutorEmpresarialId;
}

