package co.edu.cue.practicas.pattern.chain;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;

/**
 * PATRÓN CHAIN OF RESPONSIBILITY — Eslabón 2
 *
 * GPE-145: verifica que el estudiante tenga una Hoja de Vida en estado VALIDA.
 * OCL: hvValidaParaPostular — la HV debe estar Válida para poder marcar APTO.
 */
public class ValidadorHojaDeVidaValida extends ValidadorAptitud {

    @Override
    protected void ejecutarValidacion(ContextoValidacion ctx) {
        boolean hvValida = ctx.hvActual()
                .map(hv -> hv.esValida())
                .orElse(false);

        if (!hvValida)
            throw new OperacionNoPermitidaException(
                    "El estudiante no tiene una Hoja de Vida en estado VÁLIDA. " +
                    "Suba y valide la hoja de vida antes de marcar aptitud.");
    }
}
