package co.edu.cue.practicas.pattern.strategy;

import co.edu.cue.practicas.pattern.chain.ValidadorAptitud;
import co.edu.cue.practicas.pattern.chain.ValidadorCatalogoActivo;
import co.edu.cue.practicas.pattern.chain.ValidadorHojaDeVidaValida;
import co.edu.cue.practicas.pattern.chain.ValidadorPracticaAnteriorFinalizada;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — Estrategia estándar de validación
 *
 * GPE-145: implementación por defecto para todos los programas.
 * Construye la cadena completa de validación en el orden reglamentario:
 *   1. Catálogo activo configurado
 *   2. Hoja de Vida en estado VÁLIDA
 *   3. Práctica anterior FINALIZADA (si aplica)
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
        ValidadorHojaDeVidaValida eslabonDos = new ValidadorHojaDeVidaValida();
        ValidadorPracticaAnteriorFinalizada eslabonTres = new ValidadorPracticaAnteriorFinalizada();

        eslabonUno.setSiguiente(eslabonDos);
        eslabonDos.setSiguiente(eslabonTres);

        return eslabonUno;
    }
}
