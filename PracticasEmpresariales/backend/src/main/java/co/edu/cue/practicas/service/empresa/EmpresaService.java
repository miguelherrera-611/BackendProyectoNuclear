package co.edu.cue.practicas.service.empresa;

import co.edu.cue.practicas.dto.request.CrearEmpresaRequest;
import co.edu.cue.practicas.dto.response.EmpresaResponse;
import co.edu.cue.practicas.event.EmpresaObserver;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.builder.EmpresaBuilder;
import co.edu.cue.practicas.pattern.prototype.EmpresaPlantilla;
import co.edu.cue.practicas.repository.empresa.EmpresaRepository;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import co.edu.cue.practicas.security.annotation.SoloLectura;
import co.edu.cue.practicas.service.mapper.Dev3Mapper;
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GPE-150 — EmpresaService
 *
 * SOLID — SRP: solo orquesta el flujo de negocio de Empresa.
 *              Validaciones → EmpresaValidator.
 *              Mapping      → Dev3Mapper.
 *              Construcción → EmpresaBuilder (Builder pattern).
 *              Clonación    → EmpresaPlantilla (Prototype pattern).
 *
 * SOLID — DIP: depende de List<EmpresaObserver> (interfaz),
 *              no de TutorInactivacionObserver (concreción).
 *              Spring inyecta automáticamente todos los @Component
 *              que implementen EmpresaObserver.
 *
 * SOLID — OCP: para agregar un nuevo observer (ej. EmailObserver),
 *              solo se crea la clase con @Component. No se modifica aquí.
 *
 * PATRÓN SINGLETON: Spring gestiona este bean como singleton.
 * PATRÓN OBSERVER:  registra todos los observers antes de cambios de estado.
 * PATRÓN BUILDER:   usa EmpresaBuilder para construir entidades.
 * PATRÓN PROTOTYPE: usa EmpresaPlantilla para clonar configuraciones.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaValidator validator;
    private final Dev3Mapper mapper;

    /**
     * SOLID — DIP: Spring inyecta TODOS los beans que implementen EmpresaObserver.
     * Para agregar un nuevo observer, solo crear @Component que implemente la interfaz.
     */
    private final List<EmpresaObserver> observers;

    // ── CREAR — usa PATRÓN BUILDER ────────────────────────────────────────

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public EmpresaResponse crearEmpresa(CrearEmpresaRequest req) {
        validator.validarNitUnico(req.nit());

        // PATRÓN BUILDER: construye Empresa paso a paso
        Empresa empresa = new EmpresaBuilder()
                .conRazonSocial(req.razonSocial())
                .conNit(req.nit())
                .conSector(req.sector())
                .conDireccion(req.direccion(), req.municipio())
                .conTelefono(req.telefono())
                .conContacto(req.nombreContacto(), req.correo())
                .conAreas(req.areasDisponibles())
                .build();

        empresaRepository.save(empresa);
        log.info("[GPE-150] Empresa creada: {} (NIT: {})", empresa.getRazonSocial(), empresa.getNit());
        return mapper.toEmpresaResponse(empresa);
    }

    // ── CREAR DESDE PLANTILLA — usa PATRÓN PROTOTYPE ─────────────────────

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public EmpresaResponse crearDesdeEmpresaExistente(Long empresaOrigenId,
                                                       String nuevaRazonSocial,
                                                       String nuevoNit) {
        Empresa origen = buscarOFallar(empresaOrigenId);
        validator.validarNitUnico(nuevoNit);

        // PATRÓN PROTOTYPE: clona configuración base y ajusta solo lo necesario
        Empresa clon = new EmpresaPlantilla(origen).clonar();
        clon.setRazonSocial(nuevaRazonSocial);
        clon.setNit(nuevoNit);

        empresaRepository.save(clon);
        log.info("[GPE-150/Prototype] Empresa clonada desde {} → {}", origen.getRazonSocial(), nuevaRazonSocial);
        return mapper.toEmpresaResponse(clon);
    }

    // ── LEER ──────────────────────────────────────────────────────────────

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public EmpresaResponse obtenerPorId(Long id) {
        return mapper.toEmpresaResponse(buscarOFallar(id));
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public List<EmpresaResponse> listarTodas() {
        return empresaRepository.findAll().stream()
                .map(mapper::toEmpresaResponse).toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public List<EmpresaResponse> listarActivas() {
        return empresaRepository.findByEstado(EstadoEmpresa.ACTIVA)
                .stream().map(mapper::toEmpresaResponse).toList();
    }

    // ── CAMBIOS DE ESTADO — registra observers (DIP) ─────────────────────

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public EmpresaResponse activarEmpresa(Long id) {
        Empresa empresa = buscarOFallar(id);
        registrarObservers(empresa);
        empresa.activar();
        empresaRepository.save(empresa);
        log.info("[GPE-150] Empresa activada: {}", empresa.getRazonSocial());
        return mapper.toEmpresaResponse(empresa);
    }

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS})
    public EmpresaResponse inactivarEmpresa(Long id) {
        Empresa empresa = buscarOFallar(id);
        validator.validarSinVacantesActivas(id);
        registrarObservers(empresa); // Observer inactiva tutores automáticamente
        empresa.inactivar();
        empresaRepository.save(empresa);
        log.info("[GPE-150] Empresa inactivada: {}", empresa.getRazonSocial());
        return mapper.toEmpresaResponse(empresa);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public Empresa buscarOFallar(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Empresa no encontrada con id: " + id));
    }

    /**
     * PATRÓN OBSERVER + DIP:
     * Registra la lista de observers (interfaz) en la entidad.
     * La entidad nunca conoce las implementaciones concretas.
     */
    private void registrarObservers(Empresa empresa) {
        observers.forEach(empresa::agregarObserver);
    }
}
