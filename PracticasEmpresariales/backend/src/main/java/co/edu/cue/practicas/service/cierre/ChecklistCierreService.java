package co.edu.cue.practicas.service.cierre;

import co.edu.cue.practicas.dto.response.ChecklistCierreResponse;
import co.edu.cue.practicas.dto.response.ChecklistItemResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.SustentacionPractica;
import co.edu.cue.practicas.model.enums.EstadoEncuesta;
import co.edu.cue.practicas.model.enums.EstadoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEncuesta;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import co.edu.cue.practicas.repository.cierre.SustentacionPracticaRepository;
import co.edu.cue.practicas.repository.documento.PracticaDocumentoRepository;
import co.edu.cue.practicas.repository.encuesta.EncuestaSatisfaccionRepository;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.evaluacion.NotaFinalCoordinadorRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.configuracion.ProgramaConfiguracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistCierreService {

    private final InstanciaPracticaRepository instanciaRepository;
    private final EvaluacionFinalRepository evaluacionRepository;
    private final NotaFinalCoordinadorRepository notaRepository;
    private final EncuestaSatisfaccionRepository encuestaRepository;
    private final PracticaDocumentoRepository documentoRepository;
    private final SustentacionPracticaRepository sustentacionRepository;
    private final ProgramaConfiguracionService configuracionService;

    public ChecklistCierreResponse generar(Long instanciaId, CustomUserDetails actor) {
        var instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));
        if (actor.getRol() == Rol.COORDINADOR_PRACTICAS && actor.getProgramaId() != null
                && !actor.getProgramaId().equals(instancia.getExpediente().getEstudiante().getPrograma().getId())) {
            throw new AccesoNoAutorizadoException("No puedes consultar cierres de otro programa.");
        }
        Long programaId = instancia.getExpediente().getEstudiante().getPrograma().getId();
        Set<String> requisitos = requisitosConfigurados(programaId);
        Map<String, ChecklistItemResponse> base = new LinkedHashMap<>();
        // SPRINT 4 - Chain of Responsibility: cada item valida un requisito secuencial del cierre.
        base.put("evaluacion_docente", item("evaluacion_docente", "Evaluacion Docente Asesor",
                evaluacionRepository.existsByInstanciaPractica_IdAndTipoAndEstado(instanciaId, TipoEvaluacionFinal.DOCENTE_ASESOR, EstadoEvaluacionFinal.COMPLETADA),
                "/evaluaciones/docente/" + instanciaId));
        base.put("evaluacion_tutor", item("evaluacion_tutor", "Evaluacion Tutor Empresarial",
                evaluacionRepository.existsByInstanciaPractica_IdAndTipoAndEstado(instanciaId, TipoEvaluacionFinal.TUTOR_EMPRESARIAL, EstadoEvaluacionFinal.COMPLETADA),
                "/evaluaciones/tutor/" + instanciaId));
        base.put("nota_final", item("nota_final", "Nota final Coordinador", notaRepository.existsByInstanciaPractica_Id(instanciaId),
                "/evaluaciones/coordinador/" + instanciaId));
        base.put("encuesta_tutor", item("encuesta_tutor", "Encuesta Tutor Empresarial",
                encuestaRepository.existsByInstanciaPractica_IdAndTipoAndEstado(instanciaId, TipoEncuesta.PARA_TUTOR, EstadoEncuesta.COMPLETADA),
                "/encuestas/tutor/" + instanciaId));
        base.put("encuesta_estudiante", item("encuesta_estudiante", "Encuesta Estudiante",
                encuestaRepository.existsByInstanciaPractica_IdAndTipoAndEstado(instanciaId, TipoEncuesta.PARA_ESTUDIANTE, EstadoEncuesta.COMPLETADA),
                "/encuestas/estudiante/" + instanciaId));
        base.put("documentos", item("documentos", "Documentos requeridos cargados",
                documentoRepository.countByInstanciaPractica_Id(instanciaId) > 0,
                "/documentos/practica/" + instanciaId));
        base.put("sustentacion", item("sustentacion", "Sustentacion con acta firmada",
                sustentacionRepository.findByInstanciaPractica_Id(instanciaId).map(SustentacionPractica::estaCompleta).orElse(false),
                "/cierre/sustentaciones/" + instanciaId));
        // SPRINT 4 - Decorator: al checklist base se le aplican requisitos configurables por programa.
        List<ChecklistItemResponse> items = base.entrySet().stream()
                .filter(entry -> requisitos.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new));

        boolean completo = items.stream().allMatch(ChecklistItemResponse::isCompleto);
        return ChecklistCierreResponse.builder()
                .instanciaPracticaId(instanciaId)
                .items(items)
                .puedeEjecutarCierre(completo)
                .build();
    }

    private ChecklistItemResponse item(String codigo, String nombre, boolean completo, String accion) {
        return ChecklistItemResponse.builder()
                .codigo(codigo)
                .nombre(nombre)
                .completo(completo)
                .estadoVisual(completo ? "COMPLETO" : "PENDIENTE_ROJO")
                .accionRequerida(completo ? null : accion)
                .build();
    }

    private Set<String> requisitosConfigurados(Long programaId) {
        String raw = configuracionService.obtener(programaId).getRequisitosCierre();
        Set<String> requisitos = raw == null || raw.isBlank()
                ? new java.util.HashSet<>(Set.of("evaluacion_docente", "evaluacion_tutor", "nota_final", "encuesta_tutor", "encuesta_estudiante", "documentos", "sustentacion"))
                : java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        requisitos.add("sustentacion");
        // SPRINT 4 - Regla obligatoria: sustentacion siempre decora la configuracion y no puede omitirse.
        return requisitos;
    }
}
