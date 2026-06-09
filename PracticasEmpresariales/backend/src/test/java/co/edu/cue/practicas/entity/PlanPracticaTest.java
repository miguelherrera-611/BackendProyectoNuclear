package co.edu.cue.practicas.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.PlanPractica;
import co.edu.cue.practicas.model.enums.EstadoPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * GPE-167 — Pruebas del PATRÓN STATE en PlanPractica.
 *
 * Flujo permitido:
 *   BORRADOR → APROBADO_TUTOR → APROBADO_DOCENTE
 *           ↘ RECHAZADO ←——————————————————————↗
 *   RECHAZADO → BORRADOR (via resubmit)
 */
@DisplayName("PlanPractica — Patrón State: transiciones de estado")
class PlanPracticaTest {

    private PlanPractica plan;

    @BeforeEach
    void setUp() {
        plan = PlanPractica.builder()
                .id(1L)
                .objetivos("Aplicar conocimientos en entorno real")
                .cronograma("Semana 1-4: inducción; Semana 5-16: desarrollo")
                .cargadoPorId(10L)
                .build();
    }

    // =================================================================
    // Estado inicial
    // =================================================================

    @Test
    @DisplayName("Estado inicial debe ser BORRADOR")
    void estadoInicialDebeSerBorrador() {
        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.BORRADOR);
        assertThat(plan.estaAprobadoParaSeguimiento()).isFalse();
    }

    // =================================================================
    // aprobarPorTutor() — BORRADOR → APROBADO_TUTOR
    // =================================================================

    @Test
    @DisplayName("aprobarPorTutor() desde BORRADOR debe pasar a APROBADO_TUTOR")
    void aprobarPorTutorDesdeBorradorExitoso() {
        plan.aprobarPorTutor();

        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.APROBADO_TUTOR);
        assertThat(plan.getAprobadoPorTutorEn()).isNotNull();
        assertThat(plan.getMotivoRechazo()).isNull();
    }

    @Test
    @DisplayName("aprobarPorTutor() desde RECHAZADO debe permitirse (re-aproba después de rechazo)")
    void aprobarPorTutorDesdeRechazadoEsPermitido() {
        plan.rechazar("No cumple objetivos", 99L);
        plan.aprobarPorTutor();

        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.APROBADO_TUTOR);
    }

    @Test
    @DisplayName("aprobarPorTutor() desde APROBADO_TUTOR debe lanzar excepción — transición inválida")
    void aprobarPorTutorDesdeAprobadoTutorLanzaExcepcion() {
        plan.aprobarPorTutor();

        assertThatThrownBy(plan::aprobarPorTutor)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("aprobarPorTutor() desde APROBADO_DOCENTE debe lanzar excepción — ya finalizado")
    void aprobarPorTutorDesdeAprobadoDocenteLanzaExcepcion() {
        plan.aprobarPorTutor();
        plan.aprobarPorDocente();

        assertThatThrownBy(plan::aprobarPorTutor)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // aprobarPorDocente() — APROBADO_TUTOR → APROBADO_DOCENTE
    // =================================================================

    @Test
    @DisplayName("aprobarPorDocente() desde APROBADO_TUTOR debe pasar a APROBADO_DOCENTE")
    void aprobarPorDocenteDesdeAprobadoTutorExitoso() {
        plan.aprobarPorTutor();
        plan.aprobarPorDocente();

        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.APROBADO_DOCENTE);
        assertThat(plan.getAprobadoPorDocenteEn()).isNotNull();
        assertThat(plan.estaAprobadoParaSeguimiento()).isTrue();
    }

    @Test
    @DisplayName("aprobarPorDocente() desde BORRADOR debe pasar a APROBADO_DOCENTE directamente")
    void aprobarPorDocenteDesdeBorradorExitoso() {
        plan.aprobarPorDocente();

        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.APROBADO_DOCENTE);
        assertThat(plan.getAprobadoPorDocenteEn()).isNotNull();
        assertThat(plan.estaAprobadoParaSeguimiento()).isTrue();
    }

    @Test
    @DisplayName("aprobarPorDocente() desde RECHAZADO debe lanzar excepción — el plan debe re-enviarse primero")
    void aprobarPorDocenteDesdeRechazadoLanzaExcepcion() {
        plan.rechazar("Motivo", 99L);

        assertThatThrownBy(plan::aprobarPorDocente)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // rechazar() — cualquier estado excepto APROBADO_DOCENTE
    // =================================================================

    @Test
    @DisplayName("rechazar() desde BORRADOR debe cambiar a RECHAZADO con motivo")
    void rechazarDesdeBorradorExitoso() {
        plan.rechazar("Objetivos muy vagos", 5L);

        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.RECHAZADO);
        assertThat(plan.getMotivoRechazo()).isEqualTo("Objetivos muy vagos");
        assertThat(plan.getRechazadoPorId()).isEqualTo(5L);
        assertThat(plan.estaAprobadoParaSeguimiento()).isFalse();
    }

    @Test
    @DisplayName("rechazar() desde APROBADO_TUTOR debe funcionar — docente puede rechazar")
    void rechazarDesdeAprobadoTutorExitoso() {
        plan.aprobarPorTutor();
        plan.rechazar("Cronograma incompleto", 7L);

        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.RECHAZADO);
    }

    @Test
    @DisplayName("rechazar() desde APROBADO_DOCENTE debe lanzar excepción — plan ya finalizado")
    void rechazarDesdeAprobadoDocenteLanzaExcepcion() {
        plan.aprobarPorTutor();
        plan.aprobarPorDocente();

        assertThatThrownBy(() -> plan.rechazar("Motivo tardío", 7L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("aprobado");
    }

    // =================================================================
    // resubmit() — RECHAZADO → BORRADOR
    // =================================================================

    @Test
    @DisplayName("resubmit() desde RECHAZADO debe volver a BORRADOR y limpiar motivo")
    void resubmitDesdeRechazadoExitoso() {
        plan.rechazar("No cumple objetivos", 99L);
        plan.resubmit();

        assertThat(plan.getEstado()).isEqualTo(EstadoPlan.BORRADOR);
        assertThat(plan.getMotivoRechazo()).isNull();
        assertThat(plan.getRechazadoPorId()).isNull();
    }

    @Test
    @DisplayName("resubmit() desde BORRADOR debe lanzar excepción — solo aplica a RECHAZADO")
    void resubmitDesdeBorradorLanzaExcepcion() {
        assertThatThrownBy(plan::resubmit)
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("RECHAZADO");
    }

    @Test
    @DisplayName("resubmit() desde APROBADO_TUTOR debe lanzar excepción")
    void resubmitDesdeAprobadoTutorLanzaExcepcion() {
        plan.aprobarPorTutor();

        assertThatThrownBy(plan::resubmit)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // estaAprobadoParaSeguimiento()
    // =================================================================

    @Test
    @DisplayName("estaAprobadoParaSeguimiento() solo es true cuando APROBADO_DOCENTE")
    void estaAprobadoParaSeguimientoSoloEnAprobadoDocente() {
        assertThat(plan.estaAprobadoParaSeguimiento()).isFalse(); // BORRADOR

        plan.aprobarPorTutor();
        assertThat(plan.estaAprobadoParaSeguimiento()).isFalse(); // APROBADO_TUTOR

        plan.aprobarPorDocente();
        assertThat(plan.estaAprobadoParaSeguimiento()).isTrue(); // APROBADO_DOCENTE
    }
}
