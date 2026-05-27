package co.edu.cue.practicas.model.enums;

/**
 * Estado de activación de la cuenta de un usuario.
 *
 * PENDIENTE → creado por el DTI, correo con password temporal enviado,
 *             pero el usuario todavía no ha iniciado sesión ni una vez.
 * ACTIVO    → el usuario usó la password temporal y completó al menos
 *             un login exitoso.
 */
public enum EstadoCuenta {
    PENDIENTE,
    ACTIVO
}
