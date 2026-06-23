package co.edu.cue.practicas.controller.empresa;

import co.edu.cue.practicas.controller.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración — VacanteController (GPE-152 / GPE-153)
 * Flujo completo: crear empresa → activar → crear vacante → activar/desactivar (aprobar/rechazar) vacante
 */
@DisplayName("VacanteController — Pruebas de integración")
class VacanteControllerIntegrationTest extends BaseIntegrationTest {

    private String tokenCoordinador;
    private Long empresaAprobadaId;

    @BeforeEach
    void setUp() throws Exception {
        tokenCoordinador = obtenerToken("coord.test@cue.edu.co", "CoordTest2026!");

        // Crear empresa y activarla para tener empresaAprobadaId disponible en todos los tests
        String empresaResp = mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "razonSocial": "Empresa Test Vacantes",
                            "nit": "800.999.111-0",
                            "nombreContacto": "Contacto Test",
                            "correo": "vacantes@empresa.com"
                        }
                        """))
                .andReturn().getResponse().getContentAsString();

        empresaAprobadaId = objectMapper.readTree(empresaResp)
                .path("datos").path("id").asLong();

        // Activar la empresa para que las vacantes puedan crearse
        mockMvc.perform(patch("/api/v1/empresas/" + empresaAprobadaId + "/activar")
                .header("Authorization", bearer(tokenCoordinador)));
    }

    @Test
    @DisplayName("POST /vacantes — Crear vacante para empresa activa retorna 201")
    void crearVacante_empresaAprobada_retorna201() throws Exception {
        mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "Desarrollo de software",
                                "cuposTotales": 2
                            }
                            """.formatted(empresaAprobadaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.datos.cuposTotales").value(2));
    }

    @Test
    @DisplayName("POST /vacantes — Cupos 0 retorna 400")
    void crearVacante_cuposCero_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "QA",
                                "cuposTotales": 0
                            }
                            """.formatted(empresaAprobadaId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /vacantes/{id} — Edición válida retorna 200 con los datos actualizados")
    void editarVacante_valida_retorna200() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "Soporte",
                                "cuposTotales": 2
                            }
                            """.formatted(empresaAprobadaId)))
                .andReturn().getResponse().getContentAsString();

        Long vacanteId = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(put("/api/v1/vacantes/" + vacanteId)
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "area": "Soporte Avanzado",
                                "cuposTotales": 4
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.area").value("Soporte Avanzado"))
                .andExpect(jsonPath("$.datos.cuposTotales").value(4));
    }

    @Test
    @DisplayName("PUT /vacantes/{id} — Cupos totales en 0 retorna 400 (validación @Min)")
    void editarVacante_cuposCero_retorna400() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "Datos",
                                "cuposTotales": 1
                            }
                            """.formatted(empresaAprobadaId)))
                .andReturn().getResponse().getContentAsString();

        Long vacanteId = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(put("/api/v1/vacantes/" + vacanteId)
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "area": "Datos",
                                "cuposTotales": 0
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /vacantes/{id} — Vacante inexistente retorna 404")
    void editarVacante_noExiste_retorna404() throws Exception {
        mockMvc.perform(put("/api/v1/vacantes/999999")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "area": "No existe",
                                "cuposTotales": 1
                            }
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Flujo: crear vacante → aprobar → estado DISPONIBLE")
    void flujoAprobarVacante_retornaDisponible() throws Exception {
        // Crear vacante
        String resp = mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "Backend",
                                "cuposTotales": 3
                            }
                            """.formatted(empresaAprobadaId)))
                .andReturn().getResponse().getContentAsString();

        Long vacanteId = objectMapper.readTree(resp).path("datos").path("id").asLong();

        // Aprobar (PATRÓN STATE: PENDIENTE → DISPONIBLE)
        mockMvc.perform(patch("/api/v1/vacantes/" + vacanteId + "/aprobar")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("DISPONIBLE"));
    }

    @Test
    @DisplayName("Flujo: crear vacante → rechazar (legacy, delega en desactivar) → estado CERRADA")
    void flujoRechazarVacante_retornaCerrada() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "Frontend",
                                "cuposTotales": 1
                            }
                            """.formatted(empresaAprobadaId)))
                .andReturn().getResponse().getContentAsString();

        Long vacanteId = objectMapper.readTree(resp).path("datos").path("id").asLong();

        // rechazar() es un alias legacy que delega en desactivarVacante() (PENDIENTE/DISPONIBLE → CERRADA)
        mockMvc.perform(patch("/api/v1/vacantes/" + vacanteId + "/rechazar")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"No aplica al programa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("CERRADA"));
    }

    @Test
    @DisplayName("Aprobar vacante ya DISPONIBLE retorna 409 (State inválido)")
    void aprobarVacante_yaDisponible_retorna409() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "DevOps",
                                "cuposTotales": 2
                            }
                            """.formatted(empresaAprobadaId)))
                .andReturn().getResponse().getContentAsString();

        Long vacanteId = objectMapper.readTree(resp).path("datos").path("id").asLong();

        // Primera aprobación — OK
        mockMvc.perform(patch("/api/v1/vacantes/" + vacanteId + "/aprobar")
                .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isOk());

        // Segunda aprobación — CONFLICT (State inválido)
        mockMvc.perform(patch("/api/v1/vacantes/" + vacanteId + "/aprobar")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /vacantes/disponibles — Retorna solo vacantes DISPONIBLES")
    void listarDisponibles_retornaSoloDisponibles() throws Exception {
        // Crear y aprobar vacante
        String resp = mockMvc.perform(post("/api/v1/vacantes")
                        .header("Authorization", bearer(tokenCoordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "empresaId": %d,
                                "area": "Mobile",
                                "cuposTotales": 1
                            }
                            """.formatted(empresaAprobadaId)))
                .andReturn().getResponse().getContentAsString();

        Long vacanteId = objectMapper.readTree(resp).path("datos").path("id").asLong();

        mockMvc.perform(patch("/api/v1/vacantes/" + vacanteId + "/aprobar")
                .header("Authorization", bearer(tokenCoordinador)));

        mockMvc.perform(get("/api/v1/vacantes/disponibles")
                        .header("Authorization", bearer(tokenCoordinador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].estado").value("DISPONIBLE"));
    }
}
