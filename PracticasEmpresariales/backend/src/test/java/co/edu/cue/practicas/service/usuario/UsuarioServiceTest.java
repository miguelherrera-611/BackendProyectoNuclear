package co.edu.cue.practicas.service.usuario;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearUsuarioRequest;
import co.edu.cue.practicas.dto.response.UsuarioResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.facultad.FacultadRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de UsuarioService.
 *
 * La anotación @RequiereRol en los métodos del servicio es ignorada en estas pruebas
 * porque sin Spring el Aspecto AOP (ScopeValidationAspect) no se activa.
 * Esto nos permite probar la lógica de negocio pura sin la capa de seguridad.
 *
 * Cómo ejecutar en IntelliJ:
 *   Clic derecho sobre la clase → Run 'UsuarioServiceTest'
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — Pruebas unitarias")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private FacultadRepository facultadRepository;
    @Mock private ProgramaRepository programaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private UsuarioService usuarioService;

    // DTI que actúa como el usuario autenticado que ejecuta las operaciones
    private CustomUserDetails creador;
    private Usuario dtiEjemplo;

    @BeforeEach
    void setUp() {
        dtiEjemplo = Usuario.builder()
                .id(1L)
                .nombre("Admin DTI")
                .correo("dti@test.com")
                .passwordHash("$hash_bcrypt")
                .rol(Rol.ADMIN_DTI)
                .activo(true)
                .primerIngreso(false)
                .build();
        creador = new CustomUserDetails(dtiEjemplo);
    }

    // =================================================================
    // crearUsuario
    // =================================================================

    @Test
    @DisplayName("crearUsuario exitoso debe persistir el usuario, publicar evento y registrar bitácora")
    void crearUsuarioExitoso() {
        // ARRANGE
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Nuevo Admin");
        request.setCorreo("nuevo@test.com");
        request.setRol(Rol.ADMIN_DTI);

        Usuario guardado = Usuario.builder()
                .id(2L).nombre("Nuevo Admin").correo("nuevo@test.com")
                .passwordHash("$hash").rol(Rol.ADMIN_DTI).activo(true).primerIngreso(true)
                .build();

        when(usuarioRepository.existsByCorreo("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        // ACT
        UsuarioResponse response = usuarioService.crearUsuario(request, creador);

        // ASSERT
        assertThat(response.getCorreo()).isEqualTo("nuevo@test.com");
        assertThat(response.getRol()).isEqualTo(Rol.ADMIN_DTI);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
        // PATRON OBSERVER: el evento debe publicarse para que EmailService envíe la contraseña temporal
        verify(eventPublisher, times(1)).publishEvent(any());
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    @Test
    @DisplayName("crearUsuario con correo ya registrado debe lanzar OperacionNoPermitidaException")
    void crearUsuarioCorreoDuplicadoLanzaExcepcion() {
        // ARRANGE
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Duplicado");
        request.setCorreo("duplicado@test.com");
        request.setRol(Rol.ADMIN_DTI);

        when(usuarioRepository.existsByCorreo("duplicado@test.com")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> usuarioService.crearUsuario(request, creador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("correo");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario COORDINADOR_PRACTICAS sin etiquetaCargo debe lanzar excepción")
    void crearUsuarioCoordinadorPracticasSinEtiquetaLanzaExcepcion() {
        // ARRANGE
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Coordinador");
        request.setCorreo("coord@test.com");
        request.setRol(Rol.COORDINADOR_PRACTICAS);
        // etiquetaCargo = null — regla de negocio: obligatorio para este rol

        when(usuarioRepository.existsByCorreo("coord@test.com")).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> usuarioService.crearUsuario(request, creador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("etiqueta de cargo");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario ESTUDIANTE debe asignar estadoEstudiante=NO_APTO (regla OCL)")
    void crearEstudianteDebeAsignarEstadoNoApto() {
        // ARRANGE
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Estudiante Test");
        request.setCorreo("est@test.com");
        request.setRol(Rol.ESTUDIANTE);

        Usuario estudianteGuardado = Usuario.builder()
                .id(3L).nombre("Estudiante Test").correo("est@test.com")
                .passwordHash("$hash").rol(Rol.ESTUDIANTE).activo(true)
                .primerIngreso(true).estadoEstudiante(EstadoEstudiante.NO_APTO)
                .build();

        when(usuarioRepository.existsByCorreo("est@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(estudianteGuardado);

        // Capturamos el objeto Usuario que se pasa a save() para inspeccionarlo
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

        // ACT
        usuarioService.crearUsuario(request, creador);

        // ASSERT — verificamos que el Usuario construido por el servicio tiene estadoEstudiante=NO_APTO
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoEstudiante()).isEqualTo(EstadoEstudiante.NO_APTO);
    }

    // =================================================================
    // desactivarUsuario
    // =================================================================

    @Test
    @DisplayName("desactivarUsuario debe proteger al único Administrador DTI activo del sistema")
    void desactivarUnicoDtiActivoLanzaExcepcion() {
        // ARRANGE — solo hay 1 DTI activo, no se puede desactivar
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dtiEjemplo));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN_DTI)).thenReturn(1L);

        // ACT + ASSERT
        assertThatThrownBy(() -> usuarioService.desactivarUsuario(1L, creador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("único Administrador DTI");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivarUsuario exitoso cuando existe más de un DTI activo")
    void desactivarDtiConOtroDtiActivoExitoso() {
        // ARRANGE — hay 2 DTIs activos, se puede desactivar uno
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dtiEjemplo));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN_DTI)).thenReturn(2L);
        when(usuarioRepository.save(any())).thenReturn(dtiEjemplo);

        // ACT — no debe lanzar excepción
        assertThatCode(() -> usuarioService.desactivarUsuario(1L, creador))
                .doesNotThrowAnyException();

        // La entidad queda marcada como inactiva (soft delete)
        assertThat(dtiEjemplo.isActivo()).isFalse();
        verify(usuarioRepository, times(1)).save(dtiEjemplo);
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    // =================================================================
    // activarUsuario
    // =================================================================

    @Test
    @DisplayName("activarUsuario debe marcar el usuario como activo y registrar en bitácora")
    void activarUsuarioExitoso() {
        // ARRANGE — usuario inactivo que queremos reactivar
        Usuario usuarioInactivo = Usuario.builder()
                .id(5L)
                .nombre("Inactivo Test")
                .correo("inactivo@test.com")
                .passwordHash("hash")
                .rol(Rol.ESTUDIANTE)
                .activo(false)
                .build();

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuarioInactivo));
        when(usuarioRepository.save(any())).thenReturn(usuarioInactivo);

        // ACT
        assertThatCode(() -> usuarioService.activarUsuario(5L, creador))
                .doesNotThrowAnyException();

        // El usuario debe haber quedado activo
        assertThat(usuarioInactivo.isActivo()).isTrue();
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    // =================================================================
    // obtenerPorId
    // =================================================================

    @Test
    @DisplayName("obtenerPorId debe lanzar RecursoNoEncontradoException si el usuario no existe")
    void obtenerPorIdNoEncontradoLanza404() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }
}
