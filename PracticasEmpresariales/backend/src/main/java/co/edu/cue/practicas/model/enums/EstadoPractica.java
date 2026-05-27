package co.edu.cue.practicas.model.enums;

/**
 * Estado de la práctica empresarial.
 * El estado solo avanza: EN_CURSO → FINALIZADA → CANCELADA. Nunca retrocede.
 */
public enum EstadoPractica {
    EN_CURSO,
    FINALIZADA,
    CANCELADA
}
