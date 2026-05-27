package co.edu.cue.practicas.service.validator;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.repository.TutorEmpresarialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SOLID — SRP: validaciones de precondiciones externas de TutorEmpresarial.
 */
@Component
@RequiredArgsConstructor
public class TutorEmpresarialValidator {

    private final TutorEmpresarialRepository tutorRepository;

    public void validarCorreoUnico(String correo) {
        if (tutorRepository.existsByCorreo(correo))
            throw new OperacionNoPermitidaException(
                    "Ya existe un tutor registrado con el correo: " + correo);
    }
}
