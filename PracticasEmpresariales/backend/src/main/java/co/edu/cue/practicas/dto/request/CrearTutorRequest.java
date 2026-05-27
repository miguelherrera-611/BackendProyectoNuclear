package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearTutorRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String cargo,
        @Email(message = "Correo inválido") @NotBlank(message = "El correo es obligatorio") String correo,
        String telefono,
        @NotNull(message = "El ID de empresa es obligatorio") Long empresaId
) {}
