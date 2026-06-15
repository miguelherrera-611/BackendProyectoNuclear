package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConfirmarCambioCorreoRequest(

    @NotBlank(message = "El código de verificación es obligatorio")
    String codigo,

    @NotBlank(message = "El nuevo correo es obligatorio")
    @Email(message = "El nuevo correo no tiene un formato válido")
    String nuevoCorreo
) {}
