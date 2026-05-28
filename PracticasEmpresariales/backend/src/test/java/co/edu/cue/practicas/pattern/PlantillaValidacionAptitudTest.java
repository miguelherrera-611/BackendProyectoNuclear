package co.edu.cue.practicas.pattern;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.chain.ContextoValidacion;
import co.edu.cue.practicas.pattern.chain.ValidadorAptitud;
import co.edu.cue.practicas.pattern.strategy.EstrategiaValidacion;
import co.edu.cue.practicas.pattern.template.PlantillaValidacionAptitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas del PATRÓN TEMPLATE METHOD — PlantillaValidacionAptitud.
 *
 * Verifica que:
 *   1. El hook onAptoConfirmado() se llama solo cuando todos los validadores pasan.
 *   2. Si algún validador falla, onAptoConfirmado() NO se invoca.
 *   3. El flujo es fijo — no puede cambiarse desde las subclases.
 */
@DisplayName("PlantillaValidacionAptitud — Patrón Template Method")
class PlantillaValidacionAptitudTest {

    private Usuario estudiante;
    private CatalogoPractica catalogo;

    @BeforeEach
    void setUp() {
        estudiante = Usuario.builder()
                .id(1L).nombre("Est. Test").correo("e@test.com")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        catalogo = CatalogoPractica.builder()
                .id(1L).programa(Programa.builder().id(1L).build())
                .numeroPractica(1).nombre("P I").materiaNucleo("PE").codigoMateria("PE-101")
                .numCortes(3).duracionSemanas(16).activo(true).build();
    }

    /**
     * Subclase concreta de prueba que registra si el hook fue invocado.
     * Sigue el patrón Test Double (Spy manual).
     */
    static class PlantillaConSpy extends PlantillaValidacionAptitud {
        boolean hookInvocado = false;

        PlantillaConSpy(EstrategiaValidacion estrategia) {
            super(estrategia);
        }

        @Override
        protected void onAptoConfirmado(ContextoValidacion ctx) {
            hookInvocado = true;
        }
    }

    /** Estrategia que siempre pasa (validador no-op). */
    static EstrategiaValidacion estrategiaSiempresOk() {
        return () -> new ValidadorAptitud() {
            @Override
            protected void ejecutarValidacion(ContextoValidacion ctx) {
                // no lanza → pasa
            }
        };
    }

    /** Estrategia que siempre falla. */
    static EstrategiaValidacion estrategiaSiempreFalla() {
        return () -> new ValidadorAptitud() {
            @Override
            protected void ejecutarValidacion(ContextoValidacion ctx) {
                throw new OperacionNoPermitidaException("Falla intencional en test");
            }
        };
    }

    @Test
    @DisplayName("ejecutar() debe invocar el hook onAptoConfirmado() cuando todos los validadores pasan")
    void ejecutarInvocaHookCuandoValidacionPasa() {
        PlantillaConSpy plantilla = new PlantillaConSpy(estrategiaSiempresOk());

        plantilla.ejecutar(estudiante, catalogo, Optional.empty(), Optional.empty());

        assertThat(plantilla.hookInvocado).isTrue();
    }

    @Test
    @DisplayName("ejecutar() NO debe invocar el hook si algún validador falla")
    void ejecutarNoInvocaHookCuandoValidacionFalla() {
        PlantillaConSpy plantilla = new PlantillaConSpy(estrategiaSiempreFalla());

        assertThatThrownBy(() ->
                plantilla.ejecutar(estudiante, catalogo, Optional.empty(), Optional.empty()))
                .isInstanceOf(OperacionNoPermitidaException.class);

        // El hook nunca debe haberse llamado porque la validación falló
        assertThat(plantilla.hookInvocado).isFalse();
    }

    @Test
    @DisplayName("ejecutar() debe pasar el contexto correcto al hook")
    void ejecutarPasaContextoCorrectoAlHook() {
        // ARRANGE — subclase que captura el contexto recibido
        final ContextoValidacion[] contextoCapturado = {null};
        PlantillaValidacionAptitud plantilla = new PlantillaValidacionAptitud(estrategiaSiempresOk()) {
            @Override
            protected void onAptoConfirmado(ContextoValidacion ctx) {
                contextoCapturado[0] = ctx;
            }
        };
        HojaDeVida hvMock = HojaDeVida.builder().id(99L).estudiante(estudiante).version(1)
                .urlArchivo("f.pdf").build();

        // ACT
        plantilla.ejecutar(estudiante, catalogo, Optional.of(hvMock), Optional.empty());

        // ASSERT — el contexto debe contener el estudiante y el catálogo originales
        assertThat(contextoCapturado[0]).isNotNull();
        assertThat(contextoCapturado[0].estudiante()).isEqualTo(estudiante);
        assertThat(contextoCapturado[0].catalogo()).isEqualTo(catalogo);
        assertThat(contextoCapturado[0].hvActual()).isPresent();
    }
}
