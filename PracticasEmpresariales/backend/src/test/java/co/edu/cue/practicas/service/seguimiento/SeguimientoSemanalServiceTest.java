package co.edu.cue.practicas.service.seguimiento;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearSeguimientoRequest;
import co.edu.cue.practicas.dto.request.ObservacionDocenteRequest;
import co.edu.cue.practicas.dto.response.SeguimientoSemanalResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.PlanPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.SeguimientoSemanalRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.notificacion.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeguimientoSemanalService — Pruebas unitarias (GPE-168 / GPE-170)")
class SeguimientoSemanalServiceTest {

    @Mock private SeguimientoSemanalRepository seguimientoRepository;
    @Mock private InstanciaPracticaRepository instanciaRepository;
    @Mock private PlanPracticaRepository planRepository;
    @Mock private EvaluacionFinalRepository evaluacionFinalRepository;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private EmailService emailService;

    @InjectMocks
    private SeguimientoSemanalService service;

    private CustomUserDetails estudianteDetails;
    private CustomUserDetails docenteDetails;
    private CustomUserDetails otroDocente;
    private CustomUserDetails noAutorizado;

    private InstanciaPractica instanciaEnCurso;
    private SeguimientoSemanal seguimientoPendiente;
    private SeguimientoSemanal seguimientoRechazado;

    private static final Long INSTANCIA_ID = 1L;
    private static final Long ESTUDIANTE_ID = 10L;
    private static final Long DOCENTE_ID = 20L;
    private static final Long SEG_ID = 50L;

    @BeforeEach
    void setUp() {
        estudianteDetails = udConRol(Rol.ESTUDIANTE, ESTUDIANTE_ID);
        docenteDetails = udConRol(Rol.DOCENTE_ASESOR, DOCENTE_ID);
        otroDocente = udConRol(Rol.DOCENTE_ASESOR, 999L);
        noAutorizado = udConRol(Rol.ADMIN_DTI, 99L);

        Usuario estudianteUsuario = Usuario.builder()
                .id(ESTUDIANTE_ID).nombre("Est Test").correo("est@cue.edu.co")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        Usuario docenteUsuario = Usuario.builder()
                .id(DOCENTE_ID).nombre("Dr. López").correo("lopez@cue.edu.co")
                .passwordHash("h").rol(Rol.DOCENTE_ASESOR).activo(true).build();

        ExpedienteEstudiante expediente = ExpedienteEstudiante.builder()
                .id(1L).estudiante(estudianteUsuario).build();

        instanciaEnCurso = InstanciaPractica.builder()
                .id(INSTANCIA_ID).numeroPractica(1).nombre("Práctica I")
                .materiaNucleo("P").codigoMateria("PE-101")
                .numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.EN_CURSO)
                .docenteAsesor(docenteUsuario)
                .expediente(expediente).build();

        seguimientoPendiente = SeguimientoSemanal.builder()
                .id(SEG_ID).semana(1).actividades("Act 1").logros("Logros 1")
                .instanciaPractica(instanciaEnCurso).creadoPorId(ESTUDIANTE_ID).build();

