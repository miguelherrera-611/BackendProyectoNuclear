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
 * que el usuario autenticado tenga el rol correcto.
 *
 * Solo el Administrador DTI puede crear, editar, activar y desactivar usuarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String TIPO_REGISTRO = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final FacultadRepository facultadRepository;
    private final ProgramaRepository programaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaLogger auditoriaLogger;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request, CustomUserDetails creador) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new OperacionNoPermitidaException("El correo ya esta registrado en el sistema.");
        }

        if (Rol.COORDINACION_ACADEMICA.equals(request.getRol()) && request.getEtiquetaCargo() == null) {
            throw new OperacionNoPermitidaException("La etiqueta de cargo es obligatoria para el rol Coordinacion Academica.");
        }

        if (Rol.ESTUDIANTE.equals(request.getRol())
                && request.getIdentificacion() != null
                && usuarioRepository.existsByIdentificacion(request.getIdentificacion())) {
            throw new OperacionNoPermitidaException(
                    "Ya existe un estudiante con la identificacion: " + request.getIdentificacion());
        }

        Facultad facultad = resolverFacultad(request);
        Programa programa = resolverPrograma(request);
        String passwordTemporal = generarPasswordTemporal();

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .correo(request.getCorreo())
                .passwordHash(passwordEncoder.encode(passwordTemporal))
                .telefono(request.getTelefono())
                .rol(request.getRol())
                .etiquetaCargo(request.getEtiquetaCargo())
                .facultad(facultad)
                .programa(programa)
                .activo(true)
                .primerIngreso(true)
                .estadoCuenta(EstadoCuenta.PENDIENTE)
                .estadoEstudiante(Rol.ESTUDIANTE.equals(request.getRol()) ? EstadoEstudiante.NO_APTO : null)
                .identificacion(Rol.ESTUDIANTE.equals(request.getRol()) ? request.getIdentificacion() : null)
                .semestre(Rol.ESTUDIANTE.equals(request.getRol()) ? request.getSemestre() : null)
                .contactoEmergencia(Rol.ESTUDIANTE.equals(request.getRol()) ? request.getContactoEmergencia() : null)
                .build();

        usuario = usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new UsuarioCreadoEvent(this, usuario, passwordTemporal));

        auditoriaLogger.registrar(iniciarAuditoria(creador)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(usuario.getId())
                .registroAfectadoTipo(TIPO_REGISTRO)
                .valoresNuevos(toJson(usuario))
                .exitoso(true));

        return UsuarioResponse.desde(usuario);
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public UsuarioResponse editarUsuario(Long id, EditarUsuarioRequest request, CustomUserDetails editor) {
        Usuario usuario = buscarPorId(id);
        String antes = toJson(usuario);

        usuario.setNombre(request.getNombre());
        usuario.setTelefono(request.getTelefono());
        usuario.setFotoPerfil(request.getFotoPerfil());
        usuarioRepository.save(usuario);

        auditoriaLogger.registrar(iniciarAuditoria(editor)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.EDITAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo(TIPO_REGISTRO)
                .valoresAnteriores(antes)
                .valoresNuevos(toJson(usuario))
                .exitoso(true));

        return UsuarioResponse.desde(usuario);
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void desactivarUsuario(Long id, CustomUserDetails ejecutor) {
        Usuario usuario = buscarPorId(id);

        if (Rol.ADMIN_DTI.equals(usuario.getRol())) {
            long dtisActivos = usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN_DTI);
            if (dtisActivos <= 1) {
                throw new OperacionNoPermitidaException(
                        "No se puede desactivar al único Administrador DTI activo del sistema.");
            }
        }

        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        auditoriaLogger.registrar(iniciarAuditoria(ejecutor)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.DESACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo(TIPO_REGISTRO)
                .exitoso(true));
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional
    public void activarUsuario(Long id, CustomUserDetails ejecutor) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        auditoriaLogger.registrar(iniciarAuditoria(ejecutor)
                .modulo(ModuloAuditoria.USUARIOS)
                .tipoAccion(TipoAccion.ACTIVAR)
                .registroAfectadoId(id)
                .registroAfectadoTipo(TIPO_REGISTRO)
                .exitoso(true));
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::desde);
    }

    @RequiereRol(roles = {Rol.COORDINADOR_PRACTICAS, Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public java.util.List<UsuarioResponse> listarDocentesActivos() {
        return usuarioRepository.findByRolAndActivoTrue(Rol.DOCENTE_ASESOR,
                        org.springframework.data.domain.PageRequest.of(0, 500,
                                org.springframework.data.domain.Sort.by("nombre")))
                .map(UsuarioResponse::desde)
                .getContent();
    }

    @RequiereRol(roles = {Rol.ADMIN_DTI})
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.desde(buscarPorId(id));
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
    }

    private Facultad resolverFacultad(CrearUsuarioRequest request) {
        if (request.getFacultadId() == null) return null;
        return facultadRepository.findById(request.getFacultadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Facultad no encontrada: " + request.getFacultadId()));
    }

    private Programa resolverPrograma(CrearUsuarioRequest request) {
        if (request.getProgramaId() == null) return null;
        return programaRepository.findById(request.getProgramaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + request.getProgramaId()));
    }

    private String generarPasswordTemporal() {
        byte[] bytes = new byte[9];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private BitacoraAuditoria.BitacoraAuditoriaBuilder iniciarAuditoria(CustomUserDetails actor) {
        return BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .etiquetaCargoUsuario(actor.getEtiquetaCargo());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}