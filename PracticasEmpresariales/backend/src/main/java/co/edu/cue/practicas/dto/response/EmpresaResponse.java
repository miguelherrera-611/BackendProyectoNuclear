package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import java.time.LocalDateTime;
import java.util.List;

public record EmpresaResponse(
        Long id, String razonSocial, String nit, String sector,
        String direccion, String municipio, String telefono,
        String nombreContacto, String correo, EstadoEmpresa estado,
        List<String> areasDisponibles, LocalDateTime creadoEn
) {}
