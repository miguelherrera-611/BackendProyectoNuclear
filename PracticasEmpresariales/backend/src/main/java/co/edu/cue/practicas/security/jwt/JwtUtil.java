package co.edu.cue.practicas.security.jwt;

import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * PATRON SINGLETON — GPE-137
 *
 * Utilidad JWT centralizada como instancia única.
 * Maneja la generación y validación de tokens de autenticación.
 *
 * El token JWT contiene los datos del usuario en su payload (claims),
 * firmado con HMAC-SHA para que no pueda ser alterado.
 * No se almacena en base de datos: la validez la determina la firma y la expiración.
 *
 * Estructura del token generado:
 *   Header  → algoritmo de firma (HS256 o superior según longitud del secreto)
 *   Payload → userId, rol, etiquetaCargo, nombre, facultadId, programaId, expiración
 *   Firma   → HMAC del header+payload con la clave secreta
 *
 * El secreto y el tiempo de expiración se leen desde application.properties:
 *   app.jwt.secret        → clave para firmar (mínimo 64 caracteres en producción)
 *   app.jwt.expiration-ms → duración del token en milisegundos (por defecto 24h)
 */
@Slf4j
@Component
public class JwtUtil {

    // Clave secreta para firmar los tokens. Debe tener mínimo 64 caracteres en producción.
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Tiempo de vida del token en milisegundos (86400000 = 24 horas)
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Construye la clave criptográfica HMAC a partir del secreto en texto plano.
     * Se recalcula en cada uso para no almacenarla en memoria más de lo necesario.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT firmado para el usuario autenticado.
     *
     * Los claims incluidos en el token permiten al backend reconstruir
     * el contexto del usuario sin consultar la base de datos en cada petición:
     *   - userId       → ID del usuario en la BD
     *   - rol          → define los permisos de acceso
     *   - etiquetaCargo → cargo específico dentro de Coordinación Académica
     *   - nombre       → para mostrarlo en la UI sin otra consulta
     *   - facultadId   → scope de datos para Coordinación Académica
     *   - programaId   → scope de datos para Coordinador de Prácticas
     *
     * @param userDetails  datos del usuario autenticado ya cargados en memoria
     * @return token JWT en formato compacto (header.payload.firma en Base64)
     */
    public String generarToken(CustomUserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())             // correo del usuario como sujeto
                .claim("userId", userDetails.getId())
                .claim("rol", userDetails.getRol().name())
                .claim("etiquetaCargo", userDetails.getEtiquetaCargo() != null
                        ? userDetails.getEtiquetaCargo().name() : null)
                .claim("nombre", userDetails.getNombre())
                .claim("facultadId", userDetails.getFacultadId())
                .claim("programaId", userDetails.getProgramaId())
                .issuedAt(new Date())                           // fecha de creación
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // fecha de expiración
                .signWith(getSigningKey())                      // firma con HMAC-SHA
                .compact();                                     // genera el string final
    }

    /**
     * Extrae el correo (subject) del token JWT.
     * El correo se usa para cargar el usuario desde la BD en cada petición.
     *
     * @param token  token JWT en formato compacto
     * @return correo del usuario almacenado como subject en el token
     */
    public String extraerCorreo(String token) {
        return parsearClaims(token).getSubject();
    }

    /**
     * Valida que el token sea auténtico (firma correcta) y no haya expirado.
     * Si el token fue alterado o la firma no coincide, JJWT lanza JwtException.
     *
     * @param token  token JWT a validar
     * @return true si el token es válido y vigente, false en cualquier otro caso
     */
    public boolean validarToken(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Token inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el rol del usuario desde el payload del token.
     * Se usa en el JwtAuthenticationFilter para cargar los permisos sin consultar la BD.
     *
     * @param token  token JWT en formato compacto
     * @return rol del usuario como enum Rol
     */
    public Rol extraerRol(String token) {
        String rolStr = (String) parsearClaims(token).get("rol");
        return Rol.valueOf(rolStr);
    }

    /**
     * Extrae la etiqueta de cargo desde el payload del token.
     * Puede ser null si el usuario no tiene etiqueta (solo aplica a Coordinación Académica).
     *
     * @param token  token JWT en formato compacto
     * @return etiqueta de cargo como enum, o null si no aplica
     */
    public EtiquetaCargo extraerEtiquetaCargo(String token) {
        String ec = (String) parsearClaims(token).get("etiquetaCargo");
        return ec != null ? EtiquetaCargo.valueOf(ec) : null;
    }

    /**
     * Parsea el token JWT y retorna el objeto Claims con todos los datos del payload.
     * Verifica la firma automáticamente; lanza JwtException si algo falla.
     *
     * @param token  token JWT en formato compacto
     * @return claims (payload) del token ya verificado
     */
    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // verifica que la firma sea correcta
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
