package co.edu.cue.practicas.service.vinculacion;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.ConfirmarVinculacionRequest;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import co.edu.cue.practicas.service.notificacion.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VinculacionService — Pruebas unitarias (GPE-163 / GPE-164)")
class VinculacionServiceTest {

    @Mock private InstanciaPracticaRepository instanciaPracticaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteMapper mapper;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VinculacionService service;

    private CustomUserDetails coordinador;
    private CustomUserDetails noCoordinador;
    private CustomUserDetails docente;
    private CustomUserDetails tutor;
    private InstanciaPractica instanciaAsignada;
    private InstanciaPracticaResponse responseEjemplo;

    private static final Long INSTANCIA_ID = 1L;
    private static final LocalDate INICIO = LocalDate.now();
    private static final LocalDate FIN = LocalDate.now().plusMonths(4);

    @BeforeEach
    void setUp() {
        coordinador = udConRol(Rol.COORDINADOR_PRACTICAS, 1L);
        noCoordinador = udConRol(Rol.ESTUDIANTE, 10L);
        docente = udConRol(Rol.DOCENTE_ASESOR, 5L);
        tutor = udConRolYCorreo(Rol.TUTOR_EMPRESARIAL, 6L, "tutor@corp.com");

        Empresa empresa = Empresa.builder().id(1L).razonSocial("TechCorp")
                .correo("tech@corp.com").nombreContacto("Juan").estado(EstadoEmpresa.APROBADA).build();

        Usuario tutor = Usuario.builder().id(3L).rol(Rol.TUTOR_EMPRESARIAL)
                .nombre("Tutor Test").correo("tutor@corp.com").passwordHash("h").activo(true).build();

        Usuario estudianteUsuario = Usuario.builder()
                .id(10L).nombre("Est Test").correo("est@cue.edu.co")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        ExpedienteEstudiante expediente = ExpedienteEstudiante.builder()
                .id(1L).estudiante(estudianteUsuario).build();

        instanciaAsignada = InstanciaPractica.builder()
                .id(INSTANCIA_ID).numeroPractica(1).nombre("Práctica I")
                .materiaNucleo("Práctica").codigoMateria("PE-101")
                .numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO)
                .empresa(empresa).tutorEmpresarial(tutor)
                .expediente(expediente).build();

