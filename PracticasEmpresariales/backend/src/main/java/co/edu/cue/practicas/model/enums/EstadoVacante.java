package co.edu.cue.practicas.model.enums;

/**
 * GPE-152/153 — Estados del ciclo de vida de una Vacante.
 *
 * PATRÓN STATE: cada transición está encapsulada en la entidad.
 *   PENDIENTE  → DISPONIBLE  (aprobar)
 *   PENDIENTE  → RECHAZADA   (rechazar)
 *   DISPONIBLE → CERRADA     (cerrar / cupos llenos)
 */
public enum EstadoVacante {
    PENDIENTE,
    DISPONIBLE,
    RECHAZADA,
    CERRADA
}
