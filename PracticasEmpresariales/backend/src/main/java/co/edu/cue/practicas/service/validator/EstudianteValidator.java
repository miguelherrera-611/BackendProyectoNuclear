package co.edu.cue.practicas.service.validator;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SOLID — SRP: validaciones de precondiciones externas del módulo de estudiantes.
 *
 * EstudianteService cambia si cambia el flujo de negocio.
 * EstudianteValidator cambia si cambian las reglas de validación de datos de entrada.
 */
@Component
@RequiredArgsConstructor
public class EstudianteValidator {

    private final UsuarioRepository usuarioRepository;

    /** OCL: identificacionUnica */
    public void validarIdentificacionUnica(String identificacion) {
        if (identificacion != null && usuarioRepository.existsByIdentificacion(identificacion))
            throw new OperacionNoPermitidaException(
                    "Ya existe un estudiante con la identificación: " + identificacion);
    }

    /** Verifica que el usuario sea un estudiante activo */
    public void validarEsEstudianteActivo(Usuario usuario) {
        if (!Rol.ESTUDIANTE.equals(usuario.getRol()))
            throw new OperacionNoPermitidaException(
                    "El usuario no tiene rol ESTUDIANTE.");
        if (!usuario.isActivo())
            throw new OperacionNoPermitidaException(
                    "El estudiante está inactivo y no puede gestionarse.");
    }

    /** OCL: soloAptoPostulable — solo un APTO puede enviarse al proceso */
    public void validarEsApto(Usuario estudiante) {
        if (EstadoEstudiante.APTO != estudiante.getEstadoEstudiante())
            throw new OperacionNoPermitidaException(
                    "Solo los estudiantes en estado APTO pueden enviarse al proceso de práctica. " +
                    "Estudiante '" + estudiante.getNombre() + "' está en estado " +
                    estudiante.getEstadoEstudiante() + ".");
    }

    /** OCL: transicionEstadoValida — solo NO_APTO puede pasar a APTO */
    public void validarTransicionApto(Usuario estudiante) {
        if (EstadoEstudiante.APTO.equals(estudiante.getEstadoEstudiante()))
            throw new OperacionNoPermitidaException(
                    "El estudiante ya está en estado APTO.");
    }
}
