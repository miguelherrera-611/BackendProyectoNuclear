package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.EstadoCuenta;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.Rol;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private String fotoPerfil;
    private Rol rol;
    private EtiquetaCargo etiquetaCargo;
    private boolean activo;
    private boolean primerIngreso;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime creadoEn;
    private Long facultadId;
    private String facultadNombre;
    private Long programaId;
    private String programaNombre;
    private Long empresaId;
    private String razonSocialEmpresa;
    private EstadoEstudiante estadoEstudiante;
    private EstadoCuenta estadoCuenta;
    private String identificacion;
    private Integer semestre;
    private String contactoEmergencia;
    private boolean enviadoAlProceso;

    public static UsuarioResponse desde(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .correo(u.getCorreo())
                .telefono(u.getTelefono())
                .fotoPerfil(u.getFotoPerfil())
                .rol(u.getRol())
                .etiquetaCargo(u.getEtiquetaCargo())
                .activo(u.isActivo())
                .primerIngreso(u.isPrimerIngreso())
                .ultimoAcceso(u.getUltimoAcceso())
                .creadoEn(u.getCreadoEn())
                .facultadId(u.getFacultad() != null ? u.getFacultad().getId() : null)
                .facultadNombre(u.getFacultad() != null ? u.getFacultad().getNombre() : null)
                .programaId(u.getPrograma() != null ? u.getPrograma().getId() : null)
                .programaNombre(u.getPrograma() != null ? u.getPrograma().getNombre() : null)
                .empresaId(u.getEmpresa() != null ? u.getEmpresa().getId() : null)
                .razonSocialEmpresa(u.getEmpresa() != null ? u.getEmpresa().getRazonSocial() : null)
                .estadoEstudiante(u.getEstadoEstudiante())
                .estadoCuenta(u.getEstadoCuenta())
                .identificacion(u.getIdentificacion())
                .semestre(u.getSemestre())
                .contactoEmergencia(u.getContactoEmergencia())
                .enviadoAlProceso(u.isEnviadoAlProceso())
                .build();
    }
}
