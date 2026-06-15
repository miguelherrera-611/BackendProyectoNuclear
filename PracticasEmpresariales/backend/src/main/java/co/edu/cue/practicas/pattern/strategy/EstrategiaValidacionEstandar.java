package co.edu.cue.practicas.pattern.strategy;

import co.edu.cue.practicas.pattern.chain.ValidadorAptitud;
import co.edu.cue.practicas.pattern.chain.ValidadorCatalogoActivo;
import co.edu.cue.practicas.pattern.chain.ValidadorPracticaAnteriorFinalizada;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — Estrategia estándar de validación
 *
 * GPE-145: implementación por defecto para todos los programas.
 * Construye la cadena de validación en el orden reglamentario:
 *   1. Catálogo activo configurado
 *   2. Práctica anterior FINALIZADA (si aplica)
 *
 * La validación de Hoja de Vida fue eliminada de la cadena:
 * la Coordinación Académica puede marcar APTO independientemente
 * del estado de la HV del estudiante.
 *
 * Para agregar un programa con reglas distintas: crear nueva @Component
 * que implemente EstrategiaValidacion con su propia cadena.
 *
 * SOLID — OCP: nueva estrategia → nueva clase, sin tocar esta.
 * SOLID — SRP: solo construye la cadena estándar.
 */
@Component
public class EstrategiaValidacionEstandar implements EstrategiaValidacion {

    @Override
    public ValidadorAptitud construirCadena() {
        ValidadorCatalogoActivo eslabonUno = new ValidadorCatalogoActivo();
        ValidadorPracticaAnteriorFinalizada eslabonDos = new ValidadorPracticaAnteriorFinalizada();

        eslabonUno.setSiguiente(eslabonDos);

        return eslabonUno;
    }
}
