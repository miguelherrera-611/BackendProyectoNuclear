package co.edu.cue.practicas.service.programa;

import co.edu.cue.practicas.audit.ModuloAuditoria;
import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Servicio encargado del CRUD de programas académicos.
 * Usa el PATRON BUILDER (a través de ProgramaBuilder) para construir
 * objetos Programa con configuración compleja paso a paso.
 *
 * Solo el Administrador DTI puede crear y desactivar programas.
 * Listar y consultar son accesibles a cualquier usuario autenticado.
 */
@Service
@RequiredArgsConstructor
public class ProgramaService {

    // Repositorio JPA para operaciones sobre la tabla de programas
    private final ProgramaRepository programaRepository;

    // Necesitamos la facultad para asignarla al programa y verificar que exista
    private final FacultadRepository facultadRepository;

    // Registra cada operación en la bitácora de auditoría
    private final AuditoriaLogger auditoriaLogger;

    // Convierte objetos a JSON para guardar el estado en la bitácora
    private final ObjectMapper objectMapper;

    /**
     * PATRON BUILDER — GPE-140
     *
     * Crea un nuevo programa académico usando ProgramaBuilder para construir
     * el objeto paso a paso: nombre → descripción → facultad → número de prácticas
     * → promedio mínimo → requisitos por práctica.
     *
     * Usar el Builder evita tener un constructor con 10+ parámetros y permite
     * agregar los requisitos de práctica de forma encadenada y legible.
     *
     * @param request  datos del programa y sus requisitos por práctica
     * @param creador  usuario DTI autenticado que realiza la acción
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public ProgramaResponse crearPrograma(CrearProgramaRequest request, CustomUserDetails creador) {

        // Verificamos que la facultad exista antes de asignar el programa a ella
        Facultad facultad = facultadRepository.findById(request.getFacultadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Facultad no encontrada: " + request.getFacultadId()));

        // No permitimos duplicados de nombre dentro de la misma facultad (sin distinguir mayúsculas)
        if (programaRepository.existsByNombreIgnoreCaseAndFacultad_Id(request.getNombre(), request.getFacultadId())) {
            throw new OperacionNoPermitidaException("Ya existe un programa con ese nombre en la facultad.");
        }

        // PATRON BUILDER: construimos el programa de forma fluida y legible.
        // Cada método del builder devuelve el mismo builder para encadenar las llamadas.
        ProgramaBuilder builder = ProgramaBuilder.nuevo()
                .conNombre(request.getNombre())
                .conDescripcion(request.getDescripcion())
                .enFacultad(facultad)
                .conNumeroDePracticas(request.getNumeroTotalPracticas())
                .conPromedioMinimoGeneral(request.getPromedioMinimoGeneral());

        // Agregamos cada requisito de práctica si vienen en el request
        // (algunos programas pueden no tener requisitos definidos todavía)
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

        // El Builder valida los campos obligatorios (nombre y facultad) antes de construir el objeto
        Programa programa = programaRepository.save(builder.construir());

        // Registramos la creación en la bitácora con nombre y facultad como referencia
        auditoriaLogger.registrar(iniciarAuditoria(creador)
                .modulo(ModuloAuditoria.PROGRAMAS)
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(programa.getId())
                .registroAfectadoTipo("Programa")
                .valoresNuevos(toJson(java.util.Map.of(
                        "nombre", programa.getNombre(),
                        "facultad", facultad.getNombre())))
                .exitoso(true));

        return ProgramaResponse.desde(programa);
    }

    /**
     * Desactiva un programa del sistema (soft delete — no se elimina de la BD).
     * No valida si hay estudiantes activos en el programa; esa regla se agregará
     * en sprints posteriores cuando exista el módulo de prácticas.
     *
     * @param id        ID del programa a desactivar
     * @param ejecutor  usuario DTI autenticado que ejecuta la acción
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void desactivarPrograma(Long id, CustomUserDetails ejecutor) {
        Programa programa = buscarPorId(id);

        // Marcamos como inactivo sin borrar el registro de la BD
        programa.setActivo(false);
        programaRepository.save(programa);

        // Registramos la desactivación en la bitácora
        auditoriaLogger.registrar(iniciarAuditoria(ejecutor)
                .modulo(ModuloAuditoria.PROGRAMAS)
                .tipoAccion(TipoAccion.DESACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Programa")
                .exitoso(true));
    }

    /**
     * Lista todos los programas activos con paginación.
     * Accesible para cualquier usuario autenticado.
     */
    public Page<ProgramaResponse> listar(Pageable pageable) {
        return programaRepository.findByActivoTrue(pageable).map(ProgramaResponse::desde);
    }

    /**
     * Lista los programas activos de una facultad específica.
     * Se usa en el frontend para cargar el selector de programas al crear un usuario.
     *
     * @param facultadId  ID de la facultad cuyos programas se quieren listar
     */
    public List<ProgramaResponse> listarPorFacultad(Long facultadId) {
        return programaRepository.findByFacultad_IdAndActivoTrue(facultadId)
                .stream().map(ProgramaResponse::desde).toList();
    }

    /**
     * Retorna un programa específico por su ID.
     * Lanza 404 si el programa no existe.
     */
    public ProgramaResponse obtenerPorId(Long id) {
        return ProgramaResponse.desde(buscarPorId(id));
    }

    // =====================================================================
    // Métodos privados de apoyo
    // =====================================================================

    /** Busca un programa por ID o lanza 404 si no existe. Centraliza el manejo del error. */
    private Programa buscarPorId(Long id) {
        return programaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + id));
    }

    /**
     * Construye la base del registro de auditoría con los datos del actor que ejecuta la acción.
     * Se usa en todos los métodos para no repetir los mismos campos en cada bloque de auditoría.
     */
    private BitacoraAuditoria.BitacoraAuditoriaBuilder iniciarAuditoria(CustomUserDetails actor) {
        return BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .etiquetaCargoUsuario(actor.getEtiquetaCargo());
    }

    /**
     * Convierte un objeto a JSON para guardarlo en la bitácora.
     * Si falla la serialización, retorna "{}" para no interrumpir el flujo principal.
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
