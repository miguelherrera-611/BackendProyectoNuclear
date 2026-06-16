package co.edu.cue.practicas.dto.request;

import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearUsuarioRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    /**
     * Obligatorio cuando rol = COORDINACION_ACADEMICA.
     * Ignorado para los demás roles.
     */
    private EtiquetaCargo etiquetaCargo;

    private String telefono;

    /** ID de facultad — requerido para COORDINADOR_PRACTICAS */
    private Long facultadId;

    /** ID de programa — requerido para COORDINACION_ACADEMICA y ESTUDIANTE */
    private Long programaId;

    // ── Campos exclusivos de ESTUDIANTE ──────────────────────────────────────

    /** OCL: identificacionUnica — requerido cuando rol = ESTUDIANTE */
    private String identificacion;

    /** Semestre actual — requerido cuando rol = ESTUDIANTE */
    private Integer semestre;

    /** Nombre y teléfono del contacto de emergencia (ej. "Ana Herrera - 3001234567") */
    private String contactoEmergencia;
}
