package co.edu.cue.practicas.pattern.chain;

/**
 * PATRÓN CHAIN OF RESPONSIBILITY — Handler abstracto
 *
 * GPE-145: cadena secuencial de validación de aptitud.
 * Si un eslabón falla, lanza excepción y la cadena se detiene.
 * Si pasa, delega al siguiente eslabón.
 *
 * Cadena configurada en EstrategiaValidacionEstandar:
 *   ValidadorCatalogoActivo → ValidadorHojaDeVidaValida → ValidadorPracticaAnteriorFinalizada
 *
 * SOLID — OCP: para agregar un nuevo criterio de validación, solo se crea
 *              una nueva subclase y se inserta en la cadena. Nada más cambia.
 * SOLID — DIP: los clientes dependen de esta abstracción, no de validadores concretos.
 */
public abstract class ValidadorAptitud {

    private ValidadorAptitud siguiente;

    /** Encadena el siguiente validador */
    public ValidadorAptitud setSiguiente(ValidadorAptitud siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    /**
     * Valida el contexto. Si la validación falla, lanza OperacionNoPermitidaException.
     * Si pasa, delega al siguiente eslabón (si existe).
     */
    public final void validar(ContextoValidacion ctx) {
        ejecutarValidacion(ctx);
        if (siguiente != null) siguiente.validar(ctx);
    }

    /** Cada subclase implementa su criterio de validación específico */
    protected abstract void ejecutarValidacion(ContextoValidacion ctx);
}
