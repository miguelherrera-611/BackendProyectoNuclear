package co.edu.cue.practicas.audit;

/**
 * Constantes con los nombres de los módulos del sistema usados en la bitácora de auditoría.
 *
 * Centralizar estos valores evita escribir strings sueltos ("AUTH", "USUARIOS", etc.)
 * en cada servicio, lo que podría generar inconsistencias si alguien escribe el nombre
 * diferente (ej. "USUARIO" en lugar de "USUARIOS"). El compilador detecta el error
 * si se usa una constante que no existe.
 *
 * Se usa en todos los servicios al construir registros de BitacoraAuditoria:
 *   .modulo(ModuloAuditoria.AUTH)
 *   .modulo(ModuloAuditoria.USUARIOS)
 *   etc.
 */
public final class ModuloAuditoria {

    // Constructor privado para evitar instanciar esta clase; solo tiene constantes estáticas
    private ModuloAuditoria() {}

    /** Módulo de autenticación: login y cambio de contraseña */
    public static final String AUTH       = "AUTH";

    /** Módulo de gestión de usuarios: crear, editar, activar, desactivar */
    public static final String USUARIOS   = "USUARIOS";

    /** Módulo de gestión de facultades: crear, editar, desactivar */
    public static final String FACULTADES = "FACULTADES";

    /** Módulo de gestión de programas académicos: crear, desactivar */
    public static final String PROGRAMAS  = "PROGRAMAS";

    /** Módulo de gestión de estudiantes: validación de aptitud, envío al proceso */
    public static final String ESTUDIANTES = "ESTUDIANTES";

    /** Módulo de catálogo de prácticas: crear, desactivar entradas del catálogo */
    public static final String CATALOGO   = "CATALOGO";

    /** Módulo de expediente: hoja de vida, instancias de práctica */
    public static final String EXPEDIENTE = "EXPEDIENTE";
}
