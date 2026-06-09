package co.edu.cue.practicas.service.evaluacion;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import org.springframework.stereotype.Component;

/**
 * SOLID — SRP: centraliza las reglas de negocio para habilitar el registro de la nota final.
 * Ninguna otra clase debe contener esta lógica inline.
 *
 * OCP: si las reglas cambian (mínimo de seguimientos, estados requeridos, etc.)
 * solo se modifica este componente, sin tocar los servicios que lo usan.
 */
@Component
public class ReglasNotaFinal {

    /** Mínimo de seguimientos en estado REVISADO exigidos antes de registrar la nota definitiva. */
    public static final int MINIMO_SEGUIMIENTOS_REVISADOS = 3;

    /**
     * Valida que la instancia tenga el mínimo de seguimientos revisados requerido.
     *
     * @param revisados cantidad de seguimientos en estado REVISADO de la instancia
     * @throws OperacionNoPermitidaException si no se cumple el mínimo
     */
    public void validarSeguimientosMinimos(long revisados) {
        if (revisados < MINIMO_SEGUIMIENTOS_REVISADOS) {
            throw new OperacionNoPermitidaException(
                    "Se requieren al menos " + MINIMO_SEGUIMIENTOS_REVISADOS +
                    " seguimientos en estado REVISADO para continuar. " +
                    "Actualmente hay " + revisados + ".");
        }
    }
}
