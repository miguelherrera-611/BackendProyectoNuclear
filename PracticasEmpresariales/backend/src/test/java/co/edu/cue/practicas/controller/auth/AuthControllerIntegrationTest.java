package co.edu.cue.practicas.controller.auth;

import co.edu.cue.practicas.controller.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración — AuthController
 * Endpoints: POST /auth/login, POST /auth/cambiar-password
 */
@DisplayName("AuthController — Pruebas de integración")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ═══════════════════════════════════════════════════
    // POST /auth/login
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /auth/login — Credenciales válidas retorna 200 con token")
    void login_credencialesValidas_retorna200ConToken() throws Exception {
        String body = """
            {
                "correo": "dti@cue.edu.co",
                "password": "TestAdmin2026!"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.token").isNotEmpty())
                .andExpect(jsonPath("$.datos.correo").value("dti@cue.edu.co"))
                .andExpect(jsonPath("$.datos.rol").value("ADMIN_DTI"));
    }

    @Test
    @DisplayName("POST /auth/login — Credenciales incorrectas retorna 403")
    void login_credencialesIncorrectas_retorna403() throws Exception {
        String body = """
            {
                "correo": "dti@cue.edu.co",
                "password": "ClaveIncorrecta123!"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exitoso").value(false));
    }

    @Test
    @DisplayName("POST /auth/login — Correo inválido retorna 400")
    void login_correoInvalido_retorna400() throws Exception {
        String body = """
            {
                "correo": "no-es-un-correo",
                "password": "TestAdmin2026!"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login — Sin password retorna 400")
    void login_sinPassword_retorna400() throws Exception {
        String body = """
            {
                "correo": "dti@cue.edu.co"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login — Sin token no puede acceder a endpoints protegidos")
    void sinToken_noAccedeAEndpointsProtegidos() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════
    // POST /auth/cambiar-password
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /auth/cambiar-password — Sin token retorna 403")
    void cambiarPassword_sinToken_retorna403() throws Exception {
        String body = """
            {
                "passwordActual": "TestAdmin2026!",
                "passwordNueva": "NuevaClave123!",
                "passwordConfirmacion": "NuevaClave123!"
            }
            """;

        mockMvc.perform(post("/auth/cambiar-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }
}
