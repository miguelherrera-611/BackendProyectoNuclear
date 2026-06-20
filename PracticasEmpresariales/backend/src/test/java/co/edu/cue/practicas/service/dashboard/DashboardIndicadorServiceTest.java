package co.edu.cue.practicas.service.dashboard;

import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.PlanPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.SeguimientoSemanalRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GPE-132 — Pruebas unitarias del DashboardIndicadorService.
 *
 * Verifica que cada rol recibe los contadores correctos calculados
 * desde los repositorios. El EntityManager se inyecta vía
 * ReflectionTestUtils porque usa @PersistenceContext (no constructor).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardIndicadorService — Indicadores por rol (GPE-132)")
class DashboardIndicadorServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InstanciaPracticaRepository instanciaPracticaRepository;
    @Mock private PlanPracticaRepository planPracticaRepository;
    @Mock private SeguimientoSemanalRepository seguimientoSemanalRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private DashboardIndicadorService service;

    @BeforeEach
    void injectEntityManager() {
        // @PersistenceContext no está en el constructor → inyección manual
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    // =================================================================
    // DTI — total usuarios activos por rol
    // =================================================================

    @Test
    @DisplayName("DTI debe recibir contadores de usuarios activos por cada rol")
    void indicadoresDtiDevuelveContadoresDeUsuarios() {
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN_DTI)).thenReturn(2L);
        when(usuarioRepository.countByRolAndActivoTrue(Rol.COORDINACION_ACADEMICA)).thenReturn(3L);
        when(usuarioRepository.countByRolAndActivoTrue(Rol.COORDINADOR_PRACTICAS)).thenReturn(1L);
        when(usuarioRepository.countByRolAndActivoTrue(Rol.DOCENTE_ASESOR)).thenReturn(8L);
        when(usuarioRepository.countByRolAndActivoTrue(Rol.TUTOR_EMPRESARIAL)).thenReturn(12L);
        when(usuarioRepository.countByRolAndEstadoEstudianteAndActivoTrue(Rol.ESTUDIANTE, EstadoEstudiante.NO_APTO)).thenReturn(5L);
        when(usuarioRepository.countByRolAndEstadoEstudianteAndActivoTrue(Rol.ESTUDIANTE, EstadoEstudiante.APTO)).thenReturn(20L);

        DashboardIndicadores indicadores = service.obtenerIndicadores(udConRol(Rol.ADMIN_DTI, 1L));

        assertThat(indicadores.usuariosActivosAdminDti()).isEqualTo(2L);
        assertThat(indicadores.usuariosActivosCoordinacionAcademica()).isEqualTo(3L);
        assertThat(indicadores.usuariosActivosDocenteAsesor()).isEqualTo(8L);
        assertThat(indicadores.estudiantesNoApto()).isEqualTo(5L);
        assertThat(indicadores.estudiantesApto()).isEqualTo(20L);
    }

    // =================================================================
    // COORDINACION_ACADEMICA — estudiantes pendientes
    // =================================================================

    @Test
    @DisplayName("Coordinación Académica recibe estudiantes NO_APTO y APTO pendientes de envío")
    void indicadoresCoordinacionAcademicaDevuelvePendientes() {
        when(usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoFalseAndActivoTrue(
                Rol.ESTUDIANTE, EstadoEstudiante.NO_APTO)).thenReturn(7L);
        when(usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoFalseAndActivoTrue(
                Rol.ESTUDIANTE, EstadoEstudiante.APTO)).thenReturn(4L);
        when(usuarioRepository.countByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndActivoTrue(
                Rol.ESTUDIANTE, EstadoEstudiante.APTO)).thenReturn(10L);

        DashboardIndicadores indicadores = service.obtenerIndicadores(udConRol(Rol.COORDINACION_ACADEMICA, 2L));

        assertThat(indicadores.estudiantesNoAptoPendientesValidacion()).isEqualTo(7L);
        assertThat(indicadores.estudiantesAptoPendientesEnvioProceso()).isEqualTo(4L);
        assertThat(indicadores.estudiantesApto()).isEqualTo(10L);
    }

    // =================================================================
    // COORDINADOR_PRACTICAS — APTOS, vacantes, prácticas, planes
    // =================================================================

    @Test
    @DisplayName("Coordinador de Prácticas recibe sus 4 indicadores clave")
    void indicadoresCoordinadorPracticasDevuelveCuatroIndicadores() {
        Long facultadId = 5L;
        CustomUserDetails coord = udConRolYFacultad(Rol.COORDINADOR_PRACTICAS, 3L, facultadId);

        when(usuarioRepository.countEstudiantesAptosDisponiblesPorFacultad(
                eq(Rol.ESTUDIANTE), eq(EstadoEstudiante.APTO), eq(facultadId), anyList()))
                .thenReturn(6L);
        stubContarVacantesDisponibles(3L);
        when(instanciaPracticaRepository.countByEstadoAndExpediente_Estudiante_Programa_Facultad_Id(
                EstadoPractica.EN_CURSO, facultadId)).thenReturn(10L);
        when(planPracticaRepository.countByEstadoIn(anyList())).thenReturn(2L);

        DashboardIndicadores indicadores = service.obtenerIndicadores(coord);

        assertThat(indicadores.estudiantesAptoDisponibles()).isEqualTo(6L);
        assertThat(indicadores.vacantesDisponibles()).isEqualTo(3L);
        assertThat(indicadores.practicasEnCurso()).isEqualTo(10L);
        assertThat(indicadores.planesPendientesAprobacion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Coordinador sin facultad asignada retorna 0 en estudiantesAptoDisponibles y practicasEnCurso")
    void indicadoresCoordinadorSinFacultadDevuelveCero() {
        CustomUserDetails coordSinFacultad = udConRol(Rol.COORDINADOR_PRACTICAS, 3L);
        stubContarVacantesDisponibles(0L);
        when(planPracticaRepository.countByEstadoIn(anyList())).thenReturn(0L);

        DashboardIndicadores indicadores = service.obtenerIndicadores(coordSinFacultad);

        assertThat(indicadores.estudiantesAptoDisponibles()).isEqualTo(0L);
        assertThat(indicadores.practicasEnCurso()).isEqualTo(0L);
        verify(usuarioRepository, never()).countEstudiantesAptosDisponiblesPorFacultad(any(), any(), any(), any());
        verify(instanciaPracticaRepository, never())
                .countByEstadoAndExpediente_Estudiante_Programa_Facultad_Id(any(), any());
    }

    // =================================================================
    // DOCENTE_ASESOR — estudiantes asignados, seguimientos pendientes
    // =================================================================

    @Test
    @DisplayName("Docente Asesor recibe contadores de sus practicantes y revisiones pendientes")
    void indicadoresDocenteDevuelveEstudiantesYSeguimientos() {
        Long docenteId = 20L;
        when(instanciaPracticaRepository.countByDocenteAsesor_IdAndEstadoNotIn(eq(docenteId), anyList()))
                .thenReturn(5L);
        when(seguimientoSemanalRepository.countByInstanciaPractica_DocenteAsesor_IdAndEstado(
                docenteId, EstadoSeguimiento.PENDIENTE)).thenReturn(3L);
        when(planPracticaRepository.countByInstanciaPractica_DocenteAsesor_IdAndEstadoIn(
                eq(docenteId), anyList())).thenReturn(1L);

        DashboardIndicadores indicadores = service.obtenerIndicadores(udConRol(Rol.DOCENTE_ASESOR, docenteId));

        assertThat(indicadores.estudiantesAsignadosDocente()).isEqualTo(5L);
        // seguimientosPendientes = 3 (seguimientos) + 1 (planes pendientes de aprobar)
        assertThat(indicadores.seguimientosPendientesRevision()).isEqualTo(4L);
    }

    // =================================================================
    // TUTOR_EMPRESARIAL — practicantes a cargo, planes pendientes
    // =================================================================

    @Test
    @DisplayName("Tutor Empresarial recibe sus practicantes a cargo y planes pendientes")
    void indicadoresTutorDevuelvePracticantesYPlanes() {
        Usuario tutor = Usuario.builder()
                .id(30L).nombre("Tutor Test").correo("tutor@corp.com")
                .passwordHash("h").rol(Rol.TUTOR_EMPRESARIAL).activo(true).build();

        when(usuarioRepository.findByCorreoAndActivoTrue("tutor@corp.com"))
                .thenReturn(Optional.of(tutor));
        when(instanciaPracticaRepository.countByTutorEmpresarial_IdAndEstadoNotIn(eq(30L), anyList()))
                .thenReturn(3L);
        when(planPracticaRepository.countByInstanciaPractica_TutorEmpresarial_IdAndEstadoIn(
                eq(30L), anyList())).thenReturn(1L);

        CustomUserDetails tutorDetails = new CustomUserDetails(Usuario.builder()
                .id(30L).nombre("Tutor").correo("tutor@corp.com")
                .passwordHash("h").rol(Rol.TUTOR_EMPRESARIAL).activo(true).build());

        DashboardIndicadores indicadores = service.obtenerIndicadores(tutorDetails);

        assertThat(indicadores.practicantesACargo()).isEqualTo(3L);
        assertThat(indicadores.planesPendientesAprobacion()).isEqualTo(1L);
        assertThat(indicadores.encuestasPendientes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Tutor sin perfil en BD retorna indicadores en cero")
    void indicadoresTutorSinPerfilRetornaCero() {
        when(usuarioRepository.findByCorreoAndActivoTrue(anyString()))
                .thenReturn(Optional.empty());

        DashboardIndicadores indicadores = service.obtenerIndicadores(udConRol(Rol.TUTOR_EMPRESARIAL, 99L));

        assertThat(indicadores.practicantesACargo()).isEqualTo(0L);
        assertThat(indicadores.planesPendientesAprobacion()).isEqualTo(0L);
    }

    // =================================================================
    // ESTUDIANTE — semana actual calculada correctamente
    // =================================================================

    @Test
    @DisplayName("Estudiante sin práctica EN_CURSO recibe semanaActual = 0")
    void indicadoresEstudianteSinPracticaActivaRetornaCero() {
        Long estudianteId = 10L;
        when(instanciaPracticaRepository.countByExpediente_Estudiante_IdAndEstado(
                estudianteId, EstadoPractica.EN_CURSO)).thenReturn(0L);

        DashboardIndicadores indicadores = service.obtenerIndicadores(udConRol(Rol.ESTUDIANTE, estudianteId));

        assertThat(indicadores.semanaSeguimientoActual()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Estudiante con práctica EN_CURSO y 2 seguimientos recibe semanaActual = 3")
    void indicadoresEstudianteConSeguimientosRetornaSemanaSiguiente() {
        Long estudianteId = 10L;
        Long instanciaId = 100L;
        InstanciaPractica instancia = InstanciaPractica.builder()
                .id(instanciaId).numeroPractica(1).nombre("P").materiaNucleo("M")
                .codigoMateria("C").numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.EN_CURSO).build();

        when(instanciaPracticaRepository.countByExpediente_Estudiante_IdAndEstado(
                estudianteId, EstadoPractica.EN_CURSO)).thenReturn(1L);
        when(instanciaPracticaRepository.findTopByExpediente_Estudiante_IdAndEstadoOrderByCreadoEnDesc(
                estudianteId, EstadoPractica.EN_CURSO)).thenReturn(Optional.of(instancia));
        when(seguimientoSemanalRepository.countByInstanciaPractica_Id(instanciaId))
                .thenReturn(2L); // ya tiene 2 seguimientos

        DashboardIndicadores indicadores = service.obtenerIndicadores(udConRol(Rol.ESTUDIANTE, estudianteId));

        assertThat(indicadores.semanaSeguimientoActual()).isEqualTo(3L); // siguiente semana = 2+1
    }

    @Test
    @DisplayName("Estudiante recién activado (sin seguimientos) recibe semanaActual = 1")
    void indicadoresEstudianteNuevoRetornaSemanaUno() {
        Long estudianteId = 10L;
        Long instanciaId = 100L;
        InstanciaPractica instancia = InstanciaPractica.builder()
                .id(instanciaId).numeroPractica(1).nombre("P").materiaNucleo("M")
                .codigoMateria("C").numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.EN_CURSO).build();

        when(instanciaPracticaRepository.countByExpediente_Estudiante_IdAndEstado(
                estudianteId, EstadoPractica.EN_CURSO)).thenReturn(1L);
        when(instanciaPracticaRepository.findTopByExpediente_Estudiante_IdAndEstadoOrderByCreadoEnDesc(
                estudianteId, EstadoPractica.EN_CURSO)).thenReturn(Optional.of(instancia));
        when(seguimientoSemanalRepository.countByInstanciaPractica_Id(instanciaId))
                .thenReturn(0L);

        DashboardIndicadores indicadores = service.obtenerIndicadores(udConRol(Rol.ESTUDIANTE, estudianteId));

        assertThat(indicadores.semanaSeguimientoActual()).isEqualTo(1L);
    }

    // =================================================================
    // Helpers
    // =================================================================

    private CustomUserDetails udConRol(Rol rol, Long id) {
        return new CustomUserDetails(Usuario.builder()
                .id(id).nombre("Test " + rol).correo("test" + id + "@cue.edu.co")
                .passwordHash("hash").rol(rol).activo(true).build());
    }

    private CustomUserDetails udConRolYFacultad(Rol rol, Long id, Long facultadId) {
        Facultad facultad = Facultad.builder().id(facultadId).nombre("Facultad de Ingeniería").build();
        return new CustomUserDetails(Usuario.builder()
                .id(id).nombre("Test " + rol).correo("test@cue.edu.co")
                .passwordHash("hash").rol(rol).activo(true).facultad(facultad).build());
    }

    @SuppressWarnings("unchecked")
    private void stubContarVacantesDisponibles(long resultado) {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(resultado);
    }
}
