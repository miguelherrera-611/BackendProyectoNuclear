package co.edu.cue.practicas.pattern.chain;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;

/**
 * PATRÓN CHAIN OF RESPONSIBILITY — Eslabón 1
 *
 * GPE-141 / GPE-145: verifica que el catálogo de prácticas esté configurado
 * y activo antes de permitir marcar al estudiante como APTO.
 *
 * Criterio de Aceptación: "Si el catálogo no está configurado el sistema
 * alerta antes de permitir marcar APTO."
 */
public class ValidadorCatalogoActivo extends ValidadorAptitud {

    @Override
    protected void ejecutarValidacion(ContextoValidacion ctx) {
        if (ctx.catalogo() == null)
            throw new OperacionNoPermitidaException(
                    "No existe un catálogo de prácticas configurado para el número de práctica " +
                    "del estudiante. Configure el catálogo antes de marcar aptitud.");

        if (!ctx.catalogo().isActivo())
            throw new OperacionNoPermitidaException(
                    "El catálogo de prácticas '" + ctx.catalogo().getNombre() +
                    "' está inactivo. Reactive el catálogo o contacte a la Coordinación Académica.");
    }
}
