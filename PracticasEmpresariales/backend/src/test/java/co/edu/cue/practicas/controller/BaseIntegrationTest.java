package co.edu.cue.practicas.controller;

import co.edu.cue.practicas.service.notificacion.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Clase base para todas las pruebas de integración.
 *
 * @SpringBootTest: levanta el contexto completo de Spring con H2.
 * @AutoConfigureMockMvc: configura MockMvc para simular peticiones HTTP.
 * @ActiveProfiles("test"): usa application-test.properties.
 * @DirtiesContext: resetea la BD H2 entre clases de prueba.
 * @MockBean EmailService: evita llamadas HTTP reales a SendGrid y permite
 *           capturar el código de verificación 2FA enviado por login().
 *
 * Proporciona el método obtenerTokenDTI() para autenticarse (login en 2 pasos)
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

    @MockBean
    protected EmailService emailService;

    /**
     * Obtiene un token JWT autenticándose con las credenciales del DTI inicial.
     * El DTI es creado automáticamente por DataInitializer al arrancar.
     *
     * El login real tiene 2 pasos (2FA por correo):
     *   1. POST /auth/login           → valida credenciales y "envía" el código (capturado del mock)
     *   2. POST /auth/login/verificar → valida el código y entrega el JWT
     */
    protected String obtenerTokenDTI() throws Exception {
        return obtenerToken("dti@cue.edu.co", "TestAdmin2026!");
    }

    /**
     * Igual que obtenerTokenDTI() pero para cualquier usuario/contraseña.
     */
    protected String obtenerToken(String correo, String password) throws Exception {
        String loginBody = """
            {
                "correo": "%s",
                "password": "%s"
            }
            """.formatted(correo, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn();

        ArgumentCaptor<String> codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoLogin(any(), any(), codigoCaptor.capture());
        clearInvocations(emailService);
        String codigo = codigoCaptor.getValue();

        String verificarBody = """
            {
                "correo": "%s",
                "codigo": "%s"
            }
            """.formatted(correo, codigo);

        MvcResult result = mockMvc.perform(post("/auth/login/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificarBody))
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
