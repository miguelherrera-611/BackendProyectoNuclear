package co.edu.cue.practicas.service.auth;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CambiarPasswordRequest;
import co.edu.cue.practicas.dto.request.LoginRequest;
import co.edu.cue.practicas.dto.response.LoginResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de AuthService.
 *
 * @ExtendWith(MockitoExtension.class) activa Mockito para esta clase.
 * @Mock crea un objeto falso (mock) de cada dependencia — no se conecta a nada real.
 * @InjectMocks crea el AuthService real e inyecta los mocks como sus dependencias.
 *
 * Patrón de cada test: ARRANGE → ACT → ASSERT
 *   ARRANGE: preparamos los datos y configuramos qué devuelven los mocks (when/thenReturn)
 *   ACT:     ejecutamos el método que queremos probar
 *   ASSERT:  verificamos que el resultado es el esperado (assertThat)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Pruebas unitarias")
class AuthServiceTest {

    // @Mock crea versiones falsas de cada dependencia
    // Estas versiones no hacen nada por defecto; nosotros les decimos qué devolver
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaLogger auditoriaLogger;

    // @InjectMocks crea el AuthService REAL e inyecta los mocks anteriores
    @InjectMocks
    private AuthService authService;

    // Datos de prueba reutilizados en varios tests
    private Usuario usuarioEjemplo;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Creamos un usuario de prueba que representa al DTI
        usuarioEjemplo = Usuario.builder()
                .id(1L)
                .nombre("Admin DTI Test")
                .correo("dti@test.com")
                .passwordHash("$2a$hash_bcrypt_falso")
                .rol(Rol.ADMIN_DTI)
                .activo(true)
                .primerIngreso(false)
                .build();

        // Envolvemos el usuario en CustomUserDetails (como lo hace Spring Security)
        userDetails = new CustomUserDetails(usuarioEjemplo);
    }

    // =================================================================
    // TESTS DEL MÉTODO login()
    // =================================================================

    @Test
    @DisplayName("Login exitoso debe retornar token y datos del usuario")
    void loginExitosoDebeRetornarToken() {
        // ARRANGE
        LoginRequest request = new LoginRequest("dti@test.com", "Admin2026!");

        // Configuramos el mock: cuando authenticationManager.authenticate() sea llamado,
        // devolvemos un objeto Authentication con nuestro userDetails
        Authentication authMock = mock(Authentication.class);
        when(authMock.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);

        // Configuramos el mock de JwtUtil para que devuelva un token de prueba
        when(jwtUtil.generarToken(userDetails)).thenReturn("token.jwt.prueba");

        // Configuramos el mock de usuarioRepository.save() para que no haga nada
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEjemplo);

        // ACT
        LoginResponse response = authService.login(request, "127.0.0.1");

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token.jwt.prueba");
        assertThat(response.getCorreo()).isEqualTo("dti@test.com");
        assertThat(response.getRol()).isEqualTo(Rol.ADMIN_DTI);

        // Verificamos que la bitácora fue llamada exactamente 1 vez
        verify(auditoriaLogger, times(1)).registrar(any());
    }

    @Test
    @DisplayName("Login con credenciales incorrectas debe lanzar AccesoNoAutorizadoException")
    void loginConCredencialesInvalidasDebeLanzarExcepcion() {
        // ARRANGE
        LoginRequest request = new LoginRequest("dti@test.com", "contraseña_incorrecta");

        // Configuramos el mock para que simule credenciales incorrectas
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        // ACT + ASSERT
        // Verificamos que se lanza la excepción correcta con el mensaje esperado
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("Credenciales incorrectas");

        // Verificamos que se registró el login fallido en la bitácora
        verify(auditoriaLogger, times(1)).registrar(any());
    }

    // =================================================================
    // TESTS DEL MÉTODO cambiarPassword()
    // =================================================================

    @Test
    @DisplayName("Cambio de contraseña exitoso cuando las contraseñas coinciden y la actual es correcta")
    void cambiarPasswordExitoso() {
        // ARRANGE
        CambiarPasswordRequest request = new CambiarPasswordRequest(
                "Admin2026!",     // passwordActual
                "NuevaClave123!", // passwordNueva
                "NuevaClave123!"  // passwordConfirmacion (igual a la nueva)
        );

        // Simulamos que la contraseña actual sí coincide con el hash
        when(passwordEncoder.matches("Admin2026!", usuarioEjemplo.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave123!")).thenReturn("$nuevo_hash_bcrypt");
        when(usuarioRepository.save(any())).thenReturn(usuarioEjemplo);

        // ACT — no lanza excepción = éxito
        assertThatCode(() -> authService.cambiarPassword(request, userDetails))
                .doesNotThrowAnyException();

        // ASSERT — verificamos que se guardó el usuario con la nueva contraseña
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
        verify(auditoriaLogger, times(1)).registrar(any());
    }

    @Test
    @DisplayName("Cambio de contraseña debe fallar si las contraseñas nuevas no coinciden")
    void cambiarPasswordFallaSiConfirmacionNoCoincidie() {
        // ARRANGE
        CambiarPasswordRequest request = new CambiarPasswordRequest(
                "Admin2026!",
                "NuevaClave123!",
                "ClaveDistinta!"  // confirmación diferente a la nueva contraseña
        );

        // ACT + ASSERT
        assertThatThrownBy(() -> authService.cambiarPassword(request, userDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("no coinciden");

        // Verificamos que NO se guardó nada en la BD al fallar la validación
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambio de contraseña debe fallar si la contraseña actual es incorrecta")
    void cambiarPasswordFallaSiContrasenaActualIncorrecta() {
        // ARRANGE
        CambiarPasswordRequest request = new CambiarPasswordRequest(
                "ClaveActualEquivocada",
                "NuevaClave123!",
                "NuevaClave123!"
        );

        // Simulamos que la contraseña actual NO coincide con el hash
        when(passwordEncoder.matches("ClaveActualEquivocada", usuarioEjemplo.getPasswordHash()))
                .thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> authService.cambiarPassword(request, userDetails))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("contraseña actual");

        verify(usuarioRepository, never()).save(any());
    }
}
