package co.edu.cue.practicas.model.enums;

/**
 * GPE-167 — Estados del plan de práctica.
 * PATRON STATE: cada estado encapsula sus transiciones permitidas.
 *
 * BORRADOR → APROBADO_TUTOR → APROBADO_DOCENTE
 *          ↘ RECHAZADO ←———————————————————↗
 */
public enum EstadoPlan {
    BORRADOR,
    APROBADO_TUTOR,
    APROBADO_DOCENTE,
    RECHAZADO
}
