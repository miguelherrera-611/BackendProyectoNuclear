package co.edu.cue.practicas.controller.empresa;

import co.edu.cue.practicas.controller.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración — EmpresaController (GPE-150)
 * Flujo completo con JWT real: login → crear empresa → activar → inactivar
 */
@DisplayName("EmpresaController — Pruebas de integración")
class EmpresaControllerIntegrationTest extends BaseIntegrationTest {

    private String tokenCoordinador;

    @BeforeEach
    void setUp() throws Exception {
        tokenCoordinador = obtenerToken("coord.test@cue.edu.co", "CoordTest2026!");
    }

    @Test
    @DisplayName("POST /empresas — Crear empresa válida retorna 201")
    void crearEmpresa_valida_retorna201() throws Exception {
        String body = """
            {
                "razonSocial": "TechCo S.A.",
                "nit": "900.123.456-7",
                "sector": "Tecnología",
                "direccion": "Calle 10 # 5-20",
                "municipio": "Armenia",
                "telefono": "3001234567",
                "nombreContacto": "Juan Pérez",
                "correo": "contacto@techco.com",
                "areasDisponibles": ["Desarrollo", "QA"]
            }
            """;

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.razonSocial").value("TechCo S.A."))
                .andExpect(jsonPath("$.datos.estado").value("INACTIVA"));
    }

    @Test
    @DisplayName("POST /empresas — NIT duplicado retorna 409")
    void crearEmpresa_nitDuplicado_retorna409() throws Exception {
        String body = """
            {
                "razonSocial": "TechCo S.A.",
                "nit": "900.123.456-7",
                "nombreContacto": "Juan",
                "correo": "contacto@techco.com"
            }
            """;

        mockMvc.perform(post("/api/v1/empresas")
                .header("Authorization", bearer(tokenCoordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /empresas — Sin token retorna 401")
    void crearEmpresa_sinToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════
    // Flujo completo: crear → activar → inactivar
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("Flujo completo: crear empresa → activar → retorna ACTIVA")
    void flujoCompleto_crearYActivar() throws Exception {
        String body = """
            {
                "razonSocial": "Empresa Flujo Test",
                "nit": "111.222.333-4",
                "nombreContacto": "Contacto",
                "correo": "flujo@empresa.com"
            }
            """;

        // Crear
        String resp = mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).path("datos").path("id").asLong();

        // Activar
        mockMvc.perform(patch("/api/v1/empresas/" + id + "/activar")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("ACTIVA"));
    }

    @Test
    @DisplayName("Flujo: activar → inactivar → retorna INACTIVA")
    void flujoActivarEInactivar_retornaInactiva() throws Exception {
        String body = """
            {
                "razonSocial": "Empresa Inactivar",
                "nit": "555.666.777-8",
                "nombreContacto": "Contacto",
                "correo": "inactivar@empresa.com"
            }
            """;

        String resp = mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(patch("/api/v1/empresas/" + id + "/activar")
                .header("Authorization", bearer(tokenCoordinador)));

        mockMvc.perform(patch("/api/v1/empresas/" + id + "/inactivar")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("INACTIVA"));
    }

    @Test
    @DisplayName("Activar empresa ya ACTIVA retorna 409 (State inválido)")
    void activarEmpresa_yaActiva_retorna409() throws Exception {
        String body = """
            {
                "razonSocial": "Empresa Doble Activacion",
                "nit": "222.333.444-5",
                "nombreContacto": "Contacto",
                "correo": "doble@empresa.com"
            }
            """;

        String resp = mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(patch("/api/v1/empresas/" + id + "/activar")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/empresas/" + id + "/activar")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isConflict());
    }
}
