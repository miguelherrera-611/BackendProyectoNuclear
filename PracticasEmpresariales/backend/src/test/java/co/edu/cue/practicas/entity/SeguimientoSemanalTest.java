package co.edu.cue.practicas.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.SeguimientoSemanal;
import co.edu.cue.practicas.model.enums.EstadoSeguimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * GPE-168 / GPE-170 — Pruebas del PATRÓN STATE en SeguimientoSemanal.
 *
 * Flujo permitido:
 *   PENDIENTE → APROBADO  (docente aprueba)
 *   PENDIENTE → RECHAZADO (docente rechaza con observación)
 *   RECHAZADO → PENDIENTE (estudiante re-envía via resubmit)
 *
 * OCL: soloUltimoEditable — solo el seguimiento más reciente puede re-editarse si RECHAZADO.
 */
@DisplayName("SeguimientoSemanal — Patrón State: transiciones de estado")
class SeguimientoSemanalTest {

    private SeguimientoSemanal seguimiento;

    @BeforeEach
    void setUp() {
        seguimiento = SeguimientoSemanal.builder()
                .id(1L)
                .semana(1)
                .actividades("Revisión de código fuente y análisis de requerimientos")
                .logros("Comprensión del sistema existente")
                .dificultades("Documentación desactualizada")
                .creadoPorId(10L)
                .build();
    }

    // =================================================================
    // Estado inicial
    // =================================================================

    @Test
    @DisplayName("Estado inicial debe ser PENDIENTE y no ser editable")
    void estadoInicialDebeSer_PENDIENTE() {
        assertThat(seguimiento.getEstado()).isEqualTo(EstadoSeguimiento.PENDIENTE);
        assertThat(seguimiento.esEditable()).isFalse();
    }

    // =================================================================
    // aprobar() — PENDIENTE → APROBADO
    // =================================================================

    @Test
    @DisplayName("aprobar() desde PENDIENTE debe cambiar a APROBADO y registrar docente")
    void aprobarDesdePendienteExitoso() {
        Long docenteId = 20L;
        seguimiento.aprobar(docenteId);

        assertThat(seguimiento.getEstado()).isEqualTo(EstadoSeguimiento.APROBADO);
        assertThat(seguimiento.getRevisadoPorId()).isEqualTo(docenteId);
        assertThat(seguimiento.getRevisadoEn()).isNotNull();
        assertThat(seguimiento.esEditable()).isFalse();
    }

    @Test
    @DisplayName("aprobar() desde APROBADO debe lanzar excepción — ya revisado")
    void aprobarDesdeAprobadoLanzaExcepcion() {
        seguimiento.aprobar(20L);

        assertThatThrownBy(() -> seguimiento.aprobar(20L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    @DisplayName("aprobar() desde RECHAZADO debe lanzar excepción")
    void aprobarDesdeRechazadoLanzaExcepcion() {
        seguimiento.rechazar("Incompleto", 20L);

        assertThatThrownBy(() -> seguimiento.aprobar(20L))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // rechazar() — PENDIENTE → RECHAZADO
    // =================================================================

    @Test
    @DisplayName("rechazar() desde PENDIENTE debe cambiar a RECHAZADO con observaciones")
    void rechazarDesdePendienteExitoso() {
        Long docenteId = 20L;
        String obs = "Falta describir los logros cuantitativamente";
        seguimiento.rechazar(obs, docenteId);

        assertThat(seguimiento.getEstado()).isEqualTo(EstadoSeguimiento.RECHAZADO);
        assertThat(seguimiento.getObservacionesDocente()).isEqualTo(obs);
        assertThat(seguimiento.getRevisadoPorId()).isEqualTo(docenteId);
        assertThat(seguimiento.getRevisadoEn()).isNotNull();
        assertThat(seguimiento.esEditable()).isTrue();
    }

    @Test
    @DisplayName("rechazar() desde APROBADO debe lanzar excepción — ya revisado")
    void rechazarDesdeAprobadoLanzaExcepcion() {
        seguimiento.aprobar(20L);

        assertThatThrownBy(() -> seguimiento.rechazar("Observación", 20L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    @DisplayName("rechazar() desde RECHAZADO debe lanzar excepción")
    void rechazarDesdeRechazadoLanzaExcepcion() {
        seguimiento.rechazar("Primera observación", 20L);

        assertThatThrownBy(() -> seguimiento.rechazar("Segunda observación", 20L))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // resubmit() — RECHAZADO → PENDIENTE
    // =================================================================

    @Test
    @DisplayName("resubmit() desde RECHAZADO debe volver a PENDIENTE y limpiar revisor")
    void resubmitDesdeRechazadoExitoso() {
        seguimiento.rechazar("Falta información", 20L);
        seguimiento.resubmit();

        assertThat(seguimiento.getEstado()).isEqualTo(EstadoSeguimiento.PENDIENTE);
        assertThat(seguimiento.getRevisadoPorId()).isNull();
        assertThat(seguimiento.getRevisadoEn()).isNull();
        assertThat(seguimiento.esEditable()).isFalse();
    }

    @Test
    @DisplayName("resubmit() desde PENDIENTE debe lanzar excepción — solo aplica a RECHAZADO")
    void resubmitDesdePendienteLanzaExcepcion() {
        assertThatThrownBy(seguimiento::resubmit)
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("RECHAZADO");
    }

    @Test
    @DisplayName("resubmit() desde APROBADO debe lanzar excepción — seguimiento inmutable")
    void resubmitDesdeAprobadoLanzaExcepcion() {
        seguimiento.aprobar(20L);

        assertThatThrownBy(seguimiento::resubmit)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // esEditable() — OCL: soloUltimoEditable (la entidad gestiona su propio estado)
    // =================================================================

    @Test
    @DisplayName("esEditable() solo es true cuando el seguimiento está RECHAZADO")
    void esEditableSoloCuandoRechazado() {
        assertThat(seguimiento.esEditable()).isFalse(); // PENDIENTE

        seguimiento.aprobar(20L);
        assertThat(seguimiento.esEditable()).isFalse(); // APROBADO

        // Necesitamos un seguimiento nuevo en RECHAZADO
        SeguimientoSemanal rechazado = SeguimientoSemanal.builder().semana(2).build();
        rechazado.rechazar("Incompleto", 20L);
        assertThat(rechazado.esEditable()).isTrue(); // RECHAZADO
    }
}
