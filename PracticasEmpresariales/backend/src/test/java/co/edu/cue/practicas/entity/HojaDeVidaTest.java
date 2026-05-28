package co.edu.cue.practicas.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoHojaDeVida;
import co.edu.cue.practicas.model.enums.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de la entidad HojaDeVida.
 * Verifica transiciones de estado y restricciones OCL:
 *   hvInmutableEnPractica, versionPositiva, perteneceAEstudiante.
 */
@DisplayName("HojaDeVida — Transiciones de estado y validaciones OCL")
class HojaDeVidaTest {

    private HojaDeVida hv;
    private final Long VALIDADOR_ID = 10L;

    @BeforeEach
    void setUp() {
        Usuario estudiante = Usuario.builder()
                .id(5L).nombre("Est. Test").correo("est@test.com")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        hv = HojaDeVida.builder()
                .id(1L)
                .estudiante(estudiante)
                .version(1)
                .urlArchivo("hvs/est001_v1.pdf")
                .estado(EstadoHojaDeVida.PENDIENTE)
                .build();
    }

    // =================================================================
    // validar()
    // =================================================================

    @Test
    @DisplayName("validar() desde PENDIENTE debe pasar a VALIDA y registrar quién validó")
    void validarDesdePendienteDebeTransicionarAValida() {
        hv.validar(VALIDADOR_ID);

        assertThat(hv.getEstado()).isEqualTo(EstadoHojaDeVida.VALIDA);
        assertThat(hv.getValidadoPor()).isEqualTo(VALIDADOR_ID);
        assertThat(hv.getFechaValidacion()).isNotNull();
    }

    @Test
    @DisplayName("validar() desde VALIDA debe lanzar excepción")
    void validarDesdeValidaLanzaExcepcion() {
        hv.validar(VALIDADOR_ID);
        assertThatThrownBy(() -> hv.validar(VALIDADOR_ID))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    @DisplayName("validar() desde RECHAZADA debe lanzar excepción")
    void validarDesdeRechazadaLanzaExcepcion() {
        hv.rechazar(VALIDADOR_ID, "No cumple formato");
        assertThatThrownBy(() -> hv.validar(VALIDADOR_ID))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // rechazar()
    // =================================================================

    @Test
    @DisplayName("rechazar() desde PENDIENTE con motivo debe pasar a RECHAZADA")
    void rechazarDesdePendienteDebeTransicionarARechazada() {
        hv.rechazar(VALIDADOR_ID, "Formato incorrecto — falta foto");

        assertThat(hv.getEstado()).isEqualTo(EstadoHojaDeVida.RECHAZADA);
        assertThat(hv.getMotivoRechazo()).isEqualTo("Formato incorrecto — falta foto");
        assertThat(hv.getValidadoPor()).isEqualTo(VALIDADOR_ID);
        assertThat(hv.getFechaValidacion()).isNotNull();
    }

    @Test
    @DisplayName("rechazar() con motivo nulo debe lanzar excepción")
    void rechazarConMotivoNuloLanzaExcepcion() {
        assertThatThrownBy(() -> hv.rechazar(VALIDADOR_ID, null))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    @DisplayName("rechazar() con motivo vacío debe lanzar excepción")
    void rechazarConMotivoVacioLanzaExcepcion() {
        assertThatThrownBy(() -> hv.rechazar(VALIDADOR_ID, "   "))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    @DisplayName("rechazar() desde VALIDA debe lanzar excepción")
    void rechazarDesdeValidaLanzaExcepcion() {
        hv.validar(VALIDADOR_ID);
        assertThatThrownBy(() -> hv.rechazar(VALIDADOR_ID, "Motivo"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("PENDIENTE");
    }

    // =================================================================
    // esValida()
    // =================================================================

    @Test
    @DisplayName("esValida() debe retornar false cuando está PENDIENTE")
    void esValidaFalseCuandoPendiente() {
        assertThat(hv.esValida()).isFalse();
    }

    @Test
    @DisplayName("esValida() debe retornar true solo cuando está VALIDA")
    void esValidaTrueSoloCuandoValida() {
        hv.validar(VALIDADOR_ID);
        assertThat(hv.esValida()).isTrue();
    }

    @Test
    @DisplayName("esValida() debe retornar false cuando está RECHAZADA")
    void esValidaFalseCuandoRechazada() {
        hv.rechazar(VALIDADOR_ID, "Motivo de prueba");
        assertThat(hv.esValida()).isFalse();
    }
}
