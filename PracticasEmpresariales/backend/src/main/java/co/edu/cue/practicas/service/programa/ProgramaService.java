package co.edu.cue.practicas.service.programa;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearProgramaRequest;
import co.edu.cue.practicas.dto.response.ProgramaResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.facultad.FacultadRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramaService {

    private final ProgramaRepository programaRepository;
    private final FacultadRepository facultadRepository;
    private final AuditoriaLogger auditoriaLogger;

    /**
     * PATRON BUILDER — GPE-140
     * Construye el programa paso a paso usando ProgramaBuilder.
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public ProgramaResponse crearPrograma(CrearProgramaRequest request, CustomUserDetails creador) {
        Facultad facultad = facultadRepository.findById(request.getFacultadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Facultad no encontrada: " + request.getFacultadId()));

        if (programaRepository.existsByNombreIgnoreCaseAndFacultad_Id(request.getNombre(), request.getFacultadId())) {
            throw new OperacionNoPermitidaException("Ya existe un programa con ese nombre en la facultad.");
        }

        ProgramaBuilder builder = ProgramaBuilder.nuevo()
                .conNombre(request.getNombre())
                .conDescripcion(request.getDescripcion())
                .enFacultad(facultad)
                .conNumeroDePracticas(request.getNumeroTotalPracticas())
                .conPromedioMinimoGeneral(request.getPromedioMinimoGeneral());

        if (request.getRequisitos() != null) {
            for (var req : request.getRequisitos()) {
                builder.agregarRequisitoPractica(
                        req.getNumeroPractica(),
                        req.getCreditosMinimos(),
                        req.getPromedioMinimo(),
                        req.isRequierePracticaAnteriorAprobada(),
                        req.getDocumentosRequeridos()
                );
            }
        }

        Programa programa = programaRepository.save(builder.construir());

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(creador.getUsuario())
                .nombreUsuario(creador.getNombre())
                .rolUsuario(creador.getRol())
                .modulo("PROGRAMAS")
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(programa.getId())
                .registroAfectadoTipo("Programa")
                .valoresNuevos("{\"nombre\":\"" + programa.getNombre() + "\",\"facultad\":\"" + facultad.getNombre() + "\"}")
                .exitoso(true));

        return ProgramaResponse.desde(programa);
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void desactivarPrograma(Long id, CustomUserDetails ejecutor) {
        Programa programa = buscarPorId(id);
        programa.setActivo(false);
        programaRepository.save(programa);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(ejecutor.getUsuario())
                .nombreUsuario(ejecutor.getNombre())
                .rolUsuario(ejecutor.getRol())
                .modulo("PROGRAMAS")
                .tipoAccion(TipoAccion.DESACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Programa")
                .exitoso(true));
    }

    public Page<ProgramaResponse> listar(Pageable pageable) {
        return programaRepository.findByActivoTrue(pageable).map(ProgramaResponse::desde);
    }

    public List<ProgramaResponse> listarPorFacultad(Long facultadId) {
        return programaRepository.findByFacultad_IdAndActivoTrue(facultadId)
                .stream().map(ProgramaResponse::desde).toList();
    }

    public ProgramaResponse obtenerPorId(Long id) {
        return ProgramaResponse.desde(buscarPorId(id));
    }

    private Programa buscarPorId(Long id) {
        return programaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + id));
    }
}
