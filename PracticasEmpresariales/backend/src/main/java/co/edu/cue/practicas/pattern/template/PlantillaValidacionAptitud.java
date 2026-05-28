package co.edu.cue.practicas.pattern.template;

import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.pattern.chain.ContextoValidacion;
import co.edu.cue.practicas.pattern.strategy.EstrategiaValidacion;

import java.util.Optional;

/**
 * PATRÓN TEMPLATE METHOD — PlantillaValidacionAptitud
 *
 * GPE-145: define el esqueleto del proceso de validación de aptitud.
 * El flujo general es siempre el mismo; solo los valores y reglas específicas
 * varían según el programa (configurado vía Strategy).
 *
 * FLUJO FIJO (Template Method):
 *   1. construirContexto()     — reúne todos los datos del estudiante
 *   2. ejecutarValidacion()    — delega a la Strategy (Chain of Responsibility)
 *   3. onAptoConfirmado()      — hook: crea la instancia de práctica (Prototype)
 *
 * SOLID — OCP: para cambiar el flujo solo se sobreescriben los hooks.
 *              Para cambiar las reglas se cambia la EstrategiaValidacion.
 * SOLID — DIP: depende de EstrategiaValidacion (interfaz), nunca de la implementación.
 */
public abstract class PlantillaValidacionAptitud {

    private final EstrategiaValidacion estrategia;

    protected PlantillaValidacionAptitud(EstrategiaValidacion estrategia) {
        this.estrategia = estrategia;
    }

    /**
     * TEMPLATE METHOD: flujo fijo de validación y marcación de aptitud.
     * Los subclases no pueden cambiar este orden.
     */
    public final void ejecutar(Usuario estudiante,
                                CatalogoPractica catalogo,
                                Optional<HojaDeVida> hvActual,
                                Optional<InstanciaPractica> practicaAnterior) {
        ContextoValidacion ctx = construirContexto(estudiante, catalogo, hvActual, practicaAnterior);
        ejecutarValidacion(ctx);
        onAptoConfirmado(ctx);
    }

    /** Paso 1 — concreto: reúne los datos en el contexto de validación */
    private ContextoValidacion construirContexto(Usuario estudiante,
                                                  CatalogoPractica catalogo,
                                                  Optional<HojaDeVida> hvActual,
                                                  Optional<InstanciaPractica> practicaAnterior) {
        return new ContextoValidacion(estudiante, catalogo, hvActual, practicaAnterior);
    }

    /** Paso 2 — concreto: ejecuta la cadena de validación de la Strategy */
    private void ejecutarValidacion(ContextoValidacion ctx) {
        estrategia.construirCadena().validar(ctx);
    }

    /**
     * Paso 3 — HOOK: acción posterior a la validación exitosa.
     * Subclases implementan qué hacer cuando el estudiante cumple todos los requisitos
     * (ej. clonar el catálogo, crear la InstanciaPractica, actualizar el estado).
     */
    protected abstract void onAptoConfirmado(ContextoValidacion ctx);
}
