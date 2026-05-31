package co.edu.cue.practicas.service.dashboard;

import co.edu.cue.practicas.dto.response.DashboardResponse;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias del DashboardMediator.
 *
 * No usa Spring ni mocks — el Mediator es puro Java.
 * Verifica que cada rol recibe el panel correcto con las secciones y permisos esperados.
 *
 * Cómo ejecutar en IntelliJ:
 *   Clic derecho sobre la clase → Run 'DashboardMediatorTest'
 */
@DisplayName("DashboardMediator — Pruebas del patrón Mediator")
class DashboardMediatorTest {

    // Instancia real del mediador — no necesita mocks porque es lógica pura
    private DashboardMediator mediator;

    @BeforeEach
    void setUp() {
        mediator = new DashboardMediator();
    }

    /**
     * Helper: crea un CustomUserDetails mínimo con el rol indicado.
     * Los demás campos no son relevantes para estas pruebas.
     */
    private CustomUserDetails udConRol(Rol rol) {
        return new CustomUserDetails(Usuario.builder()
                .id(1L)
                .nombre("Usuario Test")
                .correo("test@test.com")
                .passwordHash("hash")
                .rol(rol)
                .activo(true)
                .build());
    }

    // =================================================================
    // TESTS DE CONTENIDO DEL PANEL POR ROL
    // =================================================================

    @Test
    @DisplayName("ADMIN_DTI debe recibir panel de administración con permisos de escritura")
    void dtiDebeRecibirPanelAdministracion() {
        DashboardResponse r = mediator.resolverDashboard(udConRol(Rol.ADMIN_DTI));

        assertThat(r.getTitulo()).isEqualTo("Panel Administrador DTI");
        assertThat(r.isSoloLectura()).isFalse();
        assertThat(r.getSecciones()).hasSize(7);
    }

    @Test
    @DisplayName("DIRECCION debe recibir panel de solo lectura")
    void direccionDebeRecibirPanelSoloLectura() {
        DashboardResponse r = mediator.resolverDashboard(udConRol(Rol.DIRECCION));

        // DIRECCION es el único rol que recibe soloLectura=true
        assertThat(r.getTitulo()).contains("Solo Lectura");
        assertThat(r.isSoloLectura()).isTrue();
    }

    @Test
    @DisplayName("ESTUDIANTE debe recibir su panel personal con 3 secciones")
    void estudianteDebeRecibirMiPanel() {
        DashboardResponse r = mediator.resolverDashboard(udConRol(Rol.ESTUDIANTE));

        assertThat(r.getTitulo()).isEqualTo("Mi Panel");
        assertThat(r.isSoloLectura()).isFalse();
        assertThat(r.getSecciones()).hasSize(3);
    }

    @Test
    @DisplayName("COORDINACION_ACADEMICA debe incluir la etiqueta de cargo en el panel")
    void coordinacionDebeIncluirEtiquetaCargo() {
        Usuario usuario = Usuario.builder()
                .id(2L)
                .nombre("Coordinadora Test")
                .correo("coord@test.com")
                .passwordHash("hash")
                .rol(Rol.COORDINACION_ACADEMICA)
                .etiquetaCargo(EtiquetaCargo.COORDINACION_ACADEMICA)
                .activo(true)
                .build();

        DashboardResponse r = mediator.resolverDashboard(new CustomUserDetails(usuario));

        assertThat(r.getTitulo()).isEqualTo("Panel Coordinación Académica");
        assertThat(r.getEtiquetaCargo()).isEqualTo(EtiquetaCargo.COORDINACION_ACADEMICA);
    }

    @Test
    @DisplayName("El dashboard incluye siempre el rol y el nombre del usuario autenticado")
    void dashboardIncludeRolYNombreUsuario() {
        DashboardResponse r = mediator.resolverDashboard(udConRol(Rol.ADMIN_DTI));

        assertThat(r.getRol()).isEqualTo(Rol.ADMIN_DTI);
        assertThat(r.getNombreUsuario()).isEqualTo("Usuario Test");
    }

    @Test
    @DisplayName("Cada rol recibe el rol correcto en su panel")
    void cadaRolTieneElRolCorrecto() {
        // Verifica que el mediador no mezcla los paneles entre roles
        for (Rol rol : Rol.values()) {
            DashboardResponse r = mediator.resolverDashboard(udConRol(rol));
            assertThat(r.getRol())
                    .as("El panel del rol %s debe tener ese rol en su respuesta", rol)
                    .isEqualTo(rol);
        }
    }

    @Test
    @DisplayName("El mediador debe reflejar los indicadores reales enviados por el servicio")
    void mediadorDebeReflejarIndicadoresReales() {
        DashboardIndicadores indicadores = DashboardIndicadores.builder()
                .usuariosActivosAdminDti(2)
                .estudiantesApto(5)
                .build();

        DashboardResponse r = mediator.resolverDashboard(udConRol(Rol.ADMIN_DTI), indicadores);

        assertThat(r.getSecciones())
                .extracting(seccion -> ((Number) seccion.get("contador")).longValue())
                .contains(2L, 5L);
    }
}
