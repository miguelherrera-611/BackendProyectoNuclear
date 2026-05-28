package co.edu.cue.practicas.model.enums;

/**
 * GPE-150 — Estados del ciclo de vida de una Empresa.
 *
 * Transiciones válidas (OCL: estadoValido):
 *   PENDIENTE → APROBADA
 *   PENDIENTE → RECHAZADA
 *   APROBADA  → INACTIVA
 *   RECHAZADA → INACTIVA
 */
public enum EstadoEmpresa {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    INACTIVA
}
