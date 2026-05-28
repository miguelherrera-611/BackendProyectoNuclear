package co.edu.cue.practicas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Clase base para todas las pruebas de integración.
 *
 * @SpringBootTest: levanta el contexto completo de Spring con H2.
 * @AutoConfigureMockMvc: configura MockMvc para simular peticiones HTTP.
 * @ActiveProfiles("test"): usa application-test.properties.
 * @DirtiesContext: resetea la BD H2 entre clases de prueba.
 *
 * Proporciona el método obtenerToken() para autenticarse
 * y usar el JWT en los endpoints protegidos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Obtiene un token JWT haciendo login con las credenciales del DTI inicial.
     * El DTI es creado automáticamente por DataInitializer al arrancar.
     */
    protected String obtenerTokenDTI() throws Exception {
        String loginBody = """
            {
                "correo": "dti@cue.edu.co",
                "password": "TestAdmin2026!"
            }
            """;

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response)
                .path("datos").path("token").asText();
    }

    /**
     * Construye el header Authorization con el token JWT.
     */
    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
