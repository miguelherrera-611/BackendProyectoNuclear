package co.edu.cue.practicas.pattern;

import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.pattern.builder.CatalogoPracticaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas del PATRÓN BUILDER — CatalogoPracticaBuilder.
 * Verifica que la construcción sea correcta y que los campos
 * obligatorios sean validados antes de crear la entidad.
 */
@DisplayName("CatalogoPracticaBuilder — Patrón Builder")
class CatalogoPracticaBuilderTest {

    private Programa programaEjemplo;

    @BeforeEach
    void setUp() {
        programaEjemplo = Programa.builder()
                .id(1L)
                .nombre("Ingeniería de Sistemas")
                .build();
    }

    // =================================================================
    // Construcción exitosa
    // =================================================================

    @Test
    @DisplayName("build() con todos los campos obligatorios debe construir el catálogo correctamente")
    void buildConDatosCompletosExitoso() {
        CatalogoPractica catalogo = new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo)
                .conNumero(1)
                .conNombre("Práctica Empresarial I")
                .conMateriaNucleo("Práctica Empresarial", "PE-101")
                .conCortes(3)
                .conDuracion(16)
                .conDocumentos("Carta, Póliza, Plan")
                .build();

        assertThat(catalogo.getPrograma()).isEqualTo(programaEjemplo);
        assertThat(catalogo.getNumeroPractica()).isEqualTo(1);
        assertThat(catalogo.getNombre()).isEqualTo("Práctica Empresarial I");
        assertThat(catalogo.getMateriaNucleo()).isEqualTo("Práctica Empresarial");
        assertThat(catalogo.getCodigoMateria()).isEqualTo("PE-101");
        assertThat(catalogo.getNumCortes()).isEqualTo(3);
        assertThat(catalogo.getDuracionSemanas()).isEqualTo(16);
        assertThat(catalogo.getDocumentosRequeridos()).isEqualTo("Carta, Póliza, Plan");
    }

    @Test
    @DisplayName("build() debe iniciar el catálogo en estado activo por defecto")
    void buildDebeIniciarActivo() {
        CatalogoPractica catalogo = buildValido();
        assertThat(catalogo.isActivo()).isTrue();
    }

    @Test
    @DisplayName("build() debe asignar creadoEn automáticamente")
    void buildDebeAsignarCreadoEn() {
        CatalogoPractica catalogo = buildValido();
        assertThat(catalogo.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("Dos build() independientes deben producir objetos distintos")
    void dosBuildProducenInstanciasIndependientes() {
        CatalogoPractica c1 = new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo).conNumero(1).conNombre("Práctica I")
                .conMateriaNucleo("PE", "PE-101").conCortes(3).conDuracion(16).build();

        CatalogoPractica c2 = new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo).conNumero(2).conNombre("Práctica II")
                .conMateriaNucleo("PE II", "PE-201").conCortes(2).conDuracion(12).build();

        assertThat(c1.getNombre()).isNotEqualTo(c2.getNombre());
        assertThat(c1.getNumeroPractica()).isNotEqualTo(c2.getNumeroPractica());
    }

    // =================================================================
    // Validaciones — campos obligatorios
    // =================================================================

    @Test
    @DisplayName("build() sin programa debe lanzar IllegalStateException")
    void buildSinProgramaLanzaExcepcion() {
        assertThatThrownBy(() -> new CatalogoPracticaBuilder()
                .conNumero(1).conNombre("P I").conMateriaNucleo("PE", "PE-101")
                .conCortes(3).conDuracion(16).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("programa");
    }

    @Test
    @DisplayName("build() con nombre vacío debe lanzar IllegalStateException")
    void buildConNombreVacioLanzaExcepcion() {
        assertThatThrownBy(() -> new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo).conNumero(1).conNombre("")
                .conMateriaNucleo("PE", "PE-101").conCortes(3).conDuracion(16).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("build() con materiaNucleo vacía debe lanzar IllegalStateException")
    void buildConMateriaNucleoVaciaLanzaExcepcion() {
        assertThatThrownBy(() -> new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo).conNumero(1).conNombre("P I")
                .conMateriaNucleo("", "PE-101").conCortes(3).conDuracion(16).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("materia núcleo");
    }

    @Test
    @DisplayName("build() con numCortes < 1 debe lanzar IllegalStateException")
    void buildConCortesCeroLanzaExcepcion() {
        assertThatThrownBy(() -> new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo).conNumero(1).conNombre("P I")
                .conMateriaNucleo("PE", "PE-101").conCortes(0).conDuracion(16).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cortes");
    }

    @Test
    @DisplayName("build() con duracion < 1 debe lanzar IllegalStateException")
    void buildConDuracionCeroLanzaExcepcion() {
        assertThatThrownBy(() -> new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo).conNumero(1).conNombre("P I")
                .conMateriaNucleo("PE", "PE-101").conCortes(3).conDuracion(0).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("semanas");
    }

    // Helper para construir un catálogo válido sin repetir código
    private CatalogoPractica buildValido() {
        return new CatalogoPracticaBuilder()
                .conPrograma(programaEjemplo)
                .conNumero(1)
                .conNombre("Práctica I")
                .conMateriaNucleo("PE", "PE-101")
                .conCortes(3)
                .conDuracion(16)
                .build();
    }
}
