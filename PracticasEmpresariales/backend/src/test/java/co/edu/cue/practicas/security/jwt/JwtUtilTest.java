package co.edu.cue.practicas.security.jwt;

import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias de JwtUtil.
 *
 * JwtUtil usa @Value para leer su secreto desde application.properties.
 * En pruebas unitarias (sin Spring) usamos ReflectionTestUtils.setField()
 * para inyectar los valores directamente en los campos privados.
 *
 * El secreto de prueba tiene exactamente 64 caracteres (512 bits)
 * para que JJWT pueda usar HMAC-SHA512.
 *
 * Cómo ejecutar en IntelliJ:
 *   Clic derecho sobre la clase → Run 'JwtUtilTest'
 */
@DisplayName("JwtUtil — Pruebas de generación y validación de tokens JWT")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private CustomUserDetails userDetails;

    // 64 caracteres exactos para satisfacer el requisito mínimo de HMAC-SHA512
    private static final String SECRET_TEST =
            "clave-secreta-para-testing-de-jwt-en-practicas-empresariales-cue";

    private static final long EXPIRATION_MS = 86_400_000L; // 24 horas

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inyectamos los valores de @Value directamente sin necesitar el contexto de Spring
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET_TEST);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", EXPIRATION_MS);

        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Admin DTI")
                .correo("dti@test.com")
                .passwordHash("hash")
                .rol(Rol.ADMIN_DTI)
                .activo(true)
                .build();
        userDetails = new CustomUserDetails(usuario);
    }

    // =================================================================
    // TESTS DE GENERACIÓN DE TOKEN
    // =================================================================

    @Test
    @DisplayName("generarToken debe retornar un JWT en formato header.payload.firma")
    void generarTokenRetornaJwtValido() {
        String token = jwtUtil.generarToken(userDetails);

        assertThat(token).isNotBlank();
        // Un JWT válido siempre tiene exactamente 3 partes separadas por punto
        assertThat(token.split("\\.")).hasSize(3);
    }

    // =================================================================
    // TESTS DE EXTRACCIÓN DE CLAIMS
    // =================================================================

    @Test
    @DisplayName("extraerCorreo debe retornar el correo del usuario desde el token")
    void extraerCorreoDesdeToken() {
        String token = jwtUtil.generarToken(userDetails);

        String correo = jwtUtil.extraerCorreo(token);

        assertThat(correo).isEqualTo("dti@test.com");
    }

    @Test
    @DisplayName("extraerRol debe retornar el rol correcto desde el payload del token")
    void extraerRolDesdeToken() {
        String token = jwtUtil.generarToken(userDetails);

        Rol rol = jwtUtil.extraerRol(token);

        assertThat(rol).isEqualTo(Rol.ADMIN_DTI);
    }

    // =================================================================
    // TESTS DE VALIDACIÓN
    // =================================================================

    @Test
    @DisplayName("validarToken debe retornar true para un token recién generado")
    void validarTokenValidoRetornaTrue() {
        String token = jwtUtil.generarToken(userDetails);

        assertThat(jwtUtil.validarToken(token)).isTrue();
    }

    @Test
    @DisplayName("validarToken debe retornar false si la firma del token fue alterada")
    void validarTokenFalsificadoRetornaFalse() {
        String tokenReal = jwtUtil.generarToken(userDetails);
        // Reemplazamos los últimos 4 caracteres de la firma para invalidarla
        String tokenFalsificado = tokenReal.substring(0, tokenReal.length() - 4) + "XXXX";

        assertThat(jwtUtil.validarToken(tokenFalsificado)).isFalse();
    }

    @Test
    @DisplayName("validarToken debe retornar false para un token ya expirado")
    void validarTokenExpiradoRetornaFalse() {
        // Sobreescribimos la expiración a -1 segundo (ya nació expirado)
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1000L);
        String tokenExpirado = jwtUtil.generarToken(userDetails);

        assertThat(jwtUtil.validarToken(tokenExpirado)).isFalse();
    }

    @Test
    @DisplayName("validarToken debe retornar false para cadenas que no son JWT")
    void validarStringNoJwtRetornaFalse() {
        assertThat(jwtUtil.validarToken("esto.no.es.un.jwt")).isFalse();
        assertThat(jwtUtil.validarToken("")).isFalse();
        assertThat(jwtUtil.validarToken("solo_texto_sin_puntos")).isFalse();
    }
}
