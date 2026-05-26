package co.edu.cue.practicas.builder;

import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.service.programa.ProgramaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias del ProgramaBuilder.
 *
 * No usa Spring ni base de datos — es puro Java.
 * Verifica que el Builder construye correctamente, aplica los valores
 * por defecto y lanza excepciones cuando faltan datos obligatorios.
 *
 * Cómo ejecutar solo esta clase en IntelliJ:
 *   Clic derecho sobre la clase → Run 'ProgramaBuilderTest'
 */
@DisplayName("ProgramaBuilder — Pruebas del patrón Builder")
class ProgramaBuilderTest {

    // Facultad de prueba reutilizada en todos los tests
    private Facultad facultadEjemplo;

    /**
     * @BeforeEach se ejecuta antes de CADA método @Test.
     * Aquí preparamos los datos base que necesitan los tests.
     */
    @BeforeEach
    void setUp() {
        // Construimos una facultad de prueba con el mínimo de datos necesarios
        facultadEjemplo = Facultad.builder()
                .id(1L)
                .nombre("Facultad de Ingeniería")
                .activa(true)
                .build();
    }

    // =================================================================
    // TESTS DE CONSTRUCCIÓN EXITOSA
    // =================================================================

    @Test
    @DisplayName("Debe construir un programa con los datos mínimos obligatorios")
    void debeContruirProgramaConDatosMinimos() {
        // ARRANGE — preparamos los datos de entrada
        // (ya preparados en setUp)

        // ACT — ejecutamos la acción que queremos probar
        Programa programa = ProgramaBuilder.nuevo()
                .conNombre("Ingeniería de Sistemas")
                .enFacultad(facultadEjemplo)
                .construir();

        // ASSERT — verificamos que el resultado es el esperado
        assertThat(programa.getNombre()).isEqualTo("Ingeniería de Sistemas");
        assertThat(programa.getFacultad()).isEqualTo(facultadEjemplo);
        assertThat(programa.isActivo()).isTrue();                // todo programa nuevo empieza activo
        assertThat(programa.getNumeroTotalPracticas()).isEqualTo(1);     // valor por defecto
        assertThat(programa.getPromedioMinimoGeneral()).isEqualTo(3.0);  // valor por defecto
    }

    @Test
    @DisplayName("Debe construir programa con todos los datos completos")
    void debeConstruirProgramaCompleto() {
        Programa programa = ProgramaBuilder.nuevo()
                .conNombre("Administración de Empresas")
                .conDescripcion("Programa de pregrado en administración")
                .enFacultad(facultadEjemplo)
                .conNumeroDePracticas(2)
                .conPromedioMinimoGeneral(3.5)
                .construir();

        assertThat(programa.getNombre()).isEqualTo("Administración de Empresas");
        assertThat(programa.getDescripcion()).isEqualTo("Programa de pregrado en administración");
        assertThat(programa.getNumeroTotalPracticas()).isEqualTo(2);
        assertThat(programa.getPromedioMinimoGeneral()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("Debe agregar requisitos de práctica correctamente")
    void debeAgregarRequisitosDePractica() {
        Programa programa = ProgramaBuilder.nuevo()
                .conNombre("Ingeniería de Sistemas")
                .enFacultad(facultadEjemplo)
                .conNumeroDePracticas(2)
                .agregarRequisitoPractica(1, 80, 3.2, false, "Hoja de vida")
                .agregarRequisitoPractica(2, 120, 3.5, true, "Carta de intención")
                .construir();

        // Verificamos que se guardaron los dos requisitos
        assertThat(programa.getRequisitos()).hasSize(2);

        // Verificamos los datos del primer requisito
        var req1 = programa.getRequisitos().get(0);
        assertThat(req1.getNumeroPractica()).isEqualTo(1);
        assertThat(req1.getCreditosMinimos()).isEqualTo(80);
        assertThat(req1.getPromedioMinimo()).isEqualTo(3.2);
        assertThat(req1.isRequierePracticaAnteriorAprobada()).isFalse();

        // Verificamos que cada requisito queda vinculado al programa (relación bidireccional)
        assertThat(req1.getPrograma()).isEqualTo(programa);
    }

    @Test
    @DisplayName("Debe iniciar con valores por defecto cuando no se configuran")
    void debeUsarValoresPorDefecto() {
        Programa programa = ProgramaBuilder.nuevo()
                .conNombre("Contaduría")
                .enFacultad(facultadEjemplo)
                .construir();

        // Si no se configura, el Builder usa 1 práctica y promedio mínimo de 3.0
        assertThat(programa.getNumeroTotalPracticas()).isEqualTo(1);
        assertThat(programa.getPromedioMinimoGeneral()).isEqualTo(3.0);
        assertThat(programa.getRequisitos()).isEmpty();
    }

    // =================================================================
    // TESTS DE VALIDACIÓN — casos donde debe lanzar excepción
    // =================================================================

    @Test
    @DisplayName("Debe lanzar excepción si el nombre está vacío")
    void debeLanzarExcepcionSiNombreEsVacio() {
        // assertThatThrownBy verifica que el código lanza la excepción esperada
        assertThatThrownBy(() ->
                ProgramaBuilder.nuevo()
                        .conNombre("")         // nombre vacío
                        .enFacultad(facultadEjemplo)
                        .construir()
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nombre es null")
    void debeLanzarExcepcionSiNombreEsNull() {
        assertThatThrownBy(() ->
                ProgramaBuilder.nuevo()
                        .enFacultad(facultadEjemplo)
                        .construir()  // nombre nunca se configuró → es null
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("Debe lanzar excepción si la facultad es null")
    void debeLanzarExcepcionSiFacultadEsNull() {
        assertThatThrownBy(() ->
                ProgramaBuilder.nuevo()
                        .conNombre("Ingeniería de Sistemas")
                        // No se llama a enFacultad() → facultad queda null
                        .construir()
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("facultad");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el número de prácticas es cero")
    void debeLanzarExcepcionSiNumeroDePracticasEsCero() {
        assertThatThrownBy(() ->
                ProgramaBuilder.nuevo()
                        .conNombre("Ingeniería de Sistemas")
                        .enFacultad(facultadEjemplo)
                        .conNumeroDePracticas(0)  // no se permiten 0 prácticas
                        .construir()
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("práctica");
    }

    @Test
    @DisplayName("Cada llamada a nuevo() debe crear una instancia independiente")
    void cadaLlamadaNuevoDebeCrearInstanciaIndependiente() {
        // Construimos dos programas distintos con el mismo Builder
        Programa p1 = ProgramaBuilder.nuevo()
                .conNombre("Sistemas")
                .enFacultad(facultadEjemplo)
                .construir();

        Programa p2 = ProgramaBuilder.nuevo()
                .conNombre("Administración")
                .enFacultad(facultadEjemplo)
                .construir();

        // Verificamos que son objetos distintos con datos distintos
        assertThat(p1.getNombre()).isNotEqualTo(p2.getNombre());
    }
}
