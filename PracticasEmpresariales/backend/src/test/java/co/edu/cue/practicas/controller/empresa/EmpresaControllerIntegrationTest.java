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
 * Flujo completo con JWT real: login → crear empresa → aprobar → inactivar
 */
@DisplayName("EmpresaController — Pruebas de integración")
class EmpresaControllerIntegrationTest extends BaseIntegrationTest {

    private String tokenCoordinador;

    @BeforeEach
    void setUp() throws Exception {
        String resp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "correo": "coord.test@cue.edu.co",
                            "password": "CoordTest2026!"
                        }
                        """))
                .andReturn().getResponse().getContentAsString();

        tokenCoordinador = objectMapper.readTree(resp)
                .path("datos").path("token").asText();
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
                .andExpect(jsonPath("$.datos.estado").value("PENDIENTE"));
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
    @DisplayName("POST /empresas — Sin token retorna 403")
    void crearEmpresa_sinToken_retorna403() throws Exception {
        mockMvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════
    // Flujo completo: crear → aprobar → inactivar
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("Flujo completo: crear empresa → aprobar → retorna APROBADA")
    void flujoCompleto_crearYAprobar() throws Exception {
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

        // Aprobar
        mockMvc.perform(patch("/api/v1/empresas/" + id + "/aprobar")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("APROBADA"));
    }

    @Test
    @DisplayName("Flujo: rechazar empresa con motivo → retorna RECHAZADA")
    void flujoRechazar_conMotivo_retornaRechazada() throws Exception {
        String body = """
            {
                "razonSocial": "Empresa Rechazar",
                "nit": "555.666.777-8",
                "nombreContacto": "Contacto",
                "correo": "rechazar@empresa.com"
            }
            """;

        String resp = mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(patch("/api/v1/empresas/" + id + "/rechazar")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"No cumple requisitos legales\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("RECHAZADA"));
    }
}
