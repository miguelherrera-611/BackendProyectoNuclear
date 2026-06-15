package co.edu.cue.practicas.service.auth;

import co.edu.cue.practicas.audit.ModuloAuditoria;
import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CambiarPasswordRequest;
import co.edu.cue.practicas.dto.request.ConfirmarCambioCorreoRequest;
import co.edu.cue.practicas.dto.request.LoginRequest;
import co.edu.cue.practicas.dto.response.LoginResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoCuenta;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.jwt.JwtUtil;
import co.edu.cue.practicas.service.notificacion.EmailService;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PATRON SINGLETON — GPE-136, GPE-137
 *
 * Servicio de autenticación y gestión de sesiones — instancia única.
 * Centraliza la creación y validación de credenciales evitando
 * múltiples instancias que podrían generar inconsistencias.
 *
 * Responsabilidades:
 *   1. login()              → verifica credenciales y devuelve token JWT
 *   2. cambiarPassword()    → permite al usuario actualizar su propia contraseña
 *   3. solicitarCambioCorreo() / confirmarCambioCorreo() → flujo 2FA para cambio de correo
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
    private final EmailService emailService;

    private record CodigoEntry(String codigo, LocalDateTime expiracion) {}
    private final Map<Long, CodigoEntry> codigosPendientes = new ConcurrentHashMap<>();

    private static final int CODIGO_DIGITOS = 6;
    private static final int EXPIRACION_MINUTOS = 10;

    /**
     * Autentica al usuario y retorna un token JWT junto con los datos del perfil.
     *
     * Flujo:
     *   1. Spring Security verifica correo y contraseña contra la BD.
     *   2. Se actualiza la fecha de último acceso del usuario.
     *   3. Se genera el token JWT con el rol, facultad y programa del usuario.
     *   4. Se registra el login exitoso en la bitácora.
     *   5. Se retorna el token y los datos necesarios para que el frontend
     *      sepa qué panel mostrar según el rol.
     *
     * Si las credenciales son incorrectas o el usuario está inactivo,
     * se registra el intento fallido y se lanza una excepción 403.
     *
     * @param request   objeto con correo y contraseña enviados desde el frontend
     * @param ipOrigen  IP del cliente, extraída en el controlador para guardarla en auditoría
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipOrigen) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Usuario usuario = userDetails.getUsuario();

            usuario.setUltimoAcceso(LocalDateTime.now());

            if (EstadoCuenta.PENDIENTE.equals(usuario.getEstadoCuenta())) {
                usuario.setEstadoCuenta(EstadoCuenta.ACTIVO);
            }

            usuarioRepository.save(usuario);

            String token = jwtUtil.generarToken(userDetails);

            auditoriaLogger.registrar(BitacoraAuditoria.builder()
                    .usuario(usuario)
                    .nombreUsuario(usuario.getNombre())
                    .rolUsuario(usuario.getRol())
                    .etiquetaCargoUsuario(usuario.getEtiquetaCargo())
                    .modulo(ModuloAuditoria.AUTH)
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

    /**
     * Permite al usuario autenticado cambiar su propia contraseña.
     *
     * @param request     contiene passwordActual, passwordNueva y passwordConfirmacion
     * @param userDetails usuario autenticado extraído del token JWT por Spring Security
     */
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
                .modulo(ModuloAuditoria.AUTH)
                .tipoAccion(TipoAccion.CAMBIO_PASSWORD)
                .registroAfectadoId(usuario.getId())
                .registroAfectadoTipo("Usuario")
                .exitoso(true));
    }

    /**
     * Genera un código de 6 dígitos, lo almacena en memoria con expiración de 10 minutos
     * y lo envía al correo actual del usuario. Solo se permite un código activo por usuario.
     *
     * @param userDetails usuario autenticado
     */
    public void solicitarCambioCorreo(CustomUserDetails userDetails) {
        Usuario usuario = userDetails.getUsuario();
        String codigo = generarCodigo();
        codigosPendientes.put(usuario.getId(),
                new CodigoEntry(codigo, LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS)));
        emailService.enviarCodigoVerificacionCorreo(usuario.getCorreo(), usuario.getNombre(), codigo);
        log.info("[AUTH] Código de cambio de correo enviado al usuario {}", usuario.getId());
    }

    /**
     * Valida el código recibido y, si es correcto y no ha expirado, actualiza el correo del usuario.
     *
     * @param request     contiene el código y el nuevo correo
     * @param userDetails usuario autenticado
     */
    @Transactional
    public void confirmarCambioCorreo(ConfirmarCambioCorreoRequest request, CustomUserDetails userDetails) {
        Usuario usuario = userDetails.getUsuario();
        Long userId = usuario.getId();

        CodigoEntry entry = codigosPendientes.get(userId);
        if (entry == null) {
            throw new OperacionNoPermitidaException("No hay un código de verificación pendiente. Solicita uno nuevo.");
        }
        if (LocalDateTime.now().isAfter(entry.expiracion())) {
            codigosPendientes.remove(userId);
            throw new OperacionNoPermitidaException("El código ha expirado. Solicita uno nuevo.");
        }
        if (!entry.codigo().equals(request.codigo())) {
            throw new AccesoNoAutorizadoException("El código de verificación es incorrecto.");
        }
        if (usuarioRepository.existsByCorreo(request.nuevoCorreo())) {
            throw new OperacionNoPermitidaException("El correo " + request.nuevoCorreo() + " ya está en uso por otra cuenta.");
        }

        String correoAnterior = usuario.getCorreo();
        usuario.setCorreo(request.nuevoCorreo());
        usuarioRepository.save(usuario);
        codigosPendientes.remove(userId);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(usuario)
                .nombreUsuario(usuario.getNombre())
                .rolUsuario(usuario.getRol())
                .etiquetaCargoUsuario(usuario.getEtiquetaCargo())
                .modulo(ModuloAuditoria.AUTH)
                .tipoAccion(TipoAccion.CAMBIO_CORREO)
                .registroAfectadoId(userId)
                .registroAfectadoTipo("Usuario")
                .exitoso(true));

        log.info("[AUTH] Correo del usuario {} cambiado de {} a {}", userId, correoAnterior, request.nuevoCorreo());
    }

    private String generarCodigo() {
        SecureRandom rng = new SecureRandom();
        int num = rng.nextInt((int) Math.pow(10, CODIGO_DIGITOS));
        return String.format("%0" + CODIGO_DIGITOS + "d", num);
    }

    private void registrarLoginFallido(String correo, String ipOrigen) {
        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .nombreUsuario(correo)
                .modulo(ModuloAuditoria.AUTH)
                .tipoAccion(TipoAccion.LOGIN_FALLIDO)
                .ipOrigen(ipOrigen)
                .exitoso(false)
                .motivoFallo("Credenciales incorrectas o cuenta inactiva"));
    }
}
