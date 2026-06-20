package co.edu.cue.practicas.service.reporte;

import co.edu.cue.practicas.dto.request.ReporteEstadoProcesoRequest;
import co.edu.cue.practicas.dto.response.ReporteEstadoProcesoResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoExportacionReporte;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ReporteEstadoProcesoService {

    private final UsuarioRepository usuarioRepository;
    private final InstanciaPracticaRepository instanciaRepository;

    public ReporteEstadoProcesoResponse construir(ReporteEstadoProcesoRequest req, CustomUserDetails actor) {
        // SPRINT 4 - Builder: construye el reporte paso a paso respetando filtros y scope del usuario.
        Long programaScope = resolverProgramaScope(req, actor);
        Long facultadScope = resolverFacultadScope(req, actor);
        Map<String, Long> estados = new LinkedHashMap<>();
        estados.put("NO_APTO", contarEstudiantes(EstadoEstudiante.NO_APTO, programaScope, facultadScope, false));
        estados.put("APTO_DISPONIBLE", programaScope != null
                ? usuarioRepository.countEstudiantesAptosDisponibles(Rol.ESTUDIANTE, EstadoEstudiante.APTO, programaScope,
                List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA))
                : facultadScope != null
                ? usuarioRepository.countEstudiantesAptosDisponiblesPorFacultad(Rol.ESTUDIANTE, EstadoEstudiante.APTO, facultadScope,
                List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA))
                : contarEstudiantes(EstadoEstudiante.APTO, programaScope, facultadScope, true));
        estados.put("EN_CURSO", contarPracticas(EstadoPractica.EN_CURSO, programaScope, facultadScope, req.getSemestreAcademico()));
        estados.put("FINALIZADO", contarPracticas(EstadoPractica.FINALIZADA, programaScope, facultadScope, req.getSemestreAcademico()));
        estados.put("CANCELADO", contarPracticas(EstadoPractica.CANCELADA, programaScope, facultadScope, req.getSemestreAcademico()));
        long total = estados.values().stream().mapToLong(Long::longValue).sum();
        Exportacion exportacion = exportar(estados, req.getFormato());
        return ReporteEstadoProcesoResponse.builder()
                .estados(estados)
                .total(total)
                .exportacion(exportacion.contenidoBase64())
                .nombreArchivo(exportacion.nombreArchivo())
                .contentType(exportacion.contentType())
                .build();
    }

    private Long resolverProgramaScope(ReporteEstadoProcesoRequest req, CustomUserDetails actor) {
        if (actor.getRol() == Rol.COORDINADOR_PRACTICAS) {
            return null;
        }
        return req.getProgramaId();
    }

    private Long resolverFacultadScope(ReporteEstadoProcesoRequest req, CustomUserDetails actor) {
        // SPRINT 4 - Decorator: agrega filtro de facultad al reporte base para Coordinacion Academica.
        if (actor.getRol() == Rol.COORDINADOR_PRACTICAS) {
            if (actor.getFacultadId() == null) {
                throw new AccesoNoAutorizadoException("El coordinador no tiene facultad asignada.");
            }
            if (req.getFacultadId() != null && !req.getFacultadId().equals(actor.getFacultadId())) {
                throw new AccesoNoAutorizadoException("El coordinador no puede ver reportes de otra facultad.");
            }
            return actor.getFacultadId();
        }
        if (actor.getRol() == Rol.COORDINACION_ACADEMICA) {
            if (actor.getFacultadId() == null) {
                throw new AccesoNoAutorizadoException("La Coordinacion Academica no tiene facultad asignada.");
            }
            if (req.getFacultadId() != null && !req.getFacultadId().equals(actor.getFacultadId())) {
                throw new AccesoNoAutorizadoException("No puedes ver reportes de otra facultad.");
            }
            return actor.getFacultadId();
        }
        return req.getFacultadId();
    }

    private long contarEstudiantes(EstadoEstudiante estado, Long programaId, Long facultadId, boolean enviados) {
        if (programaId != null) {
            return usuarioRepository.countByRolAndEstadoEstudianteAndPrograma_IdAndActivoTrue(Rol.ESTUDIANTE, estado, programaId);
        }
        if (facultadId != null) {
            if (enviados) {
                return usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndPrograma_Facultad_IdAndActivoTrue(
                        Rol.ESTUDIANTE, estado, facultadId);
            }
            return usuarioRepository.countByRolAndEstadoEstudianteAndPrograma_Facultad_IdAndActivoTrue(Rol.ESTUDIANTE, estado, facultadId);
        }
        return enviados
                ? usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndActivoTrue(Rol.ESTUDIANTE, estado)
                : usuarioRepository.countByRolAndEstadoEstudianteAndActivoTrue(Rol.ESTUDIANTE, estado);
    }

    private long contarPracticas(EstadoPractica estado, Long programaId, Long facultadId, String semestreAcademico) {
        boolean filtraSemestre = semestreAcademico != null && !semestreAcademico.isBlank();
        if (programaId != null) {
            if (filtraSemestre) {
                return instanciaRepository.countByEstadoAndExpediente_Estudiante_Programa_IdAndSemestreAcademico(
                        estado, programaId, semestreAcademico);
            }
            return instanciaRepository.countByEstadoAndExpediente_Estudiante_Programa_Id(estado, programaId);
        }
        if (facultadId != null) {
            if (filtraSemestre) {
                return instanciaRepository.countByEstadoAndExpediente_Estudiante_Programa_Facultad_IdAndSemestreAcademico(
                        estado, facultadId, semestreAcademico);
            }
            return instanciaRepository.countByEstadoAndExpediente_Estudiante_Programa_Facultad_Id(estado, facultadId);
        }
        if (filtraSemestre) {
            return instanciaRepository.countByEstadoAndSemestreAcademico(estado, semestreAcademico);
        }
        return instanciaRepository.countByEstado(estado);
    }

    private Exportacion exportar(Map<String, Long> estados, TipoExportacionReporte formato) {
        // SPRINT 4 - Strategy: selecciona algoritmo de exportacion segun formato solicitado.
        if (formato == null) {
            return new Exportacion(null, null, null);
        }
        String tabla = "Estado,Total\n" + estados.entrySet().stream()
                .map(e -> e.getKey() + "," + e.getValue())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        if (formato == TipoExportacionReporte.PDF) {
            String pdfPlano = "%PDF-1.4\n% Reporte Estado Proceso\n" + tabla + "\n%%EOF";
            return new Exportacion(Base64.getEncoder().encodeToString(pdfPlano.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    "reporte-estado-proceso.pdf", "application/pdf");
        }
        return new Exportacion(Base64.getEncoder().encodeToString(tabla.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "reporte-estado-proceso.csv", "text/csv");
    }

    private record Exportacion(String contenidoBase64, String nombreArchivo, String contentType) {}
}
