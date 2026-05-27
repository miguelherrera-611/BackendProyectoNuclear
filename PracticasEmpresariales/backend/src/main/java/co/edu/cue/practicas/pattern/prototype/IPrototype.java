package co.edu.cue.practicas.pattern.prototype;

/**
 * PATRÓN PROTOTYPE — Interfaz IPrototype
 *
 * Del diagrama del documento:
 *   IPrototype → PracticeTemplate / PracticeInstance
 *
 * Aplicado en Dev 3 para clonar la configuración de una empresa
 * aprobada como plantilla al registrar una empresa similar.
 *
 * SOLID — DIP: los clientes dependen de esta interfaz, no de
 *              implementaciones concretas.
 */
public interface IPrototype<T> {
    T clonar();
}
