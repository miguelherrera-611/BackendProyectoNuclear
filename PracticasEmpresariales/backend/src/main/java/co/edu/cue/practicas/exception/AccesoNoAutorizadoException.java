package co.edu.cue.practicas.exception;

public class AccesoNoAutorizadoException extends RuntimeException {
    public AccesoNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
