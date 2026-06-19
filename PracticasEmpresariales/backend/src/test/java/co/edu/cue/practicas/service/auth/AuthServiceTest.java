package co.edu.cue.practicas.service.auth;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CambiarPasswordRequest;
import co.edu.cue.practicas.dto.request.ConfirmarCambioCorreoRequest;
import co.edu.cue.practicas.dto.request.LoginRequest;
import co.edu.cue.practicas.dto.request.VerificarCodigoLoginRequest;
import co.edu.cue.practicas.dto.response.LoginPendienteResponse;
import co.edu.cue.practicas.dto.response.LoginResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.jwt.JwtUtil;
import co.edu.cue.practicas.service.notificacion.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de AuthService — flujo de login con verificación en 2 pasos (2FA por correo).
 *
 * Paso 1 — login(): valida credenciales y envía un código de 6 dígitos al correo (LoginPendienteResponse).
 * Paso 2 — verificarCodigoLogin(): valida el código y entrega el JWT (LoginResponse).
 *
 * Como el código generado es aleatorio e interno (no se expone en la respuesta),
 * lo capturamos con un ArgumentCaptor sobre la llamada a emailService.enviarCodigoLogin().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Pruebas unitarias")
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioEjemplo;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        usuarioEjemplo = Usuario.builder()
                .id(1L)
                .nombre("Admin DTI Test")
                .correo("dti@test.com")
                .passwordHash("$2a$hash_bcrypt_falso")
                .rol(Rol.ADMIN_DTI)
                .activo(true)
                .primerIngreso(false)
                .build();

        userDetails = new CustomUserDetails(usuarioEjemplo);
    }

    // =================================================================
    // TESTS DEL MÉTODO login() — paso 1: envía código 2FA
    // =================================================================

    @Test
    @DisplayName("Login con credenciales válidas debe enviar código 2FA y retornar LoginPendienteResponse")
    void loginExitosoDebeEnviarCodigo2FA() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("dti@test.com");
        request.setPassword("Admin2026!");

        Authentication authMock = mock(Authentication.class);
        when(authMock.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);

        LoginPendienteResponse response = authService.login(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getCorreo()).isEqualTo("dti@test.com");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600);
        verify(emailService).enviarCodigoLogin(eq("dti@test.com"), eq("Admin DTI Test"), any());
        verify(auditoriaLogger, never()).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    @Test
    @DisplayName("Login con credenciales incorrectas debe lanzar AccesoNoAutorizadoException y registrar el fallo")
    void loginConCredencialesInvalidasDebeLanzarExcepcion() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("dti@test.com");
        request.setPassword("contraseña_incorrecta");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("Credenciales incorrectas");

        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
        verify(emailService, never()).enviarCodigoLogin(any(), any(), any());
    }

    // =================================================================
    // TESTS DEL MÉTODO verificarCodigoLogin() — paso 2: entrega el JWT
    // =================================================================

    @Test
    @DisplayName("Verificar código correcto debe retornar LoginResponse con token JWT")
    void verificarCodigoLoginExitosoDebeRetornarToken() {
        String codigo = solicitarLoginYCapturarCodigo();

        when(usuarioRepository.findByCorreoAndActivoTrue("dti@test.com")).thenReturn(Optional.of(usuarioEjemplo));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEjemplo);
        when(jwtUtil.generarToken(any(CustomUserDetails.class))).thenReturn("token.jwt.prueba");

        LoginResponse response = authService.verificarCodigoLogin(
                new VerificarCodigoLoginRequest("dti@test.com", codigo), "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token.jwt.prueba");
        assertThat(response.getCorreo()).isEqualTo("dti@test.com");
        assertThat(response.getRol()).isEqualTo(Rol.ADMIN_DTI);
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    @Test
    @DisplayName("Verificar código incorrecto debe lanzar AccesoNoAutorizadoException")
    void verificarCodigoLoginConCodigoIncorrectoLanzaExcepcion() {
        solicitarLoginYCapturarCodigo();
        when(usuarioRepository.findByCorreoAndActivoTrue("dti@test.com")).thenReturn(Optional.of(usuarioEjemplo));

        assertThatThrownBy(() -> authService.verificarCodigoLogin(
                new VerificarCodigoLoginRequest("dti@test.com", "000000"), "127.0.0.1"))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("incorrecto");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verificar código sin haber solicitado login antes debe lanzar excepción")
    void verificarCodigoLoginSinCodigoPendienteLanzaExcepcion() {
        when(usuarioRepository.findByCorreoAndActivoTrue("dti@test.com")).thenReturn(Optional.of(usuarioEjemplo));

        assertThatThrownBy(() -> authService.verificarCodigoLogin(
                new VerificarCodigoLoginRequest("dti@test.com", "123456"), "127.0.0.1"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("código");
    }

    @Test
    @DisplayName("Verificar código para un correo inexistente/inactivo debe lanzar AccesoNoAutorizadoException")
    void verificarCodigoLoginUsuarioNoEncontradoLanzaExcepcion() {
        when(usuarioRepository.findByCorreoAndActivoTrue("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verificarCodigoLogin(
                new VerificarCodigoLoginRequest("noexiste@test.com", "123456"), "127.0.0.1"))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    /** Ejecuta login() y captura el código de 6 dígitos enviado por correo. */
    private String solicitarLoginYCapturarCodigo() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("dti@test.com");
        request.setPassword("Admin2026!");

        Authentication authMock = mock(Authentication.class);
        when(authMock.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);

        authService.login(request, "127.0.0.1");

        ArgumentCaptor<String> codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoLogin(any(), any(), codigoCaptor.capture());
        clearInvocations(authenticationManager, emailService);
        return codigoCaptor.getValue();
    }

    // =================================================================
    // TESTS DEL MÉTODO cambiarPassword()
    // =================================================================

    @Test
    @DisplayName("Cambio de contraseña exitoso cuando las contraseñas coinciden y la actual es correcta")
    void cambiarPasswordExitoso() {
        CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual("Admin2026!");
        request.setPasswordNueva("NuevaClave123!");
        request.setPasswordConfirmacion("NuevaClave123!");

        when(passwordEncoder.matches("Admin2026!", usuarioEjemplo.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave123!")).thenReturn("$nuevo_hash_bcrypt");
        when(usuarioRepository.save(any())).thenReturn(usuarioEjemplo);

        assertThatCode(() -> authService.cambiarPassword(request, userDetails))
                .doesNotThrowAnyException();

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    @Test
    @DisplayName("Cambio de contraseña debe fallar si las contraseñas nuevas no coinciden")
    void cambiarPasswordFallaSiConfirmacionNoCoincidie() {
        CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual("Admin2026!");
        request.setPasswordNueva("NuevaClave123!");
        request.setPasswordConfirmacion("ClaveDistinta!");

        assertThatThrownBy(() -> authService.cambiarPassword(request, userDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("no coinciden");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambio de contraseña debe fallar si la contraseña actual es incorrecta")
    void cambiarPasswordFallaSiContrasenaActualIncorrecta() {
        CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual("ClaveActualEquivocada");
        request.setPasswordNueva("NuevaClave123!");
        request.setPasswordConfirmacion("NuevaClave123!");
        when(passwordEncoder.matches("ClaveActualEquivocada", usuarioEjemplo.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.cambiarPassword(request, userDetails))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("contraseña actual");

        verify(usuarioRepository, never()).save(any());
    }

    // =================================================================
    // TESTS DE solicitarCambioCorreo() / confirmarCambioCorreo() — 2FA cambio de correo
    // =================================================================

    @Test
    @DisplayName("solicitarCambioCorreo debe enviar un código de verificación al correo actual")
    void solicitarCambioCorreoEnviaCodigo() {
        authService.solicitarCambioCorreo(userDetails);

        verify(emailService).enviarCodigoVerificacionCorreo(eq("dti@test.com"), eq("Admin DTI Test"), any());
    }

    @Test
    @DisplayName("confirmarCambioCorreo con código correcto debe actualizar el correo del usuario")
    void confirmarCambioCorreoExitoso() {
        String codigo = solicitarCambioCorreoYCapturarCodigo();
        when(usuarioRepository.existsByCorreo("nuevo@test.com")).thenReturn(false);
        when(usuarioRepository.save(any())).thenReturn(usuarioEjemplo);

        authService.confirmarCambioCorreo(new ConfirmarCambioCorreoRequest(codigo, "nuevo@test.com"), userDetails);

        assertThat(usuarioEjemplo.getCorreo()).isEqualTo("nuevo@test.com");
        verify(usuarioRepository).save(usuarioEjemplo);
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    @Test
    @DisplayName("confirmarCambioCorreo con correo ya en uso debe lanzar excepción")
    void confirmarCambioCorreoConCorreoDuplicadoLanzaExcepcion() {
        String codigo = solicitarCambioCorreoYCapturarCodigo();
        when(usuarioRepository.existsByCorreo("duplicado@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.confirmarCambioCorreo(
                new ConfirmarCambioCorreoRequest(codigo, "duplicado@test.com"), userDetails))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ya está en uso");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmarCambioCorreo con código incorrecto debe lanzar AccesoNoAutorizadoException")
    void confirmarCambioCorreoConCodigoIncorrectoLanzaExcepcion() {
        solicitarCambioCorreoYCapturarCodigo();

        assertThatThrownBy(() -> authService.confirmarCambioCorreo(
                new ConfirmarCambioCorreoRequest("000000", "nuevo@test.com"), userDetails))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmarCambioCorreo sin solicitud previa debe lanzar excepción")
    void confirmarCambioCorreoSinSolicitudPreviaLanzaExcepcion() {
        assertThatThrownBy(() -> authService.confirmarCambioCorreo(
                new ConfirmarCambioCorreoRequest("123456", "nuevo@test.com"), userDetails))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    private String solicitarCambioCorreoYCapturarCodigo() {
        authService.solicitarCambioCorreo(userDetails);
        ArgumentCaptor<String> codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoVerificacionCorreo(any(), any(), codigoCaptor.capture());
        clearInvocations(emailService);
        return codigoCaptor.getValue();
    }
}
