package co.edu.cue.practicas.pattern;

import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.prototype.CatalogoPracticaPlantilla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas del PATRÓN PROTOTYPE — CatalogoPracticaPlantilla.
 *
 * Verifica que clonar() produzca un snapshot fiel del catálogo vigente
 * y que la instancia creada sea independiente del catálogo origen.
 *
 * Regla de negocio GPE-141:
 *   "Los cambios al catálogo NO afectan las prácticas activas."
 */
@DisplayName("CatalogoPracticaPlantilla — Patrón Prototype: clonación de catálogo")
class CatalogoPracticaPlantillaTest {

    private CatalogoPractica catalogo;
    private ExpedienteEstudiante expediente;

    @BeforeEach
    void setUp() {
        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas").build();

        catalogo = CatalogoPractica.builder()
                .id(10L)
                .programa(programa)
                .numeroPractica(1)
                .nombre("Práctica Empresarial I")
                .materiaNucleo("Práctica Empresarial")
                .codigoMateria("PE-101")
                .numCortes(3)
                .duracionSemanas(16)
                .documentosRequeridos("Carta aceptación, Póliza")
                .activo(true)
                .build();

        Usuario estudiante = Usuario.builder()
                .id(5L).nombre("Juan Estudiante").correo("juan@cue.edu.co")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        expediente = ExpedienteEstudiante.builder()
                .id(1L)
                .estudiante(estudiante)
                .build();
    }

    @Test
    @DisplayName("clonar() debe crear una InstanciaPractica en estado ASIGNADA_PENDIENTE_INICIO")
    void clonarDebeCrearInstanciaEnEstadoInicial() {
        InstanciaPractica instancia = new CatalogoPracticaPlantilla(catalogo, expediente).clonar();

        assertThat(instancia.getEstado()).isEqualTo(EstadoPractica.ASIGNADA_PENDIENTE_INICIO);
    }

    @Test
    @DisplayName("clonar() debe copiar todos los campos del catálogo como snapshot")
    void clonarDebeCopiarTodosLosCampos() {
        InstanciaPractica instancia = new CatalogoPracticaPlantilla(catalogo, expediente).clonar();

        assertThat(instancia.getCatalogoPracticaId()).isEqualTo(10L);
        assertThat(instancia.getNumeroPractica()).isEqualTo(1);
        assertThat(instancia.getNombre()).isEqualTo("Práctica Empresarial I");
        assertThat(instancia.getMateriaNucleo()).isEqualTo("Práctica Empresarial");
        assertThat(instancia.getCodigoMateria()).isEqualTo("PE-101");
        assertThat(instancia.getNumCortes()).isEqualTo(3);
        assertThat(instancia.getDuracionSemanas()).isEqualTo(16);
        assertThat(instancia.getDocumentosRequeridos()).isEqualTo("Carta aceptación, Póliza");
    }

    @Test
    @DisplayName("clonar() debe vincular la instancia al expediente dado")
    void clonarDebeVincularAlExpediente() {
        InstanciaPractica instancia = new CatalogoPracticaPlantilla(catalogo, expediente).clonar();

        assertThat(instancia.getExpediente()).isEqualTo(expediente);
    }

    @Test
    @DisplayName("modificar el catálogo después de clonar NO debe afectar la instancia ya creada")
    void modificarCatalogoPostClonNoAfectaInstancia() {
        // ARRANGE — clonamos con el nombre original
        InstanciaPractica instancia = new CatalogoPracticaPlantilla(catalogo, expediente).clonar();
        String nombreOriginal = instancia.getNombre();

        // ACT — simulamos un cambio de nombre en el catálogo (ej. corrección posterior)
        catalogo.setNombre("Práctica Empresarial I — Versión Actualizada");

        // ASSERT — la instancia conserva el snapshot, no se ve afectada
        assertThat(instancia.getNombre()).isEqualTo(nombreOriginal);
        assertThat(instancia.getNombre()).isNotEqualTo(catalogo.getNombre());
    }

    @Test
    @DisplayName("dos clonar() consecutivos deben producir instancias independientes")
    void dosClonadosProducenInstanciasIndependientes() {
        InstanciaPractica i1 = new CatalogoPracticaPlantilla(catalogo, expediente).clonar();
        InstanciaPractica i2 = new CatalogoPracticaPlantilla(catalogo, expediente).clonar();

        // Son objetos distintos aunque con los mismos datos
        assertThat(i1).isNotSameAs(i2);
        assertThat(i1.getNombre()).isEqualTo(i2.getNombre());
    }
}
