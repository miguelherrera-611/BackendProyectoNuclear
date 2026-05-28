package co.edu.cue.practicas.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas del PATRÓN STATE en InstanciaPractica.
 * Verifica que las transiciones de estado sean válidas y que las
 * transiciones prohibidas lancen excepción.
 *
 * Diagrama de estados:
 *   ASIGNADA_PENDIENTE_INICIO → EN_CURSO → FINALIZADA
 *                                         ↘ CANCELADA
 */
@DisplayName("InstanciaPractica — Patrón State: transiciones de estado")
class InstanciaPracticaTest {

    private InstanciaPractica instancia;

    @BeforeEach
    void setUp() {
        instancia = InstanciaPractica.builder()
                .id(1L)
                .numeroPractica(1)
                .nombre("Práctica Empresarial I")
                .materiaNucleo("Práctica Empresarial")
                .codigoMateria("PE-101")
                .numCortes(3)
                .duracionSemanas(16)
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO)
                .build();
    }

    // =================================================================
    // iniciar()
    // =================================================================

    @Test
    @DisplayName("iniciar() desde ASIGNADA_PENDIENTE_INICIO debe pasar a EN_CURSO")
    void iniciarDesdeAsignadaDebeTransicionarAEnCurso() {
        instancia.iniciar();
        assertThat(instancia.getEstado()).isEqualTo(EstadoPractica.EN_CURSO);
    }

    @Test
    @DisplayName("iniciar() desde EN_CURSO debe lanzar excepción")
    void iniciarDesdeEnCursoLanzaExcepcion() {
        instancia.iniciar(); // → EN_CURSO
        assertThatThrownBy(instancia::iniciar)
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("iniciar");
    }

    @Test
    @DisplayName("iniciar() desde FINALIZADA debe lanzar excepción")
    void iniciarDesdeFinalizedaLanzaExcepcion() {
        instancia.iniciar();
        instancia.finalizar();
        assertThatThrownBy(instancia::iniciar)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // finalizar()
    // =================================================================

    @Test
    @DisplayName("finalizar() desde EN_CURSO debe pasar a FINALIZADA")
    void finalizarDesdeEnCursoDebeTransicionarAFinalizada() {
        instancia.iniciar();
        instancia.finalizar();
        assertThat(instancia.getEstado()).isEqualTo(EstadoPractica.FINALIZADA);
    }

    @Test
    @DisplayName("finalizar() desde ASIGNADA_PENDIENTE_INICIO debe lanzar excepción")
    void finalizarDesdeAsignadaLanzaExcepcion() {
        assertThatThrownBy(instancia::finalizar)
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("finalizar");
    }

    // =================================================================
    // cancelar()
    // =================================================================

    @Test
    @DisplayName("cancelar() desde ASIGNADA_PENDIENTE_INICIO debe pasar a CANCELADA")
    void cancelarDesdeAsignadaDebeTransicionarACancelada() {
        instancia.cancelar();
        assertThat(instancia.getEstado()).isEqualTo(EstadoPractica.CANCELADA);
    }

    @Test
    @DisplayName("cancelar() desde EN_CURSO debe pasar a CANCELADA")
    void cancelarDesdeEnCursoDebeTransicionarACancelada() {
        instancia.iniciar();
        instancia.cancelar();
        assertThat(instancia.getEstado()).isEqualTo(EstadoPractica.CANCELADA);
    }

    @Test
    @DisplayName("cancelar() desde FINALIZADA debe lanzar excepción — OCL inmutabilidad")
    void cancelarDesdeFinalizadaLanzaExcepcion() {
        instancia.iniciar();
        instancia.finalizar();
        assertThatThrownBy(instancia::cancelar)
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("FINALIZADA");
    }

    // =================================================================
    // esInmutable()
    // =================================================================

    @Test
    @DisplayName("esInmutable() debe retornar false cuando está ASIGNADA o EN_CURSO")
    void esInmutableFalseCuandoActiva() {
        assertThat(instancia.esInmutable()).isFalse();
        instancia.iniciar();
        assertThat(instancia.esInmutable()).isFalse();
    }

    @Test
    @DisplayName("esInmutable() debe retornar true cuando está FINALIZADA")
    void esInmutableTrueCuandoFinalizada() {
        instancia.iniciar();
        instancia.finalizar();
        assertThat(instancia.esInmutable()).isTrue();
    }

    @Test
    @DisplayName("esInmutable() debe retornar true cuando está CANCELADA")
    void esInmutableTrueCuandoCancelada() {
        instancia.cancelar();
        assertThat(instancia.esInmutable()).isTrue();
    }
}
