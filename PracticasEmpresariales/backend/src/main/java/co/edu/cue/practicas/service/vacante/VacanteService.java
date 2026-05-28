package co.edu.cue.practicas.service.vacante;

import co.edu.cue.practicas.dto.request.CrearVacanteRequest;
import co.edu.cue.practicas.dto.request.RechazarRequest;
import co.edu.cue.practicas.dto.response.VacanteResponse;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.Vacante;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.builder.VacanteDirector;
import co.edu.cue.practicas.repository.vacante.VacanteRepository;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import co.edu.cue.practicas.security.annotation.SoloLectura;
import co.edu.cue.practicas.service.empresa.EmpresaService;
import co.edu.cue.practicas.service.mapper.Dev3Mapper;
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GPE-152 / GPE-153 — VacanteService
 *
 * SOLID — SRP: solo orquesta el flujo de negocio de Vacante.
 *              Validaciones  → EmpresaValidator.
 *              Mapping       → Dev3Mapper.
 *              Construcción  → VacanteDirector + VacanteBuilder (Builder pattern).
 *              Transiciones  → Vacante.aprobar/rechazar/cerrar (State pattern).
 *
 * PATRÓN BUILDER: usa VacanteDirector para construir vacantes.
 * PATRÓN STATE:   delega transiciones a la entidad Vacante.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VacanteService {

    private final VacanteRepository vacanteRepository;
    private final EmpresaService empresaService;
    private final EmpresaValidator empresaValidator;
    private final VacanteDirector vacanteDirector;
    private final Dev3Mapper mapper;

    // ── CREAR — PATRÓN BUILDER via Director ───────────────────────────────

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public VacanteResponse crearVacante(CrearVacanteRequest req) {
        Empresa empresa = empresaService.buscarOFallar(req.empresaId());
        empresaValidator.validarEmpresaAprobadaParaVacantes(empresa);

        // PATRÓN BUILDER: el Director orquesta la construcción
        Vacante vacante = vacanteDirector.construirVacanteEstandar(
                empresa, req.area(), req.cuposTotales());

        vacanteRepository.save(vacante);
        log.info("[GPE-152] Vacante creada → empresa: {}, área: {}", empresa.getRazonSocial(), req.area());
        return mapper.toVacanteResponse(vacante);
    }

    // ── LEER ──────────────────────────────────────────────────────────────

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public VacanteResponse obtenerPorId(Long id) {
        return mapper.toVacanteResponse(buscarOFallar(id));
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public List<VacanteResponse> listarTodas() {
        return vacanteRepository.findAll().stream()
                .map(mapper::toVacanteResponse).toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public List<VacanteResponse> listarPendientes() {
        return vacanteRepository.findByEstado(EstadoVacante.PENDIENTE)
                .stream().map(mapper::toVacanteResponse).toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION,
                          Rol.DOCENTE_ASESOR, Rol.TUTOR_EMPRESARIAL, Rol.ESTUDIANTE})
    @Transactional(readOnly = true)
    public List<VacanteResponse> listarDisponibles() {
        return vacanteRepository.findByEstado(EstadoVacante.DISPONIBLE)
                .stream().map(mapper::toVacanteResponse).toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public List<VacanteResponse> listarPorEmpresa(Long empresaId) {
        return vacanteRepository.findByEmpresaId(empresaId)
                .stream().map(mapper::toVacanteResponse).toList();
    }

    // ── APROBAR — PATRÓN STATE ────────────────────────────────────────────

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public VacanteResponse aprobarVacante(Long id) {
        Vacante vacante = buscarOFallar(id);
        vacante.aprobar(); // STATE: PENDIENTE → DISPONIBLE
        vacanteRepository.save(vacante);
        log.info("[GPE-153] Vacante {} aprobada", id);
        return mapper.toVacanteResponse(vacante);
    }

    // ── RECHAZAR — PATRÓN STATE ───────────────────────────────────────────

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public VacanteResponse rechazarVacante(Long id, RechazarRequest req) {
        Vacante vacante = buscarOFallar(id);
        vacante.rechazar(req.motivo()); // STATE: PENDIENTE → RECHAZADA
        vacanteRepository.save(vacante);
        log.info("[GPE-153] Vacante {} rechazada: {}", id, req.motivo());
        return mapper.toVacanteResponse(vacante);
    }

    // ── CERRAR — PATRÓN STATE ─────────────────────────────────────────────

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public VacanteResponse cerrarVacante(Long id) {
        Vacante vacante = buscarOFallar(id);
        vacante.cerrar(); // STATE: DISPONIBLE → CERRADA
        vacanteRepository.save(vacante);
        return mapper.toVacanteResponse(vacante);
    }

    public Vacante buscarOFallar(Long id) {
        return vacanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Vacante no encontrada con id: " + id));
    }
}
