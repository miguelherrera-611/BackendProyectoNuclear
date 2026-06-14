package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EstadoPractica;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanciaPracticaResponse {

    private Long id;
    private int numeroPractica;
    private String nombre;
    private String materiaNucleo;
    private String codigoMateria;
    private int numCortes;
    private int duracionSemanas;
    private String documentosRequeridos;
    private EstadoPractica estado;
    private Long vacanteId;
    private Long empresaId;
    private String razonSocialEmpresa;
    private Long estudianteId;
    private String nombreEstudiante;
    private Long docenteAsesorId;
    private String nombreDocenteAsesor;
    private Long tutorEmpresarialId;
    private String nombreTutorEmpresarial;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaSustentacion;
    private boolean firmaTutor;
    private boolean firmaDocente;
    private boolean firmaEstudiante;
    private LocalDateTime vinculacionConfirmadaEn;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    /** True cuando el docente ya registró su evaluación final — práctica congelada para escritura. */
    private boolean evaluacionDocenteRegistrada;
}
