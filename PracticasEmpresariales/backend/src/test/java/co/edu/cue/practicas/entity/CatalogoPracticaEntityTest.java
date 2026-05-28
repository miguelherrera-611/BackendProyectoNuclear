package co.edu.cue.practicas.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.Programa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de la entidad CatalogoPractica.
 * Verifica la regla OCL noDesactivarConEstudiantesActivos mediante el método desactivar().
 */
@DisplayName("CatalogoPractica — Reglas OCL de desactivación")
class CatalogoPracticaEntityTest {

    private CatalogoPractica catalogo;

    @BeforeEach
    void setUp() {
        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas").build();

        catalogo = CatalogoPractica.builder()
                .id(1L)
                .programa(programa)
                .numeroPractica(1)
                .nombre("Práctica Empresarial I")
                .materiaNucleo("Práctica Empresarial")
                .codigoMateria("PE-101")
                .numCortes(3)
                .duracionSemanas(16)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("desactivar() en catálogo activo debe marcarlo como inactivo")
    void desactivarActivoDebePonerInactivo() {
        catalogo.desactivar();
        assertThat(catalogo.isActivo()).isFalse();
    }

    @Test
    @DisplayName("desactivar() en catálogo ya inactivo debe lanzar excepción")
    void desactivarInactivoLanzaExcepcion() {
        catalogo.desactivar(); // primera vez — OK
        assertThatThrownBy(catalogo::desactivar) // segunda vez — falla
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("catálogo recién construido debe estar activo por defecto")
    void catalogoNuevoDebeEstarActivo() {
        assertThat(catalogo.isActivo()).isTrue();
    }
}