        responseEjemplo = InstanciaPracticaResponse.builder()
                .id(INSTANCIA_ID).estado(EstadoPractica.EN_CURSO)
                .fechaInicio(INICIO).fechaFin(FIN)
                .firmaTutor(true).firmaDocente(true).firmaEstudiante(true).build();
    }

    // =================================================================
    // confirmarVinculacion() — GPE-164
    // =================================================================

    @Test
    @DisplayName("confirmarVinculacion() exitoso debe activar EN_CURSO y publicar evento")
    void confirmarVinculacionExitosoActivaEnCurso() {
        ConfirmarVinculacionRequest req = new ConfirmarVinculacionRequest(
                INICIO, FIN, true, true, true, null);

        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));
        when(instanciaPracticaRepository.save(any())).thenReturn(instanciaAsignada);
        when(mapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);

        InstanciaPracticaResponse resultado = service.confirmarVinculacion(INSTANCIA_ID, req, coordinador);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPractica.EN_CURSO);
        assertThat(instanciaAsignada.getEstado()).isEqualTo(EstadoPractica.EN_CURSO);
        assertThat(instanciaAsignada.getFechaInicio()).isEqualTo(INICIO);
        assertThat(instanciaAsignada.getFechaFin()).isEqualTo(FIN);
        verify(instanciaPracticaRepository).save(instanciaAsignada);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("confirmarVinculacion() debe bloquear si el actor no es coordinador")
    void confirmarVinculacionRolNoCoordinadorLanzaAccesoNoAutorizado() {
        ConfirmarVinculacionRequest req = new ConfirmarVinculacionRequest(INICIO, FIN, true, true, true, null);

        assertThatThrownBy(() -> service.confirmarVinculacion(INSTANCIA_ID, req, noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verify(instanciaPracticaRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmarVinculacion() sin las tres firmas debe lanzar excepción — GPE-163")
    void confirmarVinculacionSinTresFirmasLanzaExcepcion() {
        // Solo dos firmas (falta la del estudiante)
        ConfirmarVinculacionRequest req = new ConfirmarVinculacionRequest(
                INICIO, FIN, true, true, false, null);

        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));

        assertThatThrownBy(() -> service.confirmarVinculacion(INSTANCIA_ID, req, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("firmas");

        verify(instanciaPracticaRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmarVinculacion() con fecha fin anterior a inicio debe lanzar excepción")
    void confirmarVinculacionFechaFinAnteriorAInicioLanzaExcepcion() {
        LocalDate fechaFinAnterior = INICIO.minusDays(1);
        ConfirmarVinculacionRequest req = new ConfirmarVinculacionRequest(
                INICIO, fechaFinAnterior, true, true, true, null);

        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));

        assertThatThrownBy(() -> service.confirmarVinculacion(INSTANCIA_ID, req, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("fecha");

        verify(instanciaPracticaRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmarVinculacion() debe lanzar 404 si la instancia no existe")
    void confirmarVinculacionInstanciaNoEncontradaLanza404() {
        ConfirmarVinculacionRequest req = new ConfirmarVinculacionRequest(INICIO, FIN, true, true, true, null);
        when(instanciaPracticaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarVinculacion(99L, req, coordinador))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("confirmarVinculacion() con docenteAsesorId válido lo asigna a la instancia")
    void confirmarVinculacionAsignaDocenteAsesor() {
        Usuario docenteUsuario = Usuario.builder().id(5L).rol(Rol.DOCENTE_ASESOR)
                .nombre("Dr. López").correo("lopez@cue.edu.co").passwordHash("h").activo(true).build();

        ConfirmarVinculacionRequest req = new ConfirmarVinculacionRequest(
                INICIO, FIN, true, true, true, 5L);

        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(docenteUsuario));
        when(instanciaPracticaRepository.save(any())).thenReturn(instanciaAsignada);
        when(mapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);

        service.confirmarVinculacion(INSTANCIA_ID, req, coordinador);

        assertThat(instanciaAsignada.getDocenteAsesor()).isNotNull();
        assertThat(instanciaAsignada.getDocenteAsesor().getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("confirmarVinculacion() con docenteAsesorId que no es DOCENTE_ASESOR lanza excepción")
    void confirmarVinculacionDocenteConRolIncorrectoLanzaExcepcion() {
        Usuario noDocente = Usuario.builder().id(5L).rol(Rol.ADMIN_DTI)
                .nombre("Admin").correo("admin@cue.edu.co").passwordHash("h").activo(true).build();

        ConfirmarVinculacionRequest req = new ConfirmarVinculacionRequest(
                INICIO, FIN, true, true, true, 5L);

        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(noDocente));

        assertThatThrownBy(() -> service.confirmarVinculacion(INSTANCIA_ID, req, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Docente Asesor");
    }

    // =================================================================
    // registrarFirma() — GPE-163
    // =================================================================

    @Test
    @DisplayName("registrarFirma() TUTOR por coordinador debe marcar firmaTutor=true")
    void registrarFirmaTutorPorCoordinadorExitoso() {
        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));
        when(instanciaPracticaRepository.save(any())).thenReturn(instanciaAsignada);
        when(mapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);

        service.registrarFirma(INSTANCIA_ID, "TUTOR", coordinador);

        assertThat(instanciaAsignada.isFirmaTutor()).isTrue();
        verify(instanciaPracticaRepository).save(instanciaAsignada);
    }

    @Test
    @DisplayName("registrarFirma() DOCENTE por docente asesor debe marcar firmaDocente=true")
    void registrarFirmaDocentePorDocenteExitoso() {
        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));
        when(instanciaPracticaRepository.save(any())).thenReturn(instanciaAsignada);
        when(mapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);

        service.registrarFirma(INSTANCIA_ID, "DOCENTE", docente);

        assertThat(instanciaAsignada.isFirmaDocente()).isTrue();
    }

    @Test
    @DisplayName("registrarFirma() con tipo inválido debe lanzar excepción")
    void registrarFirmaConTipoInvalidoLanzaExcepcion() {
        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));

        assertThatThrownBy(() -> service.registrarFirma(INSTANCIA_ID, "GERENTE", coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("TUTOR");
    }

    @Test
    @DisplayName("registrarFirma() TUTOR por rol no autorizado debe lanzar AccesoNoAutorizado")
    void registrarFirmaTutorPorRolNoAutorizadoLanzaExcepcion() {
        when(instanciaPracticaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaAsignada));

        assertThatThrownBy(() -> service.registrarFirma(INSTANCIA_ID, "TUTOR", noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // =================================================================
    // listarPracticasEnCurso() — GPE-167
    // =================================================================

    @Test
    @DisplayName("listarPracticasEnCurso() retorna las prácticas EN_CURSO del sistema")
    void listarPracticasEnCursoExitoso() {
        List<InstanciaPractica> practicas = List.of(instanciaAsignada);
        instanciaAsignada.iniciar(); // → EN_CURSO
        when(instanciaPracticaRepository.findAllByEstado(EstadoPractica.EN_CURSO)).thenReturn(practicas);
        when(mapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);

        List<InstanciaPracticaResponse> resultado = service.listarPracticasEnCurso(coordinador);

        assertThat(resultado).hasSize(1);
        verify(instanciaPracticaRepository).findAllByEstado(EstadoPractica.EN_CURSO);
    }

    @Test
    @DisplayName("listarPracticasEnCurso() con rol sin acceso lanza AccesoNoAutorizado")
    void listarPracticasEnCursoRolNoAutorizadoLanzaExcepcion() {
        assertThatThrownBy(() -> service.listarPracticasEnCurso(noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("listarMisPracticantes() para tutor retorna prácticas por correo del tutor asignado")
    void listarMisPracticantesTutorExitoso() {
        when(instanciaPracticaRepository.findByTutorEmpresarial_CorreoIgnoreCaseAndEstadoNotIn(
                eq("tutor@corp.com"), anyList()))
                .thenReturn(List.of(instanciaAsignada));
        when(mapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);

        List<InstanciaPracticaResponse> resultado = service.listarMisPracticantes(tutor);

        assertThat(resultado).hasSize(1);
        verify(instanciaPracticaRepository).findByTutorEmpresarial_CorreoIgnoreCaseAndEstadoNotIn(
                eq("tutor@corp.com"),
                eq(List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA)));
    }

    @Test
    @DisplayName("listarMisPracticantes() bloquea roles no autorizados")
    void listarMisPracticantesRolNoAutorizado() {
        assertThatThrownBy(() -> service.listarMisPracticantes(noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // =================================================================
    // Helpers
    // =================================================================

    private CustomUserDetails udConRol(Rol rol, Long id) {
        return udConRolYCorreo(rol, id, "test@cue.edu.co");
    }

    private CustomUserDetails udConRolYCorreo(Rol rol, Long id, String correo) {
        return new CustomUserDetails(Usuario.builder()
                .id(id).nombre("Test " + rol).correo(correo)
                .passwordHash("hash").rol(rol).activo(true).build());
    }
}
