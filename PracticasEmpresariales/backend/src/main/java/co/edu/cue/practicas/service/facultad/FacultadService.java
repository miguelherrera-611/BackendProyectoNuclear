package co.edu.cue.practicas.service.facultad;

import co.edu.cue.practicas.audit.ModuloAuditoria;
import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Servicio encargado del CRUD de facultades.
 * Solo el Administrador DTI tiene permiso para crear, editar y desactivar facultades.
 * La verificación del rol la realiza automáticamente el ScopeValidationAspect (PATRON PROXY)
 * al detectar la anotación @RequiereRol en cada método.
 *
 * Listar y consultar por ID son públicos para cualquier usuario autenticado,
 * ya que la información de facultades es de solo lectura para los demás roles.
 */
@Service
@RequiredArgsConstructor
public class FacultadService {

    // Repositorio JPA para operaciones sobre la tabla de facultades
    private final FacultadRepository facultadRepository;

    // Registra cada operación en la bitácora de auditoría
    private final AuditoriaLogger auditoriaLogger;

    // Convierte objetos a JSON para guardar el estado en la bitácora
    private final ObjectMapper objectMapper;

    /**
     * Crea una nueva facultad en el sistema.
     * Valida que no exista otra con el mismo nombre (sin importar mayúsculas).
     *
     * @param request  nombre y descripción de la nueva facultad
     * @param creador  usuario DTI autenticado que realiza la acción
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public FacultadResponse crearFacultad(CrearFacultadRequest request, CustomUserDetails creador) {

        // Verificamos que no exista una facultad con el mismo nombre (sin distinguir mayúsculas)
        if (facultadRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new OperacionNoPermitidaException("Ya existe una facultad con ese nombre.");
        }

        // Construimos la entidad y la persistimos en la base de datos
        Facultad facultad = Facultad.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .activa(true)  // toda facultad nueva empieza activa
                .build();

        facultad = facultadRepository.save(facultad);

        // Registramos la creación en la bitácora con el nombre de la facultad como referencia
        auditoriaLogger.registrar(iniciarAuditoria(creador)
                .modulo(ModuloAuditoria.FACULTADES)
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(facultad.getId())
                .registroAfectadoTipo("Facultad")
                .valoresNuevos(toJson(java.util.Map.of("nombre", facultad.getNombre())))
                .exitoso(true));

        return FacultadResponse.desde(facultad);
    }

    /**
     * Edita el nombre y la descripción de una facultad existente.
     * No permite cambiar la facultad de activa a inactiva desde aquí;
     * para eso existe desactivarFacultad().
     *
     * @param id       ID de la facultad a editar
     * @param request  nuevos valores para nombre y descripción
     * @param editor   usuario DTI autenticado que realiza el cambio
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public FacultadResponse editarFacultad(Long id, CrearFacultadRequest request, CustomUserDetails editor) {
        Facultad facultad = buscarPorId(id);

        // Aplicamos los cambios sobre la entidad existente
        facultad.setNombre(request.getNombre());
        facultad.setDescripcion(request.getDescripcion());
        facultadRepository.save(facultad);

        // Registramos la edición en la bitácora
        auditoriaLogger.registrar(iniciarAuditoria(editor)
                .modulo(ModuloAuditoria.FACULTADES)
                .tipoAccion(TipoAccion.EDITAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Facultad")
                .exitoso(true));

        return FacultadResponse.desde(facultad);
    }

    /**
     * Desactiva una facultad del sistema (soft delete — no se elimina de la BD).
     *
     * No se puede desactivar una facultad que tenga programas activos asociados,
     * ya que esos programas quedarían sin facultad padre, rompiendo la integridad del modelo.
     * La verificación la delega al método tieneRecursosActivos() de la entidad Facultad.
     *
     * @param id        ID de la facultad a desactivar
     * @param ejecutor  usuario DTI autenticado que ejecuta la acción
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void desactivarFacultad(Long id, CustomUserDetails ejecutor) {
        Facultad facultad = buscarPorId(id);

        // Verificamos que no haya programas activos que dependan de esta facultad
        if (facultad.tieneRecursosActivos()) {
            throw new OperacionNoPermitidaException(
                    "No se puede desactivar la facultad porque tiene programas activos.");
        }

        // Marcamos como inactiva sin borrar el registro
        facultad.setActiva(false);
        facultadRepository.save(facultad);

        // Registramos la desactivación en la bitácora
        auditoriaLogger.registrar(iniciarAuditoria(ejecutor)
                .modulo(ModuloAuditoria.FACULTADES)
                .tipoAccion(TipoAccion.DESACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Facultad")
                .exitoso(true));
    }

    /**
     * Lista todas las facultades activas con paginación.
     * Disponible para cualquier usuario autenticado — no requiere rol específico.
     *
     * @Transactional(readOnly = true) mantiene la sesión JPA abierta durante el .map(),
     * necesario porque FacultadResponse.desde() accede a facultad.getProgramas().size()
     * (colección LAZY). Sin esta anotación la sesión se cierra antes del mapeo y Hibernate
     * lanza LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public Page<FacultadResponse> listar(Pageable pageable) {
        return facultadRepository.findByActivaTrue(pageable).map(FacultadResponse::desde);
    }

    /**
     * Retorna una facultad específica por su ID.
     * Lanza 404 si la facultad no existe.
     *
     * Mismo motivo que listar(): FacultadResponse.desde() accede a getProgramas().size()
     * y necesita la sesión JPA activa.
     */
    @Transactional(readOnly = true)
    public FacultadResponse obtenerPorId(Long id) {
        return FacultadResponse.desde(buscarPorId(id));
    }

    // =====================================================================
    // Métodos privados de apoyo
    // =====================================================================

    /** Busca una facultad por ID o lanza 404 si no existe. Centraliza el manejo del error. */
    private Facultad buscarPorId(Long id) {
        return facultadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Facultad no encontrada: " + id));
    }

    /**
     * Construye la base del registro de auditoría con los datos del actor que ejecuta la acción.
     * Se usa en todos los métodos para evitar repetir los mismos campos en cada bloque.
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
     * Si falla, retorna "{}" para no interrumpir el flujo principal.
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
