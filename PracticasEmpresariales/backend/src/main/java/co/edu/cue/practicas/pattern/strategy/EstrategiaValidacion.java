package co.edu.cue.practicas.pattern.strategy;

import co.edu.cue.practicas.pattern.chain.ValidadorAptitud;

/**
 * PATRÓN STRATEGY — Interfaz de estrategia de validación de aptitud
 *
 * GPE-145: cada programa puede tener su propia estrategia de validación
 * con reglas distintas. La interfaz permite que EstudianteService trabaje
 * con cualquier estrategia sin conocer su implementación concreta.
 *
 * SOLID — OCP: para agregar una nueva estrategia por programa (ej. validación
 *              especial para Ingeniería de Sistemas), solo se crea una nueva
 *              implementación. EstudianteService no se modifica.
 * SOLID — DIP: EstudianteService depende de esta interfaz, nunca de EstrategiaValidacionEstandar.
 */
public interface EstrategiaValidacion {
    /**
     * Construye y retorna la cabeza de la cadena de validadores.
     * Cada implementación puede cambiar qué validadores incluye y en qué orden.
     */
    ValidadorAptitud construirCadena();
}
