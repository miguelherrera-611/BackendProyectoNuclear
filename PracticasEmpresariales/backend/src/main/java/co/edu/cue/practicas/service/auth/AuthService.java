package co.edu.cue.practicas.service.auth;

import co.edu.cue.practicas.audit.ModuloAuditoria;
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
 *
 * Dos responsabilidades principales:
 *   1. login()           → verifica credenciales y devuelve token JWT
 *   2. cambiarPassword() → permite al usuario actualizar su propia contraseña
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // Delegamos la verificación de credenciales al mecanismo de Spring Security
    private final AuthenticationManager authenticationManager;

    // Genera y valida los tokens JWT que viajan en cada petición HTTP
    private final JwtUtil jwtUtil;

    // Acceso a la base de datos de usuarios para actualizar último acceso y guardar cambios
    private final UsuarioRepository usuarioRepository;

    // Encripta contraseñas con BCrypt y compara texto plano contra el hash almacenado
    private final PasswordEncoder passwordEncoder;

    // Registra cada acción relevante en la bitácora de auditoría (quién hizo qué y cuándo)
    private final AuditoriaLogger auditoriaLogger;

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
            // Spring Security delega la verificación a CustomUserDetailsService,
            // que carga el usuario desde la BD y compara el hash de la contraseña
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword())
            );

            // Si llegamos aquí, las credenciales son correctas — obtenemos el usuario autenticado
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Usuario usuario = userDetails.getUsuario();

            // Marcamos la fecha y hora exacta de este acceso para trazabilidad
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Generamos el JWT con los claims del usuario (id, rol, etiqueta, facultad, programa)
            // Este token viaja en el header Authorization de cada petición posterior
            String token = jwtUtil.generarToken(userDetails);

            // Dejamos constancia del login exitoso en la bitácora de auditoría
            auditoriaLogger.registrar(BitacoraAuditoria.builder()
                    .usuario(usuario)
                    .nombreUsuario(usuario.getNombre())
                    .rolUsuario(usuario.getRol())
                    .etiquetaCargoUsuario(usuario.getEtiquetaCargo())
                    .modulo(ModuloAuditoria.AUTH)
                    .tipoAccion(TipoAccion.LOGIN_EXITOSO)
                    .ipOrigen(ipOrigen)
                    .exitoso(true));

            // Construimos la respuesta con el token y los datos del perfil.
            // El frontend los usa para decidir qué dashboard mostrar y
            // para saber si debe forzar el cambio de contraseña (primerIngreso=true).
            return LoginResponse.builder()
                    .token(token)
                    .tipo("Bearer")
                    .usuarioId(usuario.getId())
                    .nombre(usuario.getNombre())
                    .correo(usuario.getCorreo())
                    .rol(usuario.getRol())
                    .etiquetaCargo(usuario.getEtiquetaCargo())
                    .primerIngreso(usuario.isPrimerIngreso())  // true → frontend redirige al cambio de contraseña
                    .facultadId(usuario.getFacultad() != null ? usuario.getFacultad().getId() : null)
                    .programaId(usuario.getPrograma() != null ? usuario.getPrograma().getId() : null)
                    .build();

        } catch (BadCredentialsException | DisabledException e) {
            // Credenciales incorrectas o usuario desactivado → registramos el intento fallido
            // y lanzamos 403 sin revelar qué fue exactamente lo incorrecto
            registrarLoginFallido(request.getCorreo(), ipOrigen);
            throw new AccesoNoAutorizadoException("Credenciales incorrectas o cuenta inactiva.");
        }
    }

    /**
     * Permite al usuario autenticado cambiar su propia contraseña.
     *
     * Validaciones antes de guardar:
     *   - La nueva contraseña y su confirmación deben ser iguales.
     *   - La contraseña actual debe coincidir con el hash en la BD.
     *
     * Al completarse, se marca primerIngreso=false para que el sistema
     * no vuelva a exigir el cambio en el siguiente login.
     *
     * @param request     contiene passwordActual, passwordNueva y passwordConfirmacion
     * @param userDetails usuario autenticado extraído del token JWT por Spring Security
     */
    @Transactional
    public void cambiarPassword(CambiarPasswordRequest request, CustomUserDetails userDetails) {

        // Verificamos que ambas versiones de la nueva contraseña coincidan
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new OperacionNoPermitidaException("Las contraseñas nuevas no coinciden.");
        }

        Usuario usuario = userDetails.getUsuario();

        // Comparamos el texto plano recibido contra el hash BCrypt almacenado en BD
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new AccesoNoAutorizadoException("La contraseña actual es incorrecta.");
        }

        // Guardamos el nuevo hash; nunca almacenamos contraseñas en texto plano
        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));

        // Marcamos que ya no es el primer ingreso, para que no se fuerce el cambio nuevamente
        usuario.setPrimerIngreso(false);
        usuarioRepository.save(usuario);

        // Registramos el cambio de contraseña en la bitácora para trazabilidad
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
     * Registra en la bitácora un intento de login fallido.
     * Se usa el correo ingresado como nombre de usuario porque el usuario
     * puede no existir en el sistema (no hay objeto Usuario disponible).
     * Este método solo registra; la excepción la lanza quien lo llama.
     */
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
