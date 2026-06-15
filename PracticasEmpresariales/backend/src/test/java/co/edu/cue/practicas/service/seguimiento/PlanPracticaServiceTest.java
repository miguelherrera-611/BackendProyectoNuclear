package co.edu.cue.practicas.service.seguimiento;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.AprobarRechazarPlanRequest;
import co.edu.cue.practicas.dto.request.CrearPlanRequest;
import co.edu.cue.practicas.dto.response.PlanPracticaResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.PlanPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.notificacion.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanPracticaService — Pruebas unitarias (GPE-167)")
class PlanPracticaServiceTest {

    @Mock private PlanPracticaRepository planRepository;
    @Mock private InstanciaPracticaRepository instanciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private EmailService emailService;

    @InjectMocks
    private PlanPracticaService service;

    private CustomUserDetails estudiante;
    private CustomUserDetails docenteDetails;
    private CustomUserDetails tutorDetails;
    private CustomUserDetails noAutorizado;

    private InstanciaPractica instanciaEnCurso;
    private PlanPractica planEnBorrador;
    private PlanPractica planAprobadoTutor;
    private Usuario tutor;

    private static final Long INSTANCIA_ID = 1L;
    private static final Long ESTUDIANTE_ID = 10L;
    private static final Long DOCENTE_ID = 20L;
    private static final Long TUTOR_ID = 30L;
    private static final Long PLAN_ID = 40L;

    @BeforeEach
    void setUp() {
        estudiante = udConRol(Rol.ESTUDIANTE, ESTUDIANTE_ID);
        docenteDetails = udConRol(Rol.DOCENTE_ASESOR, DOCENTE_ID);
        tutorDetails = udConRol(Rol.TUTOR_EMPRESARIAL, TUTOR_ID);
        noAutorizado = udConRol(Rol.ADMIN_DTI, 99L);

        Usuario estudianteUsuario = Usuario.builder()
                .id(ESTUDIANTE_ID).nombre("Est Test").correo("est@cue.edu.co")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        Usuario docenteUsuario = Usuario.builder()
                .id(DOCENTE_ID).nombre("Dr. López").correo("lopez@cue.edu.co")
                .passwordHash("h").rol(Rol.DOCENTE_ASESOR).activo(true).build();

        tutor = Usuario.builder()
                .id(TUTOR_ID).nombre("Tutor Test").correo("tutor@corp.com")
                .passwordHash("h").rol(Rol.TUTOR_EMPRESARIAL).activo(true).build();

        ExpedienteEstudiante expediente = ExpedienteEstudiante.builder()
                .id(1L).estudiante(estudianteUsuario).build();

        instanciaEnCurso = InstanciaPractica.builder()
                .id(INSTANCIA_ID).numeroPractica(1).nombre("Práctica I")
                .materiaNucleo("Práctica").codigoMateria("PE-101")
                .numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.EN_CURSO)
                .docenteAsesor(docenteUsuario).tutorEmpresarial(tutor)
                .expediente(expediente).build();

        planEnBorrador = PlanPractica.builder()
                .id(PLAN_ID).instanciaPractica(instanciaEnCurso)
                .objetivos("Obj iniciales").cronograma("Sem 1-16")
                .cargadoPorId(ESTUDIANTE_ID).build();

