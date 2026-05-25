package co.edu.cue.practicas.service.facultad;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearFacultadRequest;
import co.edu.cue.practicas.dto.response.FacultadResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.facultad.FacultadRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacultadService {

    private final FacultadRepository facultadRepository;
    private final AuditoriaLogger auditoriaLogger;

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public FacultadResponse crearFacultad(CrearFacultadRequest request, CustomUserDetails creador) {
        if (facultadRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new OperacionNoPermitidaException("Ya existe una facultad con ese nombre.");
        }

        Facultad facultad = Facultad.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .activa(true)
                .build();

        facultad = facultadRepository.save(facultad);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(creador.getUsuario())
                .nombreUsuario(creador.getNombre())
                .rolUsuario(creador.getRol())
                .etiquetaCargoUsuario(creador.getEtiquetaCargo())
                .modulo("FACULTADES")
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(facultad.getId())
                .registroAfectadoTipo("Facultad")
                .valoresNuevos("{\"nombre\":\"" + facultad.getNombre() + "\"}")
                .exitoso(true));

        return FacultadResponse.desde(facultad);
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public FacultadResponse editarFacultad(Long id, CrearFacultadRequest request, CustomUserDetails editor) {
        Facultad facultad = buscarPorId(id);
        facultad.setNombre(request.getNombre());
        facultad.setDescripcion(request.getDescripcion());
        facultadRepository.save(facultad);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(editor.getUsuario())
                .nombreUsuario(editor.getNombre())
                .rolUsuario(editor.getRol())
                .modulo("FACULTADES")
                .tipoAccion(TipoAccion.EDITAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Facultad")
                .exitoso(true));

        return FacultadResponse.desde(facultad);
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void desactivarFacultad(Long id, CustomUserDetails ejecutor) {
        Facultad facultad = buscarPorId(id);

        if (facultad.tieneRecursosActivos()) {
            throw new OperacionNoPermitidaException(
                    "No se puede desactivar la facultad porque tiene programas activos.");
        }

        facultad.setActiva(false);
        facultadRepository.save(facultad);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(ejecutor.getUsuario())
                .nombreUsuario(ejecutor.getNombre())
                .rolUsuario(ejecutor.getRol())
                .modulo("FACULTADES")
                .tipoAccion(TipoAccion.DESACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Facultad")
                .exitoso(true));
    }

    public Page<FacultadResponse> listar(Pageable pageable) {
        return facultadRepository.findByActivaTrue(pageable).map(FacultadResponse::desde);
    }

    public FacultadResponse obtenerPorId(Long id) {
        return FacultadResponse.desde(buscarPorId(id));
    }

    private Facultad buscarPorId(Long id) {
        return facultadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Facultad no encontrada: " + id));
    }
}
