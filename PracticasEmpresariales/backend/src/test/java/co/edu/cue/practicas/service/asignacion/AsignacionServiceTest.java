package co.edu.cue.practicas.service.asignacion;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearAsignacionRequest;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.repository.catalogo.CatalogoPracticaRepository;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.tutor.TutorEmpresarialRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.repository.vacante.VacanteRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsignacionService — Pruebas unitarias (GPE-157 / GPE-158 / GPE-159)")
class AsignacionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private VacanteRepository vacanteRepository;
    @Mock private ExpedienteEstudianteRepository expedienteRepository;
    @Mock private CatalogoPracticaRepository catalogoRepository;
    @Mock private InstanciaPracticaRepository instanciaRepository;
    @Mock private TutorEmpresarialRepository tutorRepository;
    @Mock private EstudianteMapper estudianteMapper;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private EmailService emailService;

    @InjectMocks
    private AsignacionService service;

    // ── Entidades de prueba ───────────────────────────────────────────
    private CustomUserDetails coordinador;
    private CustomUserDetails noCoordinador;
    private Usuario estudiante;
    private Empresa empresa;
    private Vacante vacante;
    private ExpedienteEstudiante expediente;
    private CatalogoPractica catalogo;
    private InstanciaPracticaResponse responseEjemplo;

    private static final Long ESTUDIANTE_ID = 10L;
    private static final Long VACANTE_ID = 20L;
    private static final Long CATALOGO_ID = 30L;
    private static final Long INSTANCIA_ID = 40L;

    @BeforeEach
    void setUp() {
        coordinador = udConRol(Rol.COORDINADOR_PRACTICAS);
        noCoordinador = udConRol(Rol.ESTUDIANTE);

        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas").activo(true).build();

        estudiante = Usuario.builder()
                .id(ESTUDIANTE_ID).nombre("Ana García").correo("ana@cue.edu.co")
                .passwordHash("hash").rol(Rol.ESTUDIANTE)
                .estadoEstudiante(EstadoEstudiante.APTO).enviadoAlProceso(true)
                .programa(programa).activo(true).build();

        empresa = Empresa.builder()
                .id(1L).razonSocial("TechCorp S.A.").nit("900.1")
                .correo("tech@corp.com").nombreContacto("Luis").estado(EstadoEmpresa.APROBADA).build();

        vacante = Vacante.builder()
                .id(VACANTE_ID).empresa(empresa).area("Desarrollo de Software")
                .cuposTotales(2).cuposOcupados(0).estado(EstadoVacante.DISPONIBLE).build();

        expediente = ExpedienteEstudiante.builder()
                .id(1L).estudiante(estudiante).practicas(new ArrayList<>()).build();

        catalogo = CatalogoPractica.builder()
                .id(CATALOGO_ID).numeroPractica(1).nombre("Práctica Empresarial I")
                .materiaNucleo("Práctica Empresarial").codigoMateria("PE-101")
                .numCortes(3).duracionSemanas(16).activo(true).build();

        responseEjemplo = InstanciaPracticaResponse.builder()
                .id(INSTANCIA_ID).numeroPractica(1)
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO)
                .razonSocialEmpresa("TechCorp S.A.").build();
    }

    // =================================================================
    // asignar() — GPE-157
    // =================================================================

    @Test
    @DisplayName("asignar() exitoso debe crear instancia, decrementar cupo y notificar")
    void asignarExitosoDebeCrearInstanciaYDecrementarCupo() {
        stubsParaAsignarExitoso();

        CrearAsignacionRequest req = reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID);
        InstanciaPracticaResponse resultado = service.asignar(req, coordinador);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPractica.ASIGNADA_PENDIENTE_INICIO);
        assertThat(vacante.getCuposOcupados()).isEqualTo(1);
        verify(instanciaRepository).save(any(InstanciaPractica.class));
        verify(vacanteRepository).save(vacante);
    }

    @Test
    @DisplayName("asignar() debe bloquear si el actor no es COORDINADOR_PRACTICAS — OCL soloCoordinacionCrea")
    void asignarConRolNoCoordinadorLanzaAccesoNoAutorizado() {
        CrearAsignacionRequest req = reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID);

        assertThatThrownBy(() -> service.asignar(req, noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verify(instanciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("asignar() debe bloquear si el estudiante no fue enviado al proceso")
    void asignarEstudianteNoEnviadoAlProcesoLanzaException() {
        estudiante = estudianteBuilder().enviadoAlProceso(false).build();
        when(usuarioRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> service.asignar(reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID), coordinador))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("asignar() debe bloquear si el estudiante no es APTO")
    void asignarEstudianteNoAptoLanzaException() {
        estudiante = estudianteBuilder().estadoEstudiante(EstadoEstudiante.NO_APTO).enviadoAlProceso(true).build();
        when(usuarioRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> service.asignar(reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID), coordinador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APTO");
    }

    @Test
    @DisplayName("asignar() debe bloquear si la vacante no tiene cupos disponibles")
    void asignarVacanteSinCuposLanzaException() {
        vacante = Vacante.builder()
                .id(VACANTE_ID).empresa(empresa).area("Dev")
                .cuposTotales(1).cuposOcupados(1).estado(EstadoVacante.DISPONIBLE).build();

        when(usuarioRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante));
        when(vacanteRepository.findById(VACANTE_ID)).thenReturn(Optional.of(vacante));

        assertThatThrownBy(() -> service.asignar(reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID), coordinador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("practicantes");
    }

    @Test
    @DisplayName("asignar() debe bloquear si ya tiene práctica EN_CURSO — OCL maxUnaPracticaActiva")
    void asignarConPracticaActivaLanzaOperacionNoPermitida() {
        when(usuarioRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante));
        when(vacanteRepository.findById(VACANTE_ID)).thenReturn(Optional.of(vacante));
        when(expedienteRepository.findByEstudiante_Id(ESTUDIANTE_ID)).thenReturn(Optional.of(expediente));
        when(catalogoRepository.findById(CATALOGO_ID)).thenReturn(Optional.of(catalogo));
        when(instanciaRepository.countByExpediente_Estudiante_IdAndEstado(ESTUDIANTE_ID, EstadoPractica.EN_CURSO))
                .thenReturn(1L); // ya tiene una práctica activa

        assertThatThrownBy(() -> service.asignar(reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID), coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("EN_CURSO");

        verify(instanciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("asignar() debe lanzar 404 si el estudiante no existe")
    void asignarEstudianteNoEncontradoLanza404() {
        when(usuarioRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignar(reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID), coordinador))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("asignar() debe lanzar 404 si la vacante no existe")
    void asignarVacanteNoEncontradaLanza404() {
        when(usuarioRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante));
        when(vacanteRepository.findById(VACANTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignar(reqAsignacion(ESTUDIANTE_ID, VACANTE_ID, CATALOGO_ID), coordinador))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("asignar() con docente y tutor opcionales debe asignarlos a la instancia")
    void asignarConDocenteYTutorOpcionalesExitoso() {
        Usuario docente = Usuario.builder().id(50L).rol(Rol.DOCENTE_ASESOR)
                .nombre("Dr. Pérez").correo("perez@cue.edu.co").passwordHash("h").activo(true).build();
        TutorEmpresarial tutor = TutorEmpresarial.builder().id(60L)
                .nombre("Carlos Tutor").correo("ctutor@tech.com").activo(true).build();

        stubsParaAsignarExitoso();
        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(docente));
        when(tutorRepository.findById(60L)).thenReturn(Optional.of(tutor));

        CrearAsignacionRequest req = CrearAsignacionRequest.builder()
                .estudianteId(ESTUDIANTE_ID).vacanteId(VACANTE_ID)
                .catalogoPracticaId(CATALOGO_ID).docenteAsesorId(50L).tutorEmpresarialId(60L)
                .build();

        service.asignar(req, coordinador);

        verify(instanciaRepository).save(argThat(i ->
                i.getDocenteAsesor() != null && i.getDocenteAsesor().getId().equals(50L) &&
                i.getTutorEmpresarial() != null && i.getTutorEmpresarial().getId().equals(60L)));
    }

    // =================================================================
    // cancelarAsignacion() — GPE-158
    // =================================================================

    @Test
    @DisplayName("cancelarAsignacion() exitoso debe cambiar a CANCELADA y liberar cupo")
    void cancelarAsignacionExitosoLiberaCupo() {
        vacante.ocuparCupo(); // cuposOcupados = 1
        InstanciaPractica instancia = instanciaAsignada();
        instancia.setVacanteId(VACANTE_ID);

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instancia));
        when(vacanteRepository.findById(VACANTE_ID)).thenReturn(Optional.of(vacante));

        service.cancelarAsignacion(INSTANCIA_ID, "Estudiante no cumple perfil", coordinador);

        assertThat(instancia.getEstado()).isEqualTo(EstadoPractica.CANCELADA);
        assertThat(vacante.getCuposOcupados()).isEqualTo(0); // cupo liberado
        verify(instanciaRepository).save(instancia);
        verify(vacanteRepository).save(vacante);
    }

    @Test
    @DisplayName("cancelarAsignacion() debe bloquear si el actor no es COORDINADOR_PRACTICAS")
    void cancelarConRolNoCoordinadorLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.cancelarAsignacion(INSTANCIA_ID, "motivo", noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verify(instanciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelarAsignacion() debe bloquear si la práctica ya está EN_CURSO")
    void cancelarPracticaEnCursoLanzaOperacionNoPermitida() {
        InstanciaPractica instancia = instanciaAsignada();
        instancia.iniciar(); // → EN_CURSO

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instancia));

        assertThatThrownBy(() -> service.cancelarAsignacion(INSTANCIA_ID, "motivo", coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(instanciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelarAsignacion() debe bloquear si la práctica ya está FINALIZADA")
    void cancelarPracticaFinalizadaLanzaOperacionNoPermitida() {
        InstanciaPractica instancia = instanciaAsignada();
        instancia.iniciar();
        instancia.finalizar(); // → FINALIZADA

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instancia));

        assertThatThrownBy(() -> service.cancelarAsignacion(INSTANCIA_ID, "motivo", coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("cancelarAsignacion() debe lanzar 404 si la instancia no existe")
    void cancelarInstanciaNoEncontradaLanza404() {
        when(instanciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelarAsignacion(99L, "motivo", coordinador))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =================================================================
    // listarAsignaciones() — GPE-158
    // =================================================================

    @Test
    @DisplayName("listarAsignaciones() sin filtro debe retornar todas las instancias")
    void listarAsignacionesSinFiltroRetornaTodas() {
        List<InstanciaPractica> instancias = List.of(instanciaAsignada(), instanciaAsignada());
        when(instanciaRepository.findAll()).thenReturn(instancias);
        when(estudianteMapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);

        List<InstanciaPracticaResponse> resultado = service.listarAsignaciones(null, coordinador);

        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("listarAsignaciones() con filtro de estado válido usa findAllByEstado")
    void listarAsignacionesConFiltroEstadoValido() {
        when(instanciaRepository.findAllByEstado(EstadoPractica.EN_CURSO)).thenReturn(List.of());

        service.listarAsignaciones("EN_CURSO", coordinador);

        verify(instanciaRepository).findAllByEstado(EstadoPractica.EN_CURSO);
        verify(instanciaRepository, never()).findAll();
    }

    @Test
    @DisplayName("listarAsignaciones() con estado inválido debe lanzar excepción")
    void listarAsignacionesConEstadoInvalidoLanzaException() {
        assertThatThrownBy(() -> service.listarAsignaciones("ESTADO_INVENTADO", coordinador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ESTADO_INVENTADO");
    }

    @Test
    @DisplayName("listarAsignaciones() con rol no permitido lanza AccesoNoAutorizado")
    void listarAsignacionesRolNoCoordinadorLanzaAccesoNoAutorizado() {
        assertThatThrownBy(() -> service.listarAsignaciones(null, noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // =================================================================
    // obtenerAsignacion() — GPE-159
    // =================================================================

    @Test
    @DisplayName("obtenerAsignacion() exitoso retorna el DTO de la instancia")
    void obtenerAsignacionExitoso() {
        InstanciaPractica instancia = instanciaAsignada();
        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instancia));
        when(estudianteMapper.toInstanciaPracticaResponse(instancia)).thenReturn(responseEjemplo);

        InstanciaPracticaResponse resultado = service.obtenerAsignacion(INSTANCIA_ID, coordinador);

        assertThat(resultado.getId()).isEqualTo(INSTANCIA_ID);
    }

    @Test
    @DisplayName("obtenerAsignacion() debe lanzar 404 si no existe")
    void obtenerAsignacionNoEncontradaLanza404() {
        when(instanciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerAsignacion(99L, coordinador))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =================================================================
    // Helpers privados
    // =================================================================

    private void stubsParaAsignarExitoso() {
        when(usuarioRepository.findById(ESTUDIANTE_ID)).thenReturn(Optional.of(estudiante));
        when(vacanteRepository.findById(VACANTE_ID)).thenReturn(Optional.of(vacante));
        when(expedienteRepository.findByEstudiante_Id(ESTUDIANTE_ID)).thenReturn(Optional.of(expediente));
        when(catalogoRepository.findById(CATALOGO_ID)).thenReturn(Optional.of(catalogo));
        when(instanciaRepository.countByExpediente_Estudiante_IdAndEstado(
                eq(ESTUDIANTE_ID), eq(EstadoPractica.EN_CURSO))).thenReturn(0L);
        when(estudianteMapper.toInstanciaPracticaResponse(any())).thenReturn(responseEjemplo);
    }

    private CrearAsignacionRequest reqAsignacion(Long estudianteId, Long vacanteId, Long catalogoId) {
        return CrearAsignacionRequest.builder()
                .estudianteId(estudianteId).vacanteId(vacanteId)
                .catalogoPracticaId(catalogoId).build();
    }

    private InstanciaPractica instanciaAsignada() {
        return InstanciaPractica.builder()
                .id(INSTANCIA_ID).numeroPractica(1).nombre("Práctica I")
                .materiaNucleo("Práctica").codigoMateria("PE-101")
                .numCortes(3).duracionSemanas(16)
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO)
                .empresa(empresa).catalogoPracticaId(CATALOGO_ID).build();
    }

    private Usuario.UsuarioBuilder estudianteBuilder() {
        return Usuario.builder()
                .id(ESTUDIANTE_ID).nombre("Ana García").correo("ana@cue.edu.co")
                .passwordHash("hash").rol(Rol.ESTUDIANTE).activo(true);
    }

    private CustomUserDetails udConRol(Rol rol) {
        return new CustomUserDetails(Usuario.builder()
                .id(1L).nombre("Coordinador Test").correo("coord@cue.edu.co")
                .passwordHash("hash").rol(rol).activo(true).build());
    }
}