        planAprobadoTutor = PlanPractica.builder()
                .id(PLAN_ID).instanciaPractica(instanciaEnCurso)
                .objetivos("Obj iniciales").cronograma("Sem 1-16")
                .cargadoPorId(ESTUDIANTE_ID).build();
        planAprobadoTutor.aprobarPorTutor();
    }

    // =================================================================
    // crearOActualizarPlan() — solo ESTUDIANTE, práctica EN_CURSO
    // =================================================================

    @Test
    @DisplayName("crearOActualizarPlan() exitoso para estudiante en práctica EN_CURSO")
    void crearPlanNuevoExitoso() {
        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaEnCurso));
        when(planRepository.findTopByInstanciaPractica_IdOrderByCreadoEnDesc(INSTANCIA_ID))
                .thenReturn(Optional.empty()); // sin plan previo
        when(planRepository.save(any())).thenReturn(planEnBorrador);

        PlanPracticaResponse resultado = service.crearOActualizarPlan(
                INSTANCIA_ID, new CrearPlanRequest("Objetivos", "Cronograma"), null, estudiante);

        assertThat(resultado).isNotNull();
        verify(planRepository).save(any(PlanPractica.class));
    }

    @Test
    @DisplayName("crearOActualizarPlan() debe bloquear si el actor no es ESTUDIANTE")
    void crearPlanRolNoEstudianteLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.crearOActualizarPlan(
                INSTANCIA_ID, new CrearPlanRequest("Obj", "Cron"), null, noAutorizado))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("crearOActualizarPlan() debe bloquear si la práctica no está EN_CURSO")
    void crearPlanPracticaNoEnCursoLanzaExcepcion() {
        InstanciaPractica instanciaAsignada = InstanciaPractica.builder()
                .id(INSTANCIA_ID).numeroPractica(1).nombre("P").materiaNucleo("M")
                .codigoMateria("C").numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO).build();

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));

        assertThatThrownBy(() -> service.crearOActualizarPlan(
                INSTANCIA_ID, new CrearPlanRequest("Obj", "Cron"), null, estudiante))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("EN_CURSO");
    }

    @Test
    @DisplayName("crearOActualizarPlan() debe bloquear si el plan ya está APROBADO_DOCENTE")
    void crearPlanCuandoYaEstaAprobadoDocenteLanzaExcepcion() {
        PlanPractica planFinalizado = PlanPractica.builder()
                .id(PLAN_ID).instanciaPractica(instanciaEnCurso)
                .objetivos("Obj").cronograma("Cron").cargadoPorId(ESTUDIANTE_ID).build();
        planFinalizado.aprobarPorTutor();
        planFinalizado.aprobarPorDocente();

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaEnCurso));
        when(planRepository.findTopByInstanciaPractica_IdOrderByCreadoEnDesc(INSTANCIA_ID))
                .thenReturn(Optional.of(planFinalizado));

        assertThatThrownBy(() -> service.crearOActualizarPlan(
                INSTANCIA_ID, new CrearPlanRequest("Nuevos obj", "Nuevo cron"), null, estudiante))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("APROBADO_DOCENTE");

        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearOActualizarPlan() debe bloquear si el plan está APROBADO_TUTOR")
    void crearPlanCuandoYaEstaAprobadoTutorLanzaExcepcion() {
        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaEnCurso));
        when(planRepository.findTopByInstanciaPractica_IdOrderByCreadoEnDesc(INSTANCIA_ID))
                .thenReturn(Optional.of(planAprobadoTutor));

        assertThatThrownBy(() -> service.crearOActualizarPlan(
                INSTANCIA_ID, new CrearPlanRequest("Obj", "Cron"), null, estudiante))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("crearOActualizarPlan() sobre plan RECHAZADO debe hacer resubmit y actualizar contenido")
    void actualizarPlanRechazadoHaceResubmit() {
        PlanPractica planRechazado = PlanPractica.builder()
                .id(PLAN_ID).instanciaPractica(instanciaEnCurso)
                .objetivos("Obj viejos").cronograma("Cron viejos").cargadoPorId(ESTUDIANTE_ID).build();
        planRechazado.rechazar("Muy vago", 99L);

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaEnCurso));
        when(planRepository.findTopByInstanciaPractica_IdOrderByCreadoEnDesc(INSTANCIA_ID))
                .thenReturn(Optional.of(planRechazado));
        when(planRepository.save(any())).thenReturn(planRechazado);

        service.crearOActualizarPlan(INSTANCIA_ID, new CrearPlanRequest("Nuevos obj", "Nuevo cron"), null, estudiante);

        assertThat(planRechazado.getEstado()).isEqualTo(EstadoPlan.BORRADOR);
        assertThat(planRechazado.getObjetivos()).isEqualTo("Nuevos obj");
        verify(planRepository).save(planRechazado);
    }

    // =================================================================
    // aprobarPorTutor() — solo TUTOR_EMPRESARIAL asignado a esa práctica
    // =================================================================

    @Test
    @DisplayName("aprobarPorTutor() exitoso debe cambiar estado a APROBADO_TUTOR")
    void aprobarPorTutorExitoso() {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(planEnBorrador));
        when(usuarioRepository.findByCorreoAndActivoTrue(tutorDetails.getUsername()))
                .thenReturn(Optional.of(tutor));
        when(planRepository.save(any())).thenReturn(planEnBorrador);

        PlanPracticaResponse resultado = service.aprobarPorTutor(PLAN_ID, tutorDetails);

        assertThat(resultado).isNotNull();
        assertThat(planEnBorrador.getEstado()).isEqualTo(EstadoPlan.APROBADO_TUTOR);
    }

    @Test
    @DisplayName("aprobarPorTutor() debe bloquear si el actor no es TUTOR_EMPRESARIAL")
    void aprobarPorTutorRolNoTutorLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.aprobarPorTutor(PLAN_ID, estudiante))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("aprobarPorTutor() debe bloquear si el tutor no está asignado a esa práctica")
    void aprobarPorTutorNoAsignadoLanzaAccesoNoAutorizado() {
        Usuario otroTutor = Usuario.builder()
                .id(99L).nombre("Otro Tutor").correo("otro@corp.com")
                .passwordHash("h").rol(Rol.TUTOR_EMPRESARIAL).activo(true).build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(planEnBorrador));
        when(usuarioRepository.findByCorreoAndActivoTrue(tutorDetails.getUsername()))
                .thenReturn(Optional.of(otroTutor)); // tutor diferente al asignado

        assertThatThrownBy(() -> service.aprobarPorTutor(PLAN_ID, tutorDetails))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("tutor asignado");
    }

    // =================================================================
    // aprobarPorDocente() — solo DOCENTE_ASESOR asignado a esa práctica
    // =================================================================

    @Test
    @DisplayName("aprobarPorDocente() exitoso debe cambiar estado a APROBADO_DOCENTE")
    void aprobarPorDocenteExitoso() {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(planAprobadoTutor));
        when(planRepository.save(any())).thenReturn(planAprobadoTutor);

        PlanPracticaResponse resultado = service.aprobarPorDocente(PLAN_ID, docenteDetails);

        assertThat(planAprobadoTutor.getEstado()).isEqualTo(EstadoPlan.APROBADO_DOCENTE);
        assertThat(planAprobadoTutor.estaAprobadoParaSeguimiento()).isTrue();
    }

    @Test
    @DisplayName("aprobarPorDocente() debe bloquear si el actor no es DOCENTE_ASESOR")
    void aprobarPorDocenteRolNodocenteLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.aprobarPorDocente(PLAN_ID, estudiante))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("aprobarPorDocente() debe bloquear si el docente no está asignado a esa práctica")
    void aprobarPorDocenteNoAsignadoLanzaAccesoNoAutorizado() {
        CustomUserDetails otroDocente = udConRol(Rol.DOCENTE_ASESOR, 999L);

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(planAprobadoTutor));

        assertThatThrownBy(() -> service.aprobarPorDocente(PLAN_ID, otroDocente))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("docente asesor asignado");
    }

    @Test
    @DisplayName("aprobarPorDocente() desde BORRADOR debe cambiar a APROBADO_DOCENTE directamente")
    void aprobarPorDocenteDesdeBorradorExitoso() {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(planEnBorrador));
        when(planRepository.save(any())).thenReturn(planEnBorrador);

        PlanPracticaResponse resultado = service.aprobarPorDocente(PLAN_ID, docenteDetails);

        assertThat(planEnBorrador.getEstado()).isEqualTo(EstadoPlan.APROBADO_DOCENTE);
        assertThat(planEnBorrador.estaAprobadoParaSeguimiento()).isTrue();
    }

    // =================================================================
    // rechazarPlan() — DOCENTE o TUTOR con motivo obligatorio
    // =================================================================

    @Test
    @DisplayName("rechazarPlan() exitoso por docente debe cambiar estado a RECHAZADO")
    void rechazarPlanPorDocenteExitoso() {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(planAprobadoTutor));
        when(planRepository.save(any())).thenReturn(planAprobadoTutor);

        PlanPracticaResponse resultado = service.rechazarPlan(
                PLAN_ID, new AprobarRechazarPlanRequest("Cronograma incompleto"), docenteDetails);

        assertThat(planAprobadoTutor.getEstado()).isEqualTo(EstadoPlan.RECHAZADO);
        assertThat(planAprobadoTutor.getMotivoRechazo()).isEqualTo("Cronograma incompleto");
    }

    @Test
    @DisplayName("rechazarPlan() sin motivo debe lanzar excepción — motivo obligatorio")
    void rechazarPlanSinMotivoLanzaExcepcion() {
        // La validación del motivo ocurre ANTES de buscarPlan(), no se llama al repo
        assertThatThrownBy(() -> service.rechazarPlan(
                PLAN_ID, new AprobarRechazarPlanRequest(""), docenteDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("motivo");

        verify(planRepository, never()).findById(any());
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("rechazarPlan() con rol no autorizado lanza AccesoNoAutorizado")
    void rechazarPlanRolNoAutorizadoLanzaExcepcion() {
        assertThatThrownBy(() -> service.rechazarPlan(
                PLAN_ID, new AprobarRechazarPlanRequest("Motivo"), estudiante))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("rechazarPlan() debe lanzar 404 si el plan no existe")
    void rechazarPlanNoEncontradoLanza404() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rechazarPlan(
                99L, new AprobarRechazarPlanRequest("Motivo"), docenteDetails))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =================================================================
    // obtenerPlanActual() — acceso por rol
    // =================================================================

    @Test
    @DisplayName("obtenerPlanActual() debe retornar null si no existe plan para la instancia")
    void obtenerPlanActualSinPlanRetornaNull() {
        when(planRepository.findTopByInstanciaPractica_IdOrderByCreadoEnDesc(INSTANCIA_ID))
                .thenReturn(Optional.empty());

        assertThat(service.obtenerPlanActual(INSTANCIA_ID, docenteDetails)).isNull();
    }

    // =================================================================
    // Helpers
    // =================================================================

    private CustomUserDetails udConRol(Rol rol, Long id) {
        return new CustomUserDetails(Usuario.builder()
                .id(id).nombre("Test " + rol).correo("test" + id + "@cue.edu.co")
                .passwordHash("hash").rol(rol).activo(true).build());
    }
}
