package co.edu.cue.practicas.model.enums;

/**
 * Estado de la instancia de práctica empresarial de un estudiante.
 * El estado avanza en orden; nunca retrocede.
 *
 * ASIGNADA_PENDIENTE_INICIO → EN_CURSO → FINALIZADA
 *                                      ↘ CANCELADA
 */
public enum EstadoPractica {
    /** Creada automáticamente al marcar al estudiante como APTO — aún no inicia. */
    ASIGNADA_PENDIENTE_INICIO,
    EN_CURSO,
    FINALIZADA,
    CANCELADA
}
