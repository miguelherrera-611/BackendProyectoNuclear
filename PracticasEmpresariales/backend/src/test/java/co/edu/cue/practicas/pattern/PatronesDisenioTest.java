package co.edu.cue.practicas.pattern;

import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.Vacante;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import co.edu.cue.practicas.pattern.builder.EmpresaBuilder;
import co.edu.cue.practicas.pattern.builder.VacanteBuilder;
import co.edu.cue.practicas.pattern.builder.VacanteDirector;
import co.edu.cue.practicas.pattern.prototype.EmpresaPlantilla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias — Patrones de diseño (Builder y Prototype)
 */
@DisplayName("Patrones de Diseño — Builder y Prototype")
class PatronesDisenioTest {

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — PATRÓN BUILDER (EmpresaBuilder)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ EmpresaBuilder debe construir empresa con todos los campos")
    void empresaBuilder_conTodosLosCampos_debeConstruirCorrectamente() {
        // ACT
        Empresa empresa = new EmpresaBuilder()
                .conRazonSocial("TechCo S.A.")
                .conNit("900.123.456-7")
                .conSector("Tecnología")
                .conDireccion("Calle 10", "Armenia")
                .conTelefono("3001234567")
                .conContacto("Juan Pérez", "juan@techco.com")
                .conAreas(List.of("Desarrollo", "QA"))
                .build();

        // ASSERT
        assertNotNull(empresa);
        assertEquals("TechCo S.A.", empresa.getRazonSocial());
        assertEquals("900.123.456-7", empresa.getNit());
        assertEquals(EstadoEmpresa.INACTIVA, empresa.getEstado()); // siempre inicia INACTIVA
        assertEquals(2, empresa.getAreasDisponibles().size());
    }

    @Test
    @DisplayName("❌ EmpresaBuilder sin razón social debe lanzar excepción")
    void empresaBuilder_sinRazonSocial_debeLanzarExcepcion() {
        assertThrows(IllegalStateException.class, () ->
                new EmpresaBuilder()
                        .conNit("900.123.456-7")
                        .conContacto("Juan", "juan@techco.com")
                        .build()
        );
    }

    @Test
    @DisplayName("❌ EmpresaBuilder sin NIT debe lanzar excepción")
    void empresaBuilder_sinNit_debeLanzarExcepcion() {
        assertThrows(IllegalStateException.class, () ->
                new EmpresaBuilder()
                        .conRazonSocial("TechCo")
                        .conContacto("Juan", "juan@techco.com")
                        .build()
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — PATRÓN BUILDER (VacanteBuilder + VacanteDirector)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ VacanteBuilder debe construir vacante en estado PENDIENTE")
    void vacanteBuilder_conDatosValidos_debeCrearVacantePendiente() {
        // ARRANGE
        Empresa empresa = new EmpresaBuilder()
                .conRazonSocial("TechCo").conNit("123")
                .conContacto("Juan", "juan@techco.com").build();

        // ACT
        Vacante vacante = new VacanteBuilder()
                .conEmpresa(empresa)
                .conArea("Desarrollo de software")
                .conCupos(3)
                .build();

        // ASSERT
        assertNotNull(vacante);
        assertEquals("Desarrollo de software", vacante.getArea());
        assertEquals(3, vacante.getCuposTotales());
        assertEquals(0, vacante.getCuposOcupados());
        assertEquals(EstadoVacante.PENDIENTE, vacante.getEstado()); // siempre inicia PENDIENTE
    }

    @Test
    @DisplayName("✅ VacanteDirector debe construir vacante estándar correctamente")
    void vacanteDirector_construirEstandar_debeFuncionar() {
        // ARRANGE
        Empresa empresa = new EmpresaBuilder()
                .conRazonSocial("TechCo").conNit("123")
                .conContacto("Juan", "juan@techco.com").build();
        VacanteDirector director = new VacanteDirector();

        // ACT
        Vacante vacante = director.construirVacanteEstandar(empresa, "QA Testing", 2);

        // ASSERT
        assertEquals("QA Testing", vacante.getArea());
        assertEquals(2, vacante.getCuposTotales());
        assertEquals(EstadoVacante.PENDIENTE, vacante.getEstado());
    }

    @Test
    @DisplayName("❌ VacanteBuilder sin empresa debe lanzar excepción")
    void vacanteBuilder_sinEmpresa_debeLanzarExcepcion() {
        assertThrows(IllegalStateException.class, () ->
                new VacanteBuilder().conArea("Desarrollo").conCupos(2).build()
        );
    }

    @Test
    @DisplayName("❌ VacanteBuilder con cupos 0 debe lanzar excepción")
    void vacanteBuilder_conCeropCupos_debeLanzarExcepcion() {
        Empresa empresa = new EmpresaBuilder()
                .conRazonSocial("TechCo").conNit("123")
                .conContacto("Juan", "juan@techco.com").build();
        assertThrows(IllegalStateException.class, () ->
                new VacanteBuilder().conEmpresa(empresa).conArea("Dev").conCupos(0).build()
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — PATRÓN PROTOTYPE (EmpresaPlantilla)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Clonar empresa debe copiar configuración base")
    void empresaPlantilla_clonar_debeCopiarConfiguracion() {
        // ARRANGE
        Empresa original = new EmpresaBuilder()
                .conRazonSocial("TechCo S.A.")
                .conNit("900.123.456-7")
                .conSector("Tecnología")
                .conDireccion("Calle 10", "Armenia")
                .conTelefono("3001234567")
                .conContacto("Juan", "juan@techco.com")
                .conAreas(List.of("Desarrollo", "QA"))
                .build();

        // ACT
        Empresa clon = new EmpresaPlantilla(original).clonar();

        // ASSERT
        assertNotNull(clon);
        assertNull(clon.getId()); // el clon no tiene ID aún
        assertNull(clon.getRazonSocial()); // razón social no se clona
        assertNull(clon.getNit()); // NIT no se clona
        assertEquals("Tecnología", clon.getSector()); // sector sí se clona
        assertEquals("Armenia", clon.getMunicipio()); // municipio sí
        assertEquals("3001234567", clon.getTelefono()); // teléfono sí
        assertEquals(EstadoEmpresa.INACTIVA, clon.getEstado()); // siempre INACTIVA
        assertEquals(2, clon.getAreasDisponibles().size()); // áreas se clonan
    }

    @Test
    @DisplayName("✅ El clon debe ser independiente del original")
    void empresaPlantilla_clon_debeSerIndependienteDelOriginal() {
        // ARRANGE
        Empresa original = new EmpresaBuilder()
                .conRazonSocial("TechCo").conNit("123")
                .conSector("Tecnología")
                .conContacto("Juan", "juan@techco.com")
                .conAreas(List.of("Desarrollo"))
                .build();

        // ACT
        Empresa clon = new EmpresaPlantilla(original).clonar();
        clon.getAreasDisponibles().add("QA"); // modificamos el clon

        // ASSERT — el original no se debe ver afectado
        assertEquals(1, original.getAreasDisponibles().size());
        assertEquals(2, clon.getAreasDisponibles().size());
    }
}
