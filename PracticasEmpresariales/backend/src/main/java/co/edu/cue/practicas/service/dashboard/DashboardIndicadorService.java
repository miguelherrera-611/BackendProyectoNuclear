package co.edu.cue.practicas.service.dashboard;

import co.edu.cue.practicas.model.entity.TutorEmpresarial;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.tutor.TutorEmpresarialRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Calcula los indicadores numéricos del dashboard.
 * Cumple SRP porque solo agrega métricas de lectura:
 * no construye la vista y no decide navegación.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardIndicadorService {

    private static final List<EstadoPractica> ESTADOS_FINALES =
            List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA);

    private final UsuarioRepository usuarioRepository;
    private final InstanciaPracticaRepository instanciaPracticaRepository;
    private final TutorEmpresarialRepository tutorEmpresarialRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardIndicadores obtenerIndicadores(CustomUserDetails userDetails) {
        return switch (userDetails.getRol()) {
            case ADMIN_DTI -> indicadoresDTI();
            case COORDINACION_ACADEMICA -> indicadoresCoordinacionAcademica();
            case COORDINADOR_PRACTICAS -> indicadoresCoordinadorPracticas(userDetails);
            case DOCENTE_ASESOR -> indicadoresDocente(userDetails);
            case TUTOR_EMPRESARIAL -> indicadoresTutor(userDetails);
            case DIRECCION -> indicadoresDireccion();
            case ESTUDIANTE -> indicadoresEstudiante(userDetails);
        };
    }

    private DashboardIndicadores indicadoresDTI() {
        return DashboardIndicadores.builder()
                .usuariosActivosAdminDti(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN_DTI))
                .usuariosActivosCoordinacionAcademica(usuarioRepository.countByRolAndActivoTrue(Rol.COORDINACION_ACADEMICA))
                .usuariosActivosCoordinadorPracticas(usuarioRepository.countByRolAndActivoTrue(Rol.COORDINADOR_PRACTICAS))
                .usuariosActivosDocenteAsesor(usuarioRepository.countByRolAndActivoTrue(Rol.DOCENTE_ASESOR))
                .usuariosActivosTutorEmpresarial(usuarioRepository.countByRolAndActivoTrue(Rol.TUTOR_EMPRESARIAL))
                .estudiantesNoApto(usuarioRepository.countByRolAndEstadoEstudianteAndActivoTrue(Rol.ESTUDIANTE, EstadoEstudiante.NO_APTO))
                .estudiantesApto(usuarioRepository.countByRolAndEstadoEstudianteAndActivoTrue(Rol.ESTUDIANTE, EstadoEstudiante.APTO))
                .build();
    }

    private DashboardIndicadores indicadoresCoordinacionAcademica() {
        return DashboardIndicadores.builder()
                .estudiantesNoAptoPendientesValidacion(
                        usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoFalseAndActivoTrue(
                                Rol.ESTUDIANTE, EstadoEstudiante.NO_APTO))
                .estudiantesAptoPendientesEnvioProceso(
                        usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoFalseAndActivoTrue(
                                Rol.ESTUDIANTE, EstadoEstudiante.APTO))
                .estudiantesApto(
                        usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndActivoTrue(
                                Rol.ESTUDIANTE, EstadoEstudiante.APTO))
                .build();
    }

    private DashboardIndicadores indicadoresCoordinadorPracticas(CustomUserDetails userDetails) {
        Long programaId = userDetails.getProgramaId();
        long estudiantesAptosDisponibles = programaId == null
                ? 0L
                : usuarioRepository.countEstudiantesAptosDisponibles(
                        Rol.ESTUDIANTE,
                        EstadoEstudiante.APTO,
                        programaId,
                        ESTADOS_FINALES);

        return DashboardIndicadores.builder()
                .estudiantesAptoDisponibles(estudiantesAptosDisponibles)
                .vacantesDisponibles(contarVacantesDisponibles())
                .practicasEnCurso(instanciaPracticaRepository.countByEstado(EstadoPractica.EN_CURSO))
                .planesPendientesAprobacion(0L)
                .build();
    }

    private DashboardIndicadores indicadoresDocente(CustomUserDetails userDetails) {
        Long docenteId = userDetails.getId();
        return DashboardIndicadores.builder()
                .estudiantesAsignadosDocente(instanciaPracticaRepository.countByDocenteAsesor_IdAndEstadoNotIn(docenteId, ESTADOS_FINALES))
                .seguimientosPendientesRevision(0L)
                .sustentacionesProgramadas(0L)
                .build();
    }

    private DashboardIndicadores indicadoresTutor(CustomUserDetails userDetails) {
        TutorEmpresarial tutor = tutorEmpresarialRepository
                .findByCorreoAndActivoTrue(userDetails.getUsername())
                .orElse(null);

        if (tutor == null) {
            return DashboardIndicadores.vacio();
        }

        return DashboardIndicadores.builder()
                .practicantesACargo(instanciaPracticaRepository.countByTutorEmpresarial_IdAndEstadoNotIn(tutor.getId(), ESTADOS_FINALES))
                .planesPendientesAprobacion(0L)
                .encuestasPendientes(0L)
                .build();
    }

    private DashboardIndicadores indicadoresDireccion() {
        long practicasEnCurso = instanciaPracticaRepository.countByEstado(EstadoPractica.EN_CURSO);
        long estudiantesAprobados = usuarioRepository.countByRolAndEstadoEstudianteAndActivoTrue(Rol.ESTUDIANTE, EstadoEstudiante.APTO);
        long estudiantesTotales = usuarioRepository.countByRolAndActivoTrue(Rol.ESTUDIANTE);
        long tasaAprobacionGlobal = estudiantesTotales == 0 ? 0 : Math.round((estudiantesAprobados * 100.0) / estudiantesTotales);

        var practicasPorPrograma = entityManager.createQuery(
                        "SELECT p.id, p.nombre FROM Programa p WHERE p.activo = true",
                        Object[].class)
                .getResultList()
                .stream()
                .map(row -> new ProgramaIndicador(
                        (Long) row[0],
                        (String) row[1],
                        instanciaPracticaRepository.countByExpediente_Estudiante_Programa_IdAndEstado(
                                (Long) row[0],
                                EstadoPractica.EN_CURSO)))
                .toList();

        return DashboardIndicadores.builder()
                .practicasEnCursoDireccion(practicasEnCurso)
                .tasaAprobacionGlobal(tasaAprobacionGlobal)
                .practicasEnCursoPorPrograma(practicasPorPrograma)
                .build();
    }

    private long contarVacantesDisponibles() {
        return entityManager.createQuery(
                        "SELECT COUNT(v) FROM Vacante v WHERE v.estado = :estado AND v.cuposOcupados < v.cuposTotales",
                        Long.class)
                .setParameter("estado", EstadoVacante.DISPONIBLE)
                .getSingleResult();
    }

    private DashboardIndicadores indicadoresEstudiante(CustomUserDetails userDetails) {
        long practicaActiva = instanciaPracticaRepository.countByExpediente_Estudiante_IdAndEstado(userDetails.getId(), EstadoPractica.EN_CURSO);
        return DashboardIndicadores.builder()
                .semanaSeguimientoActual(practicaActiva > 0 ? 1L : 0L)
                .documentosPendientes(0L)
                .build();
    }
}

