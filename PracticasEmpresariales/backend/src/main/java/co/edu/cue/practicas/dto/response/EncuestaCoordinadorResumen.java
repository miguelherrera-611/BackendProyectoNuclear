package co.edu.cue.practicas.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EncuestaCoordinadorResumen {
    private Long instanciaId;
    private String nombrePractica;
    private String nombreEstudiante;
    private Long tutorEmpresarialId;
    private String nombreTutor;
    private boolean evaluacionDocenteCompleta;
    private boolean encuestaTutorEnviada;
    private boolean encuestaTutorCompletada;
    private boolean encuestaEstudianteEnviada;
    private boolean encuestaEstudianteCompletada;
}
