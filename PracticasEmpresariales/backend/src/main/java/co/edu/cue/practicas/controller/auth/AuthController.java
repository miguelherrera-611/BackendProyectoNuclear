package co.edu.cue.practicas.controller.auth;

import co.edu.cue.practicas.dto.request.CambiarPasswordRequest;
import co.edu.cue.practicas.dto.request.ConfirmarCambioCorreoRequest;
import co.edu.cue.practicas.dto.request.LoginRequest;
import co.edu.cue.practicas.dto.request.VerificarCodigoLoginRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.LoginPendienteResponse;
import co.edu.cue.practicas.dto.response.LoginResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginPendienteResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        LoginPendienteResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Código de verificación enviado", response));
    }

    @PostMapping("/login/verificar")
    public ResponseEntity<ApiResponse<LoginResponse>> verificarCodigoLogin(
            @Valid @RequestBody VerificarCodigoLoginRequest request,
            HttpServletRequest httpRequest) {

        LoginResponse response = authService.verificarCodigoLogin(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.cambiarPassword(request, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente", null));
    }

    @PostMapping("/correo/solicitar-cambio")
    public ResponseEntity<ApiResponse<Void>> solicitarCambioCorreo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.solicitarCambioCorreo(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Código de verificación enviado a tu correo actual", null));
    }

    @PostMapping("/correo/confirmar-cambio")
    public ResponseEntity<ApiResponse<Void>> confirmarCambioCorreo(
            @Valid @RequestBody ConfirmarCambioCorreoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.confirmarCambioCorreo(request, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Correo electrónico actualizado correctamente", null));
    }
}
