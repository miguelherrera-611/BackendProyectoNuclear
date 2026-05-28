package co.edu.cue.practicas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CrearEmpresaRequest(
        @NotBlank(message = "La razón social es obligatoria") String razonSocial,
        @NotBlank(message = "El NIT es obligatorio") String nit,
        String sector,
        String direccion,
        String municipio,
        String telefono,
        @NotBlank(message = "El nombre del contacto es obligatorio") String nombreContacto,
        @Email(message = "Correo inválido") String correo,
        List<String> areasDisponibles
) {}