        seguimientoRechazado = SeguimientoSemanal.builder()
                .id(SEG_ID).semana(1).actividades("Act 1").logros("Logros 1")
                .instanciaPractica(instanciaEnCurso).creadoPorId(ESTUDIANTE_ID).build();
        seguimientoRechazado.rechazar("Falta detalle", DOCENTE_ID);
    }

    // =================================================================
    // crearSeguimiento() — GPE-170
    // =================================================================

    @Test
    @DisplayName("crearSeguimiento() exitoso debe persistir y actualizar la semana")
    void crearSeguimientoExitoso() {
        stubPlanAprobado();
        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaEnCurso));
        when(seguimientoRepository.existsByInstanciaPractica_IdAndSemana(INSTANCIA_ID, 1)).thenReturn(false);
        when(seguimientoRepository.save(any())).thenReturn(seguimientoPendiente);

        CrearSeguimientoRequest req = new CrearSeguimientoRequest(1, "Actividades", "Logros", "Dificultades", null);
        SeguimientoSemanalResponse resultado = service.crearSeguimiento(INSTANCIA_ID, req, estudianteDetails);

        assertThat(resultado).isNotNull();
        verify(seguimientoRepository).save(any(SeguimientoSemanal.class));
    }

    @Test
    @DisplayName("crearSeguimiento() debe bloquear si el actor no es ESTUDIANTE")
    void crearSeguimientoRolNoEstudianteLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.crearSeguimiento(
                INSTANCIA_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), noAutorizado))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("crearSeguimiento() debe bloquear si la práctica no está EN_CURSO")
    void crearSeguimientoPracticaNoEnCursoLanzaExcepcion() {
        InstanciaPractica asignada = InstanciaPractica.builder()
                .id(INSTANCIA_ID).numeroPractica(1).nombre("P").materiaNucleo("M")
                .codigoMateria("C").numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO).build();

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(asignada));

        assertThatThrownBy(() -> service.crearSeguimiento(
                INSTANCIA_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), estudianteDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("EN_CURSO");
    }

    @Test
    @DisplayName("crearSeguimiento() debe bloquear si no existe plan APROBADO_DOCENTE — OCL requierePlanAprobado")
    void crearSeguimientoSinPlanAprobadoLanzaExcepcion() {
        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaEnCurso));
        when(planRepository.existsByInstanciaPractica_IdAndEstado(INSTANCIA_ID, EstadoPlan.APROBADO_DOCENTE))
                .thenReturn(false); // sin plan aprobado

        assertThatThrownBy(() -> service.crearSeguimiento(
                INSTANCIA_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), estudianteDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("plan");
    }

    @Test
    @DisplayName("crearSeguimiento() debe bloquear si ya existe un seguimiento de esa semana — OCL semanaUnica")
    void crearSeguimientoDuplicadoPorSemanaLanzaExcepcion() {
        stubPlanAprobado();
        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaEnCurso));
        when(seguimientoRepository.existsByInstanciaPractica_IdAndSemana(INSTANCIA_ID, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.crearSeguimiento(
                INSTANCIA_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), estudianteDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("semana 1");

        verify(seguimientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearSeguimiento() debe bloquear si la práctica ya fue calificada por el docente — práctica congelada")
    void crearSeguimientoPracticaCalificadaLanzaExcepcion() {
        when(evaluacionFinalRepository.existsByInstanciaPractica_IdAndTipo(INSTANCIA_ID, TipoEvaluacionFinal.DOCENTE_ASESOR))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crearSeguimiento(
                INSTANCIA_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), estudianteDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("calificada");

        verify(seguimientoRepository, never()).save(any());
    }

    // =================================================================
    // editarSeguimiento() — OCL soloUltimoEditable
    // =================================================================

    @Test
    @DisplayName("editarSeguimiento() exitoso sobre el último seguimiento RECHAZADO")
    void editarSeguimientoRechazadoExitoso() {
        when(seguimientoRepository.findById(SEG_ID)).thenReturn(Optional.of(seguimientoRechazado));
        when(seguimientoRepository.findTopByInstanciaPractica_IdOrderBySemanaDesc(INSTANCIA_ID))
                .thenReturn(Optional.of(seguimientoRechazado)); // es el último
        when(seguimientoRepository.save(any())).thenReturn(seguimientoRechazado);

        CrearSeguimientoRequest req = new CrearSeguimientoRequest(1, "Nuevas Act", "Nuevos Logros", null, null);
        SeguimientoSemanalResponse resultado = service.editarSeguimiento(SEG_ID, req, estudianteDetails);

        assertThat(resultado).isNotNull();
        assertThat(seguimientoRechazado.getActividades()).isEqualTo("Nuevas Act");
        assertThat(seguimientoRechazado.getEstado()).isEqualTo(EstadoSeguimiento.ENVIADO);
        verify(seguimientoRepository).save(seguimientoRechazado);
    }

    @Test
    @DisplayName("editarSeguimiento() debe bloquear si no es el último seguimiento — OCL soloUltimoEditable")
    void editarSeguimientoNoUltimoLanzaExcepcion() {
        SeguimientoSemanal semana2 = SeguimientoSemanal.builder()
                .id(99L).semana(2).instanciaPractica(instanciaEnCurso).build();
        semana2.rechazar("Incompleto", DOCENTE_ID);

        when(seguimientoRepository.findById(SEG_ID)).thenReturn(Optional.of(seguimientoRechazado));
        when(seguimientoRepository.findTopByInstanciaPractica_IdOrderBySemanaDesc(INSTANCIA_ID))
                .thenReturn(Optional.of(semana2)); // el último es semana2, no el que editamos

        assertThatThrownBy(() -> service.editarSeguimiento(
                SEG_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), estudianteDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("más reciente");
    }

    @Test
    @DisplayName("editarSeguimiento() debe bloquear si el estado no es RECHAZADO")
    void editarSeguimientoEnEstadoPendienteLanzaExcepcion() {
        when(seguimientoRepository.findById(SEG_ID)).thenReturn(Optional.of(seguimientoPendiente));
        when(seguimientoRepository.findTopByInstanciaPractica_IdOrderBySemanaDesc(INSTANCIA_ID))
                .thenReturn(Optional.of(seguimientoPendiente));

        assertThatThrownBy(() -> service.editarSeguimiento(
                SEG_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), estudianteDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("RECHAZADO");
    }

    @Test
    @DisplayName("editarSeguimiento() debe bloquear si el actor no es ESTUDIANTE")
    void editarSeguimientoRolNoEstudianteLanzaExcepcion() {
        assertThatThrownBy(() -> service.editarSeguimiento(
                SEG_ID, new CrearSeguimientoRequest(1, "A", "L", null, null), noAutorizado))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // =================================================================
    // marcarRevisado() — solo DOCENTE_ASESOR asignado a esa práctica
    // =================================================================

    @Test
    @DisplayName("marcarRevisado() exitoso por el docente asignado debe cambiar estado a REVISADO")
    void marcarRevisadoExitoso() {
        when(seguimientoRepository.findById(SEG_ID)).thenReturn(Optional.of(seguimientoPendiente));
        when(seguimientoRepository.save(any())).thenReturn(seguimientoPendiente);

        SeguimientoSemanalResponse resultado = service.marcarRevisado(SEG_ID, docenteDetails);

        assertThat(resultado).isNotNull();
        assertThat(seguimientoPendiente.getEstado()).isEqualTo(EstadoSeguimiento.REVISADO);
        assertThat(seguimientoPendiente.getRevisadoPorId()).isEqualTo(DOCENTE_ID);
    }

    @Test
    @DisplayName("marcarRevisado() debe bloquear si el actor no es DOCENTE_ASESOR")
    void marcarRevisadoRolNoDocenteLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.marcarRevisado(SEG_ID, estudianteDetails))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("marcarRevisado() debe bloquear si el docente no está asignado — OCL soloCalificaSusEstudiantes")
    void marcarRevisadoDocenteNoAsignadoLanzaAccesoNoAutorizado() {
        when(seguimientoRepository.findById(SEG_ID)).thenReturn(Optional.of(seguimientoPendiente));

        assertThatThrownBy(() -> service.marcarRevisado(SEG_ID, otroDocente))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("docente asesor asignado");
    }

    @Test
    @DisplayName("marcarRevisado() debe lanzar 404 si el seguimiento no existe")
    void marcarRevisadoNoEncontradoLanza404() {
        when(seguimientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarRevisado(99L, docenteDetails))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =================================================================
    // rechazar() — DOCENTE con observación obligatoria
    // =================================================================

    @Test
    @DisplayName("rechazar() exitoso debe cambiar a RECHAZADO con observaciones del docente")
    void rechazarSeguimientoExitoso() {
        when(seguimientoRepository.findById(SEG_ID)).thenReturn(Optional.of(seguimientoPendiente));
        when(seguimientoRepository.save(any())).thenReturn(seguimientoPendiente);

        SeguimientoSemanalResponse resultado = service.rechazar(
                SEG_ID, new ObservacionDocenteRequest("Faltan evidencias"), docenteDetails);

        assertThat(seguimientoPendiente.getEstado()).isEqualTo(EstadoSeguimiento.RECHAZADO);
        assertThat(seguimientoPendiente.getObservacionesDocente()).isEqualTo("Faltan evidencias");
        assertThat(seguimientoPendiente.getRevisadoPorId()).isEqualTo(DOCENTE_ID);
    }

    @Test
    @DisplayName("rechazar() debe bloquear si el actor no es DOCENTE_ASESOR")
    void rechazarSeguimientoRolNoDocenteLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.rechazar(
                SEG_ID, new ObservacionDocenteRequest("Obs"), estudianteDetails))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("rechazar() debe bloquear si el docente no es el asignado — OCL soloCalificaSusEstudiantes")
    void rechazarSeguimientoDocenteNoAsignadoLanzaAccesoNoAutorizado() {
        when(seguimientoRepository.findById(SEG_ID)).thenReturn(Optional.of(seguimientoPendiente));

        assertThatThrownBy(() -> service.rechazar(
                SEG_ID, new ObservacionDocenteRequest("Obs"), otroDocente))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // =================================================================
    // listarPorInstancia()
    // =================================================================

    @Test
    @DisplayName("listarPorInstancia() retorna todos los seguimientos ordenados por semana")
    void listarPorInstanciaExitoso() {
        when(seguimientoRepository.findByInstanciaPractica_IdOrderBySemanaAsc(INSTANCIA_ID))
                .thenReturn(List.of(seguimientoPendiente));

        List<SeguimientoSemanalResponse> resultado = service.listarPorInstancia(INSTANCIA_ID, docenteDetails);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSemana()).isEqualTo(1);
    }

    // =================================================================
    // Helpers
    // =================================================================

    private void stubPlanAprobado() {
        when(planRepository.existsByInstanciaPractica_IdAndEstado(INSTANCIA_ID, EstadoPlan.APROBADO_DOCENTE))
                .thenReturn(true);
    }

    private CustomUserDetails udConRol(Rol rol, Long id) {
        return new CustomUserDetails(Usuario.builder()
                .id(id).nombre("Test " + rol).correo("test" + id + "@cue.edu.co")
                .passwordHash("hash").rol(rol).activo(true).build());
    }
}
