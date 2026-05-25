package co.edu.cue.practicas.service.auth;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CambiarPasswordRequest;
import co.edu.cue.practicas.dto.request.LoginRequest;
import co.edu.cue.practicas.dto.response.LoginResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * PATRON SINGLETON — GPE-136, GPE-137
 *
 * Servicio de autenticación y gestión de sesiones — instancia única.
 * Centraliza la creación y validación de credenciales evitando
 * múltiples instancias que podrían generar inconsistencias.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaLogger auditoriaLogger;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipOrigen) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Usuario usuario = userDetails.getUsuario();

            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            String token = jwtUtil.generarToken(userDetails);

            auditoriaLogger.registrar(BitacoraAuditoria.builder()
                    .usuario(usuario)
                    .nombreUsuario(usuario.getNombre())
                    .rolUsuario(usuario.getRol())
                    .etiquetaCargoUsuario(usuario.getEtiquetaCargo())
                    .modulo("AUTH")
                    .tipoAccion(TipoAccion.LOGIN_EXITOSO)
                    .ipOrigen(ipOrigen)
                    .exitoso(true));

            return LoginResponse.builder()
                    .token(token)
                    .tipo("Bearer")
                    .usuarioId(usuario.getId())
                    .nombre(usuario.getNombre())
                    .correo(usuario.getCorreo())
                    .rol(usuario.getRol())
                    .etiquetaCargo(usuario.getEtiquetaCargo())
                    .primerIngreso(usuario.isPrimerIngreso())
                    .facultadId(usuario.getFacultad() != null ? usuario.getFacultad().getId() : null)
                    .programaId(usuario.getPrograma() != null ? usuario.getPrograma().getId() : null)
                    .build();

        } catch (BadCredentialsException | DisabledException e) {
            registrarLoginFallido(request.getCorreo(), ipOrigen);
            throw new AccesoNoAutorizadoException("Credenciales incorrectas o cuenta inactiva.");
        }
    }

    @Transactional
    public void cambiarPassword(CambiarPasswordRequest request, CustomUserDetails userDetails) {
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new OperacionNoPermitidaException("Las contraseñas nuevas no coinciden.");
        }

        Usuario usuario = userDetails.getUsuario();

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new AccesoNoAutorizadoException("La contraseña actual es incorrecta.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setPrimerIngreso(false);
        usuarioRepository.save(usuario);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(usuario)
                .nombreUsuario(usuario.getNombre())
                .rolUsuario(usuario.getRol())
                .etiquetaCargoUsuario(usuario.getEtiquetaCargo())
                .modulo("AUTH")
                .tipoAccion(TipoAccion.CAMBIO_PASSWORD)
                .registroAfectadoId(usuario.getId())
                .registroAfectadoTipo("Usuario")
                .exitoso(true));
    }

    private void registrarLoginFallido(String correo, String ipOrigen) {
        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .nombreUsuario(correo)
                .modulo("AUTH")
                .tipoAccion(TipoAccion.LOGIN_FALLIDO)
                .ipOrigen(ipOrigen)
                .exitoso(false)
                .motivoFallo("Credenciales incorrectas o cuenta inactiva"));
    }
}
