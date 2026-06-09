package co.edu.cue.practicas.model.enums;

/**
 * GPE-168 / GPE-170 — Estado del seguimiento semanal del estudiante.
 * Flujo: ENVIADO (estudiante envía) → REVISADO (docente revisa) | RECHAZADO (docente rechaza).
 * RECHAZADO permite re-envío del estudiante → vuelve a ENVIADO.
 * PENDIENTE / APROBADO conservados para compatibilidad con registros anteriores.
 * OCL: soloUltimoEditable — solo el seguimiento más reciente puede re-editarse si RECHAZADO.
 */
public enum EstadoSeguimiento {
    /** Estado inicial cuando el estudiante envía el seguimiento. */
    ENVIADO,
    /** El docente asesor marcó el seguimiento como revisado (sin nota). */
    REVISADO,
    /** El docente rechazó el seguimiento; el estudiante puede re-editarlo. */
    RECHAZADO,
    /** Conservado para compatibilidad con registros anteriores a esta versión. */
    PENDIENTE,
    /** Conservado para compatibilidad con registros anteriores a esta versión. */
    APROBADO
}
