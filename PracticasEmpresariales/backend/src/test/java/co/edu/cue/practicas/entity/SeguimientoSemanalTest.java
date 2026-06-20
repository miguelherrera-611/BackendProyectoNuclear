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
 * Flujo principal:
 *   ENVIADO  → REVISADO  (docente marca revisado, sin nota)
 *   ENVIADO  → RECHAZADO (docente rechaza con observación)
 *   RECHAZADO → ENVIADO  (estudiante re-envía via resubmit)
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
    @DisplayName("Estado inicial debe ser ENVIADO y no ser editable")
    void estadoInicialDebeSer_ENVIADO() {
        assertThat(seguimiento.getEstado()).isEqualTo(EstadoSeguimiento.ENVIADO);
        assertThat(seguimiento.esEditable()).isFalse();
    }

    // =================================================================
    // revisar() — ENVIADO → REVISADO
    // =================================================================

    @Test
    @DisplayName("revisar() desde ENVIADO debe cambiar a REVISADO y registrar docente")
    void revisarDesdeEnviadoExitoso() {
        Long docenteId = 20L;
        seguimiento.revisar(docenteId);

        assertThat(seguimiento.getEstado()).isEqualTo(EstadoSeguimiento.REVISADO);
        assertThat(seguimiento.getRevisadoPorId()).isEqualTo(docenteId);
        assertThat(seguimiento.getRevisadoEn()).isNotNull();
        assertThat(seguimiento.esEditable()).isFalse();
    }

    @Test
    @DisplayName("revisar() desde REVISADO debe lanzar excepción — ya revisado")
    void revisarDesdeRevisadoLanzaExcepcion() {
        seguimiento.revisar(20L);

        assertThatThrownBy(() -> seguimiento.revisar(20L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ENVIADO");
    }

    @Test
    @DisplayName("revisar() desde RECHAZADO debe lanzar excepción")
    void revisarDesdeRechazadoLanzaExcepcion() {
        seguimiento.rechazar("Incompleto", 20L);

        assertThatThrownBy(() -> seguimiento.revisar(20L))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // rechazar() — ENVIADO → RECHAZADO
    // =================================================================

    @Test
    @DisplayName("rechazar() desde ENVIADO debe cambiar a RECHAZADO con observaciones")
    void rechazarDesdeEnviadoExitoso() {
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
    @DisplayName("rechazar() desde REVISADO debe lanzar excepción — ya revisado")
    void rechazarDesdeRevisadoLanzaExcepcion() {
        seguimiento.revisar(20L);

        assertThatThrownBy(() -> seguimiento.rechazar("Observación", 20L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ENVIADO");
    }

    @Test
    @DisplayName("rechazar() desde RECHAZADO debe lanzar excepción")
    void rechazarDesdeRechazadoLanzaExcepcion() {
        seguimiento.rechazar("Primera observación", 20L);

        assertThatThrownBy(() -> seguimiento.rechazar("Segunda observación", 20L))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // resubmit() — RECHAZADO → ENVIADO
    // =================================================================

    @Test
    @DisplayName("resubmit() desde RECHAZADO debe volver a ENVIADO y limpiar revisor")
    void resubmitDesdeRechazadoExitoso() {
        seguimiento.rechazar("Falta información", 20L);
        seguimiento.resubmit();

        assertThat(seguimiento.getEstado()).isEqualTo(EstadoSeguimiento.ENVIADO);
        assertThat(seguimiento.getRevisadoPorId()).isNull();
        assertThat(seguimiento.getRevisadoEn()).isNull();
        assertThat(seguimiento.esEditable()).isFalse();
    }

    @Test
    @DisplayName("resubmit() desde ENVIADO debe lanzar excepción — solo aplica a RECHAZADO")
    void resubmitDesdeEnviadoLanzaExcepcion() {
        assertThatThrownBy(seguimiento::resubmit)
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("RECHAZADO");
    }

    @Test
    @DisplayName("resubmit() desde REVISADO debe lanzar excepción — seguimiento inmutable")
    void resubmitDesdeRevisadoLanzaExcepcion() {
        seguimiento.revisar(20L);

        assertThatThrownBy(seguimiento::resubmit)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // esEditable() — OCL: soloUltimoEditable (la entidad gestiona su propio estado)
    // =================================================================

    @Test
    @DisplayName("esEditable() solo es true cuando el seguimiento está RECHAZADO")
    void esEditableSoloCuandoRechazado() {
        assertThat(seguimiento.esEditable()).isFalse(); // ENVIADO

        seguimiento.revisar(20L);
        assertThat(seguimiento.esEditable()).isFalse(); // REVISADO

        SeguimientoSemanal rechazado = SeguimientoSemanal.builder().semana(2).build();
        rechazado.rechazar("Incompleto", 20L);
        assertThat(rechazado.esEditable()).isTrue(); // RECHAZADO
    }

}
