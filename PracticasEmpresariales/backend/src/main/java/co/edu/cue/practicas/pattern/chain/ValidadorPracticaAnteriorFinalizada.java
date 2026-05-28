package co.edu.cue.practicas.pattern.chain;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoPractica;

/**
 * PATRÓN CHAIN OF RESPONSIBILITY — Eslabón 3
 *
 * GPE-145: si la práctica es mayor que la 1, verifica que la práctica anterior
 * esté en estado FINALIZADA antes de permitir iniciar la siguiente.
 * OCL: transicionEstadoValida — no se puede marcar APTO para Práctica N
 *      si la Práctica N-1 no está FINALIZADA.
 */
public class ValidadorPracticaAnteriorFinalizada extends ValidadorAptitud {

    @Override
    protected void ejecutarValidacion(ContextoValidacion ctx) {
        int numeroPractica = ctx.catalogo().getNumeroPractica();
        if (numeroPractica <= 1) return; // la primera práctica no tiene precondición

        boolean anteriorFinalizada = ctx.practicaAnterior()
                .map(p -> p.getEstado() == EstadoPractica.FINALIZADA)
                .orElse(false);

        if (!anteriorFinalizada)
            throw new OperacionNoPermitidaException(
                    "No se puede marcar APTO para la Práctica " + numeroPractica +
                    " porque la Práctica " + (numeroPractica - 1) + " no está FINALIZADA. " +
                    "Finalice la práctica anterior antes de continuar.");
    }
}
