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
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(CustomUserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("rol", userDetails.getRol().name())
                .claim("etiquetaCargo", userDetails.getEtiquetaCargo() != null
                        ? userDetails.getEtiquetaCargo().name() : null)
                .claim("nombre", userDetails.getNombre())
                .claim("facultadId", userDetails.getFacultadId())
                .claim("programaId", userDetails.getProgramaId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extraerCorreo(String token) {
        return parsearClaims(token).getSubject();
    }

    public boolean validarToken(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public Rol extraerRol(String token) {
        String rolStr = (String) parsearClaims(token).get("rol");
        return Rol.valueOf(rolStr);
    }

    public EtiquetaCargo extraerEtiquetaCargo(String token) {
        String ec = (String) parsearClaims(token).get("etiquetaCargo");
        return ec != null ? EtiquetaCargo.valueOf(ec) : null;
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
