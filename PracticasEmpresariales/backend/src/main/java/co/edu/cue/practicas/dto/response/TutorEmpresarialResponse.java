package co.edu.cue.practicas.dto.response;

import java.time.LocalDateTime;

public record TutorEmpresarialResponse(
        Long id, String nombre, String cargo, String correo, String telefono,
        Long empresaId, String razonSocialEmpresa,
        boolean disponible, boolean activo, LocalDateTime creadoEn
) {}
