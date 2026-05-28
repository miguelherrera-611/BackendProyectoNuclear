package co.edu.cue.practicas.pattern;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoHojaDeVida;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.chain.ContextoValidacion;
import co.edu.cue.practicas.pattern.chain.ValidadorCatalogoActivo;
import co.edu.cue.practicas.pattern.chain.ValidadorHojaDeVidaValida;
import co.edu.cue.practicas.pattern.chain.ValidadorPracticaAnteriorFinalizada;
import co.edu.cue.practicas.pattern.strategy.EstrategiaValidacionEstandar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas del PATRÓN CHAIN OF RESPONSIBILITY — Validadores de Aptitud.
 *
 * Cada clase anidada (@Nested) prueba un eslabón independiente.
 * La última clase prueba la cadena completa construida por EstrategiaValidacionEstandar.
 *
 * GPE-145: cadena secuencial — si un eslabón falla, la cadena se detiene
 * y lanza OperacionNoPermitidaException.
 */
@DisplayName("Chain of Responsibility — Validadores de Aptitud (GPE-145)")
class ValidadoresAptitudTest {

    private Usuario estudiante;
    private CatalogoPractica catalogoActivo;

    @BeforeEach
    void setUp() {
        estudiante = Usuario.builder()
                .id(1L).nombre("Estudiante").correo("est@test.com")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        catalogoActivo = CatalogoPractica.builder()
                .id(1L)
                .programa(Programa.builder().id(1L).nombre("Sistemas").build())
                .numeroPractica(1).nombre("Práctica I")
                .materiaNucleo("PE").codigoMateria("PE-101")
                .numCortes(3).duracionSemanas(16).activo(true)
                .build();
    }

    // Helpers para HV
    private HojaDeVida hvValida() {
        HojaDeVida hv = HojaDeVida.builder()
                .id(1L).estudiante(estudiante).version(1)
                .urlArchivo("hv.pdf").estado(EstadoHojaDeVida.PENDIENTE).build();
        hv.validar(10L);
        return hv;
    }

    private HojaDeVida hvPendiente() {
        return HojaDeVida.builder()
                .id(2L).estudiante(estudiante).version(1)
                .urlArchivo("hv.pdf").estado(EstadoHojaDeVida.PENDIENTE).build();
    }

    // =================================================================
    @Nested
    @DisplayName("Eslabón 1 — ValidadorCatalogoActivo")
    class ValidadorCatalogoActivoTest {

        @Test
        @DisplayName("debe pasar cuando el catálogo está activo")
        void pasaCuandoCatalogoActivo() {
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.of(hvValida()), Optional.empty());

            assertThatCode(() -> new ValidadorCatalogoActivo().validar(ctx))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("debe fallar cuando el catálogo es null — catálogo no configurado")
        void fallaCuandoCatalogoNull() {
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, null, Optional.empty(), Optional.empty());

            assertThatThrownBy(() -> new ValidadorCatalogoActivo().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("catálogo");
        }

        @Test
        @DisplayName("debe fallar cuando el catálogo está inactivo")
        void fallaCuandoCatalogoInactivo() {
            catalogoActivo.desactivar();
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.empty(), Optional.empty());

