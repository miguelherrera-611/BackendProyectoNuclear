package co.edu.cue.practicas.service.catalogo;

import co.edu.cue.practicas.dto.request.ActualizarCatalogoPracticaRequest;
import co.edu.cue.practicas.dto.request.CrearCatalogoPracticaRequest;
import co.edu.cue.practicas.dto.response.CatalogoPracticaResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.builder.CatalogoPracticaDirector;
import co.edu.cue.practicas.repository.catalogo.CatalogoPracticaRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import co.edu.cue.practicas.security.annotation.SoloLectura;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GPE-141 — CatalogoPracticaService
 *
 * SOLID — SRP: orquesta el flujo de negocio del catálogo de prácticas.
 *              Construcción     → CatalogoPracticaDirector + Builder.
 *              Mapping          → EstudianteMapper.
 *              Validaciones OCL → método privado + InstanciaPracticaRepository.
 *
 * PATRÓN BUILDER: usa CatalogoPracticaDirector para construir entradas del catálogo.
 * PATRÓN SINGLETON: Spring gestiona este bean como singleton.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CatalogoPracticaService {

    private final CatalogoPracticaRepository catalogoRepository;
    private final ProgramaRepository programaRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final CatalogoPracticaDirector director;
    private final EstudianteMapper mapper;

    // ── CREAR — PATRÓN BUILDER via Director ──────────────────────────────────

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    public CatalogoPracticaResponse crearEntrada(CrearCatalogoPracticaRequest req) {
        Programa programa = buscarProgramaOFallar(req.programaId());
        validarNumeroUnico(req.programaId(), req.numeroPractica());

        // PATRÓN BUILDER: el Director dirige la construcción paso a paso
        CatalogoPractica catalogo = director.construirDesdeSolicitud(programa, req);
        catalogoRepository.save(catalogo);

        log.info("[GPE-141] Catálogo creado: '{}' (práctica {}) para programa {}",
                catalogo.getNombre(), catalogo.getNumeroPractica(), programa.getNombre());
        return mapper.toCatalogoPracticaResponse(catalogo);
    }

    // ── LEER ─────────────────────────────────────────────────────────────────

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public List<CatalogoPracticaResponse> listarTodos() {
        return catalogoRepository.findAll().stream()
                .map(mapper::toCatalogoPracticaResponse)
                .toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public List<CatalogoPracticaResponse> listarPorPrograma(Long programaId) {
        return catalogoRepository.findByPrograma_Id(programaId).stream()
                .map(mapper::toCatalogoPracticaResponse)
                .toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public List<CatalogoPracticaResponse> listarActivosPorPrograma(Long programaId) {
        return catalogoRepository.findByPrograma_IdAndActivoTrue(programaId).stream()
                .map(mapper::toCatalogoPracticaResponse)
                .toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public CatalogoPracticaResponse obtenerPorId(Long id) {
        return mapper.toCatalogoPracticaResponse(buscarOFallar(id));
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────────

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    public CatalogoPracticaResponse actualizar(Long id, ActualizarCatalogoPracticaRequest req) {
        CatalogoPractica catalogo = buscarOFallar(id);
        catalogo.setNombre(req.nombre());
        catalogo.setMateriaNucleo(req.materiaNucleo());
        catalogo.setCodigoMateria(req.codigoMateria());
        catalogo.setNumCortes(req.numCortes());
        catalogo.setDuracionSemanas(req.duracionSemanas());
        catalogo.setDocumentosRequeridos(req.documentosRequeridos());
        catalogoRepository.save(catalogo);
        log.info("[GPE-141] Catálogo '{}' actualizado", catalogo.getNombre());
        return mapper.toCatalogoPracticaResponse(catalogo);
    }

    // ── DESACTIVAR — OCL: noDesactivarConEstudiantesActivos ──────────────────

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    public CatalogoPracticaResponse desactivar(Long id) {
        CatalogoPractica catalogo = buscarOFallar(id);
        validarSinEstudiantesActivos(id);
        catalogo.desactivar();
        catalogoRepository.save(catalogo);
        log.info("[GPE-141] Catálogo '{}' desactivado", catalogo.getNombre());
        return mapper.toCatalogoPracticaResponse(catalogo);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public CatalogoPractica buscarOFallar(Long id) {
        return catalogoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Catálogo de práctica no encontrado con id: " + id));
    }

    public CatalogoPractica buscarActivoPorProgramaYNumero(Long programaId, int numeroPractica) {
        return catalogoRepository.findByPrograma_IdAndNumeroPracticaAndActivoTrue(
                programaId, numeroPractica)
                .orElse(null);
    }

    private Programa buscarProgramaOFallar(Long programaId) {
        return programaRepository.findById(programaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Programa no encontrado con id: " + programaId));
    }

    private void validarNumeroUnico(Long programaId, int numeroPractica) {
        if (catalogoRepository.existsByPrograma_IdAndNumeroPractica(programaId, numeroPractica))
            throw new OperacionNoPermitidaException(
                    "Ya existe una entrada en el catálogo para la Práctica "
                    + numeroPractica + " en este programa.");
    }

    /** OCL: noDesactivarConEstudiantesActivos */
    private void validarSinEstudiantesActivos(Long catalogoId) {
        List<EstadoPractica> estadosFinales = List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA);
        boolean tieneActivos = instanciaRepository
                .existsByCatalogoPracticaIdAndEstadoNotIn(catalogoId, estadosFinales);
        if (tieneActivos)
            throw new OperacionNoPermitidaException(
                    "No se puede desactivar el catálogo: hay estudiantes activos en esta práctica.");
    }
}
