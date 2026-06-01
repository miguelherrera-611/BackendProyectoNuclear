package co.edu.cue.practicas.model.enums;

/**
 * GPE-168 / GPE-170 — Estado del seguimiento semanal del estudiante.
 * PATRON STATE: PENDIENTE es editable por docente; APROBADO/RECHAZADO son finales.
 * OCL: soloUltimoEditable — solo el seguimiento más reciente puede re-editarse si RECHAZADO.
 */
public enum EstadoSeguimiento {
    PENDIENTE,
    APROBADO,
    RECHAZADO
}
