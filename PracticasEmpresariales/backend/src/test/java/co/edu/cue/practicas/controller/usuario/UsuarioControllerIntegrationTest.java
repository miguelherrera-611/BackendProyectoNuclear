package co.edu.cue.practicas.controller.usuario;

import co.edu.cue.practicas.controller.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración — UsuarioController
 * Endpoints: POST /usuarios, GET /usuarios, GET /usuarios/{id},
 *            PUT /usuarios/{id}, PATCH /usuarios/{id}/desactivar
 */
@DisplayName("UsuarioController — Pruebas de integración")
class UsuarioControllerIntegrationTest extends BaseIntegrationTest {

    private String tokenDTI;

    @BeforeEach
    void setUp() throws Exception {
        tokenDTI = obtenerTokenDTI();
    }

    // ═══════════════════════════════════════════════════
    // POST /usuarios — Crear usuario
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("POST /usuarios — DTI crea coordinador académico retorna 201")
    void crearUsuario_coordinadorAcademico_retorna201() throws Exception {
        String body = """
    {
        "nombre": "Coordinador Practicas Test",
        "correo": "coord.academica@cue.edu.co",
        "rol": "COORDINACION_ACADEMICA",
        "etiquetaCargo": "COORDINACION_ACADEMICA"
    }
    """;

        mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(tokenDTI))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.correo").value("coord.academica@cue.edu.co"))
                .andExpect(jsonPath("$.datos.rol").value("COORDINACION_ACADEMICA"));
    }

    @Test
    @DisplayName("POST /usuarios — Correo duplicado retorna 409")
    void crearUsuario_correoDuplicado_retorna409() throws Exception {
        String body = """
            {
                "nombre": "Usuario Test",
                "correo": "dti@cue.edu.co",
                "rol": "COORDINADOR_PRACTICAS"
            }
            """;

        mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(tokenDTI))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /usuarios — Sin token retorna 403")
    void crearUsuario_sinToken_retorna403() throws Exception {
        String body = """
            {
                "nombre": "Test",
                "correo": "test@cue.edu.co",
                "rol": "ESTUDIANTE"
            }
            """;

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /usuarios — Sin nombre retorna 400")
    void crearUsuario_sinNombre_retorna400() throws Exception {
        String body = """
            {
                "correo": "test@cue.edu.co",
                "rol": "ESTUDIANTE"
            }
            """;

        mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(tokenDTI))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════
    // GET /usuarios
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("GET /usuarios — DTI puede listar todos los usuarios")
    void listarUsuarios_DTI_retorna200() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header("Authorization", bearer(tokenDTI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.content").isArray());
    }

    @Test
    @DisplayName("GET /usuarios/{id} — Obtener usuario existente retorna 200")
    void obtenerUsuario_existente_retorna200() throws Exception {
        // Crear usuario primero
        String body = """
            {
                "nombre": "Usuario Consulta",
                "correo": "consulta@cue.edu.co",
                "rol": "DOCENTE_ASESOR"
            }
            """;

        String resp = mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(tokenDTI))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(get("/usuarios/" + id)
                        .header("Authorization", bearer(tokenDTI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.correo").value("consulta@cue.edu.co"));
    }

    @Test
    @DisplayName("GET /usuarios/{id} — Usuario inexistente retorna 404")
    void obtenerUsuario_noExiste_retorna404() throws Exception {
        mockMvc.perform(get("/usuarios/9999")
                        .header("Authorization", bearer(tokenDTI)))
                .andExpect(status().isNotFound());
    }

    // ═══════════════════════════════════════════════════
    // PATCH /usuarios/{id}/desactivar
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /usuarios/{id}/desactivar — Desactivar usuario retorna 200")
    void desactivarUsuario_retorna200() throws Exception {
        String body = """
            {
                "nombre": "Usuario a Desactivar",
                "correo": "desactivar@cue.edu.co",
                "rol": "DOCENTE_ASESOR"
            }
            """;

        String resp = mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(tokenDTI))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(patch("/usuarios/" + id + "/desactivar")
                        .header("Authorization", bearer(tokenDTI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));
    }
}
