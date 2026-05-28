package co.edu.cue.practicas.service.usuario;

import co.edu.cue.practicas.audit.ModuloAuditoria;
import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearUsuarioRequest;
import co.edu.cue.practicas.dto.request.EditarUsuarioRequest;
import co.edu.cue.practicas.dto.response.UsuarioResponse;
import co.edu.cue.practicas.event.UsuarioCreadoEvent;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoCuenta;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.facultad.FacultadRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * PATRON SINGLETON + PROXY — GPE-136
 *
 * Servicio de gestión de usuarios.
 * Los métodos anotados con @RequiereRol son interceptados por
 * ScopeValidationAspect (Proxy) antes de ejecutarse, verificando
 * que el usuario autenticado tenga el rol correcto. Si no lo tiene,
 * el proxy lanza una excepción 403 sin llegar a ejecutar el método.
 *
 * Solo el Administrador DTI puede crear, editar, activar y desactivar usuarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    // Acceso a la tabla de usuarios en la base de datos
    private final UsuarioRepository usuarioRepository;

    // Necesitamos verificar que la facultad exista antes de asignársela al usuario
    private final FacultadRepository facultadRepository;

    // Necesitamos verificar que el programa exista antes de asignárselo al usuario
    private final ProgramaRepository programaRepository;

    // Encripta la contraseña temporal antes de guardarla en la BD
    private final PasswordEncoder passwordEncoder;

    // Registra cada operación (crear, editar, activar, desactivar) en la bitácora
    private final AuditoriaLogger auditoriaLogger;

    // Publica eventos de dominio (PATRON OBSERVER) para notificar a otros módulos sin acoplamiento directo
    private final ApplicationEventPublisher eventPublisher;

    // Serializa objetos a JSON para guardar el "antes y después" de cada cambio en la bitácora
    private final ObjectMapper objectMapper;

    /**
     * Crea un nuevo usuario en el sistema y le envía su contraseña temporal por correo.
     *
     * Reglas de negocio aplicadas:
     *   - El correo no puede estar repetido en el sistema.
     *   - Si el rol es COORDINACION_ACADEMICA, la etiqueta de cargo es obligatoria.
     *   - Los estudiantes se crean siempre con estado NO_APTO (regla OCL del modelo).
     *   - La contraseña es generada aleatoriamente; el usuario la cambiará en el primer login.
     *
     * Al final se publica un evento (PATRON OBSERVER) que dispara el envío
     * del correo de bienvenida sin que este servicio conozca al EmailService.
     *
     * @param request   datos del nuevo usuario (nombre, correo, rol, facultad, programa, etc.)
     * @param creador   usuario autenticado que ejecuta la acción (queda en la bitácora)
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request, CustomUserDetails creador) {

        // Verificamos que el correo no esté ya registrado antes de crear el usuario
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new OperacionNoPermitidaException("El correo ya está registrado en el sistema.");
        }

        // La etiqueta de cargo distingue al coordinador (ej. Decano, Jefe de Depto.)
        // Solo aplica para el rol COORDINACION_ACADEMICA y es obligatoria en ese caso
        if (Rol.COORDINACION_ACADEMICA.equals(request.getRol()) && request.getEtiquetaCargo() == null) {
            throw new OperacionNoPermitidaException("La etiqueta de cargo es obligatoria para el rol Coordinación Académica.");
        }

        // OCL: identificacionUnica — solo aplica para estudiantes
        if (Rol.ESTUDIANTE.equals(request.getRol())
                && request.getIdentificacion() != null
                && usuarioRepository.existsByIdentificacion(request.getIdentificacion())) {
            throw new OperacionNoPermitidaException(
                    "Ya existe un estudiante con la identificación: " + request.getIdentificacion());
        }

        // Resolvemos las relaciones opcionales (facultad y programa) si vienen en el request
        Facultad facultad = resolverFacultad(request);
        Programa programa = resolverPrograma(request);

        // Generamos una contraseña aleatoria segura de 12 caracteres en Base64URL
        String passwordTemporal = generarPasswordTemporal();

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .correo(request.getCorreo())
                .passwordHash(passwordEncoder.encode(passwordTemporal))  // guardamos el hash, nunca el texto plano
                .telefono(request.getTelefono())
                .rol(request.getRol())
                .etiquetaCargo(request.getEtiquetaCargo())
                .facultad(facultad)
                .programa(programa)
                .activo(true)
                .primerIngreso(true)       // obliga al usuario a cambiar la contraseña en el primer login
                .estadoCuenta(EstadoCuenta.PENDIENTE)  // no ha iniciado sesión aún con la password temporal
                // OCL: todo estudiante se crea siempre con estado NO_APTO hasta que Coordinación lo valide
                .estadoEstudiante(Rol.ESTUDIANTE.equals(request.getRol()) ? EstadoEstudiante.NO_APTO : null)
                .identificacion(Rol.ESTUDIANTE.equals(request.getRol()) ? request.getIdentificacion() : null)
                .semestre(Rol.ESTUDIANTE.equals(request.getRol()) ? request.getSemestre() : null)
                .contactoEmergencia(Rol.ESTUDIANTE.equals(request.getRol()) ? request.getContactoEmergencia() : null)
                .build();

        // Persistimos el usuario en la base de datos
        usuario = usuarioRepository.save(usuario);

        // PATRON OBSERVER: publicamos el evento de usuario creado.
        // El NotificacionEventListener lo escucha y envía el correo con la contraseña temporal
        // de forma asíncrona, sin que este servicio dependa directamente del EmailService.
        eventPublisher.publishEvent(new UsuarioCreadoEvent(this, usuario, passwordTemporal));

        // Registramos la creación en la bitácora con los valores del nuevo usuario
        auditoriaLogger.registrar(iniciarAuditoria(creador)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(usuario.getId())
                .registroAfectadoTipo("Usuario")
                .valoresNuevos(toJson(usuario))  // snapshot del objeto recién creado
                .exitoso(true));

        return UsuarioResponse.desde(usuario);
    }

    /**
     * Edita los datos básicos de un usuario: nombre, teléfono y foto de perfil.
     * No permite cambiar correo, rol ni contraseña desde aquí.
     * Guarda en la bitácora el estado anterior y el nuevo para poder comparar cambios.
     *
     * @param id       ID del usuario a editar
     * @param request  nuevos valores de nombre, teléfono y fotoPerfil
     * @param editor   usuario autenticado que realiza la edición
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public UsuarioResponse editarUsuario(Long id, EditarUsuarioRequest request, CustomUserDetails editor) {
        Usuario usuario = buscarPorId(id);

        // Capturamos el estado actual antes de modificarlo para el registro de auditoría
        String antes = toJson(usuario);

        // Aplicamos solo los campos editables (correo y rol no se pueden cambiar aquí)
        usuario.setNombre(request.getNombre());
        usuario.setTelefono(request.getTelefono());
        usuario.setFotoPerfil(request.getFotoPerfil());
        usuarioRepository.save(usuario);

        // Guardamos en la bitácora tanto los valores previos como los nuevos
        auditoriaLogger.registrar(iniciarAuditoria(editor)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.EDITAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Usuario")
                .valoresAnteriores(antes)       // cómo estaba antes del cambio
                .valoresNuevos(toJson(usuario)) // cómo quedó después
                .exitoso(true));

        return UsuarioResponse.desde(usuario);
    }

    /**
     * Desactiva un usuario del sistema (soft delete — no se borra de la BD).
     *
     * Regla de negocio crítica: el sistema siempre debe tener al menos un
     * Administrador DTI activo. Si se intenta desactivar al único DTI activo,
     * se lanza una excepción para proteger el acceso al sistema.
     *
     * @param id        ID del usuario a desactivar
     * @param ejecutor  usuario autenticado que ejecuta la acción
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void desactivarUsuario(Long id, CustomUserDetails ejecutor) {
        Usuario usuario = buscarPorId(id);

        // OCL: minimoUnDTIActivo — nunca se puede quedar el sistema sin un DTI activo
        if (Rol.ADMIN_DTI.equals(usuario.getRol())) {
            long dtisActivos = usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN_DTI);
            if (dtisActivos <= 1) {
                throw new OperacionNoPermitidaException(
                        "No se puede desactivar al único Administrador DTI activo del sistema.");
            }
        }

        // Marcamos como inactivo sin eliminar el registro (los datos históricos se conservan)
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        // Registramos la desactivación en la bitácora
        auditoriaLogger.registrar(iniciarAuditoria(ejecutor)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.DESACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Usuario")
                .exitoso(true));
    }

    /**
     * Reactiva un usuario que estaba desactivado, permitiéndole volver a iniciar sesión.
     *
     * @param id        ID del usuario a activar
     * @param ejecutor  usuario autenticado que ejecuta la acción
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void activarUsuario(Long id, CustomUserDetails ejecutor) {
        Usuario usuario = buscarPorId(id);

        // Reactivamos el acceso al sistema para este usuario
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        // Registramos la reactivación en la bitácora
        auditoriaLogger.registrar(iniciarAuditoria(ejecutor)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.ACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo("Usuario")
                .exitoso(true));
    }

    /**
     * Retorna todos los usuarios del sistema con paginación.
     * El tamaño y orden de página se configura desde el controlador.
     *
     * @Transactional(readOnly = true) mantiene la sesión JPA abierta durante el .map(),
     * necesario porque UsuarioResponse.desde() accede a u.getFacultad().getNombre()
     * y u.getPrograma().getNombre() — ambas relaciones son LAZY @ManyToOne.
     * Sin esta anotación Hibernate cierra la sesión tras findAll() y lanza
     * LazyInitializationException al intentar navegar esas relaciones.
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::desde);
    }

    /**
     * Retorna los datos de un usuario específico por su ID.
     * Lanza 404 si el usuario no existe.
     *
     * Mismo motivo que listarUsuarios(): la sesión JPA debe estar activa cuando
     * UsuarioResponse.desde() accede a getFacultad() y getPrograma().
     */
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.desde(buscarPorId(id));
    }

    // =====================================================================
    // Métodos privados de apoyo
    // =====================================================================

    /** Busca un usuario por ID o lanza 404 si no existe. Centraliza el manejo del error. */
    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
    }

    /**
     * Resuelve la facultad del request si se proporcionó un ID.
     * La facultad es opcional para roles como ADMIN_DTI o DIRECCION que no pertenecen a una facultad.
     */
    private Facultad resolverFacultad(CrearUsuarioRequest request) {
        if (request.getFacultadId() == null) return null;
        return facultadRepository.findById(request.getFacultadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Facultad no encontrada: " + request.getFacultadId()));
    }

    /**
     * Resuelve el programa del request si se proporcionó un ID.
     * El programa es opcional; solo aplica para el rol COORDINADOR_PRACTICAS.
     */
    private Programa resolverPrograma(CrearUsuarioRequest request) {
        if (request.getProgramaId() == null) return null;
        return programaRepository.findById(request.getProgramaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + request.getProgramaId()));
    }

    /**
     * Genera una contraseña temporal aleatoria de 12 caracteres usando Base64URL.
     * Se usa SecureRandom (criptográficamente seguro) en lugar de Random para que
     * no sea predecible. La contraseña se envía por correo y el usuario la cambia al primer login.
     */
    private String generarPasswordTemporal() {
        byte[] bytes = new byte[9];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Construye la base del registro de auditoría con los datos del actor que ejecuta la acción.
     * Se usa en todos los métodos para evitar repetir los mismos 4 campos en cada bloque.
     * El llamador solo agrega los campos específicos de la operación (módulo, tipo de acción, etc.)
     */
    private BitacoraAuditoria.BitacoraAuditoriaBuilder iniciarAuditoria(CustomUserDetails actor) {
        return BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .etiquetaCargoUsuario(actor.getEtiquetaCargo());
    }

    /**
     * Convierte cualquier objeto a formato JSON para guardarlo en la bitácora.
     * Si falla la serialización (caso extremadamente raro), devuelve "{}" para
     * no interrumpir el flujo principal por un error de logging.
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
