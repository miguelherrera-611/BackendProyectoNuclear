package co.edu.cue.practicas.service.tutor;

import co.edu.cue.practicas.dto.request.ActualizarTelefonoTutorRequest;
import co.edu.cue.practicas.dto.request.CrearTutorRequest;
import co.edu.cue.practicas.dto.response.TutorEmpresarialResponse;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.TutorEmpresarial;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.tutor.TutorEmpresarialRepository;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import co.edu.cue.practicas.security.annotation.SoloLectura;
import co.edu.cue.practicas.service.empresa.EmpresaService;
import co.edu.cue.practicas.service.mapper.Dev3Mapper;
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import co.edu.cue.practicas.service.validator.TutorEmpresarialValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GPE-151 — TutorEmpresarialService
 *
 * SOLID — SRP: solo orquesta el flujo de negocio del tutor.
 *              Validaciones → EmpresaValidator + TutorEmpresarialValidator.
 *              Mapping      → Dev3Mapper.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TutorEmpresarialService {

    private final TutorEmpresarialRepository tutorRepository;
    private final EmpresaService empresaService;
    private final EmpresaValidator empresaValidator;
    private final TutorEmpresarialValidator tutorValidator;
    private final Dev3Mapper mapper;

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    public TutorEmpresarialResponse crearTutor(CrearTutorRequest req) {
        Empresa empresa = empresaService.buscarOFallar(req.empresaId());

        // Validaciones separadas (SRP)
        empresaValidator.validarEmpresaAprobadaParaTutores(empresa);
        tutorValidator.validarCorreoUnico(req.correo());

        TutorEmpresarial tutor = TutorEmpresarial.builder()
                .nombre(req.nombre())
                .cargo(req.cargo())
                .correo(req.correo())
                .telefono(req.telefono())
                .empresa(empresa)
                .build();

        tutorRepository.save(tutor);
        log.info("[GPE-151] Tutor creado: {} → empresa {}", tutor.getNombre(), empresa.getRazonSocial());
        return mapper.toTutorResponse(tutor);
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public TutorEmpresarialResponse obtenerPorId(Long id) {
        return mapper.toTutorResponse(buscarOFallar(id));
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public List<TutorEmpresarialResponse> listarPorEmpresa(Long empresaId) {
        return tutorRepository.findByEmpresaId(empresaId)
                .stream().map(mapper::toTutorResponse).toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public List<TutorEmpresarialResponse> listarTodos() {
        return tutorRepository.findAll()
                .stream().map(mapper::toTutorResponse).toList();
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    public TutorEmpresarialResponse desactivarTutor(Long id) {
        TutorEmpresarial tutor = buscarOFallar(id);
        tutor.desactivar();
        tutorRepository.save(tutor);
        log.info("[GPE-151] Tutor desactivado: {}", tutor.getNombre());
        return mapper.toTutorResponse(tutor);
    }

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI})
    public TutorEmpresarialResponse actualizarTelefono(Long id, ActualizarTelefonoTutorRequest req) {
        TutorEmpresarial tutor = buscarOFallar(id);
        tutor.setTelefono(req.telefono());
        tutorRepository.save(tutor);
        log.info("[GPE-151] Teléfono actualizado para tutor id={}", id);
        return mapper.toTutorResponse(tutor);
    }

    public TutorEmpresarial buscarOFallar(Long id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tutor no encontrado con id: " + id));
    }
}