            assertThatThrownBy(() -> new ValidadorCatalogoActivo().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("inactivo");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("Eslabón 2 — ValidadorHojaDeVidaValida")
    class ValidadorHojaDeVidaValidaTest {

        @Test
        @DisplayName("debe pasar cuando la HV está en estado VALIDA")
        void pasaCuandoHvValida() {
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.of(hvValida()), Optional.empty());

            assertThatCode(() -> new ValidadorHojaDeVidaValida().validar(ctx))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("debe fallar cuando no tiene HV")
        void fallaCuandoSinHv() {
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.empty(), Optional.empty());

            assertThatThrownBy(() -> new ValidadorHojaDeVidaValida().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("VÁLIDA");
        }

        @Test
        @DisplayName("debe fallar cuando la HV está PENDIENTE")
        void fallaCuandoHvPendiente() {
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.of(hvPendiente()), Optional.empty());

            assertThatThrownBy(() -> new ValidadorHojaDeVidaValida().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("VÁLIDA");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("Eslabón 3 — ValidadorPracticaAnteriorFinalizada")
    class ValidadorPracticaAnteriorFinalizadaTest {

        @Test
        @DisplayName("debe pasar para la Práctica 1 sin importar si hay anterior")
        void pasaSiempreParaPrimeraPractica() {
            // numeroPractica = 1 → no requiere anterior
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.empty(), Optional.empty());

            assertThatCode(() -> new ValidadorPracticaAnteriorFinalizada().validar(ctx))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("debe pasar para Práctica 2 cuando la Práctica 1 está FINALIZADA")
        void pasaParaPractica2CuandoAnteriorFinalizada() {
            CatalogoPractica catalogo2 = CatalogoPractica.builder()
                    .id(2L).programa(Programa.builder().id(1L).build())
                    .numeroPractica(2).nombre("Práctica II")
                    .materiaNucleo("PE").codigoMateria("PE-201")
                    .numCortes(2).duracionSemanas(12).activo(true).build();

            InstanciaPractica practica1Finalizada = InstanciaPractica.builder()
                    .numeroPractica(1).estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO).build();
            practica1Finalizada.iniciar();
            practica1Finalizada.finalizar();

            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogo2, Optional.empty(),
                    Optional.of(practica1Finalizada));

            assertThatCode(() -> new ValidadorPracticaAnteriorFinalizada().validar(ctx))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("debe fallar para Práctica 2 cuando la Práctica 1 está EN_CURSO")
        void fallaCuandoAnteriorNoFinalizada() {
            CatalogoPractica catalogo2 = CatalogoPractica.builder()
                    .id(2L).programa(Programa.builder().id(1L).build())
                    .numeroPractica(2).nombre("Práctica II")
                    .materiaNucleo("PE").codigoMateria("PE-201")
                    .numCortes(2).duracionSemanas(12).activo(true).build();

            InstanciaPractica practica1EnCurso = InstanciaPractica.builder()
                    .numeroPractica(1).estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO).build();
            practica1EnCurso.iniciar(); // → EN_CURSO, no FINALIZADA

            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogo2, Optional.empty(),
                    Optional.of(practica1EnCurso));

            assertThatThrownBy(() -> new ValidadorPracticaAnteriorFinalizada().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("Práctica 1");
        }

        @Test
        @DisplayName("debe fallar para Práctica 2 cuando no existe práctica anterior")
        void fallaCuandoNoExisteAnterior() {
            CatalogoPractica catalogo2 = CatalogoPractica.builder()
                    .id(2L).programa(Programa.builder().id(1L).build())
                    .numeroPractica(2).nombre("Práctica II")
                    .materiaNucleo("PE").codigoMateria("PE-201")
                    .numCortes(2).duracionSemanas(12).activo(true).build();

            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogo2, Optional.empty(), Optional.empty());

            assertThatThrownBy(() -> new ValidadorPracticaAnteriorFinalizada().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("Práctica 1");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("Cadena completa — EstrategiaValidacionEstandar")
    class CadenaCompletaTest {

        @Test
        @DisplayName("cadena completa pasa cuando todos los requisitos se cumplen")
        void cadenaCompletaPassCuandoTodoValido() {
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.of(hvValida()), Optional.empty());

            assertThatCode(() -> new EstrategiaValidacionEstandar()
                    .construirCadena().validar(ctx))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("cadena se detiene en el primer eslabón si el catálogo es null")
        void cadenaSeDetieneEnPrimerEslabonSinCatalogo() {
            // Sin catálogo → el primer eslabón (ValidadorCatalogoActivo) falla
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, null, Optional.of(hvValida()), Optional.empty());

            assertThatThrownBy(() -> new EstrategiaValidacionEstandar()
                    .construirCadena().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("catálogo");
        }

        @Test
        @DisplayName("cadena se detiene en el segundo eslabón si la HV no es válida")
        void cadenaSeDetieneEnSegundoEslabonSinHv() {
            // Catálogo OK, HV PENDIENTE → falla en ValidadorHojaDeVidaValida
            ContextoValidacion ctx = new ContextoValidacion(
                    estudiante, catalogoActivo, Optional.of(hvPendiente()), Optional.empty());

            assertThatThrownBy(() -> new EstrategiaValidacionEstandar()
                    .construirCadena().validar(ctx))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("VÁLIDA");
        }
    }
}
