package co.edu.cue.practicas.service.seguimiento;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.AprobarRechazarPlanRequest;
import co.edu.cue.practicas.dto.request.CrearPlanRequest;
import co.edu.cue.practicas.dto.response.PlanPracticaResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.PlanPractica;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoPlan;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.PlanPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.notificacion.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * GPE-167 — Servicio para gestión del plan de práctica.
 *
 * PATRON FACADE: coordina creación de plan + notificación + auditoría desde un punto.
 * PATRON OBSERVER: al aprobar/rechazar notifica al estudiante por correo.
 * PATRON STATE: delega las transiciones a PlanPractica (entidad).
 *
 * SOLID — SRP: solo gestiona el ciclo de vida del plan de práctica.
 * OCL: requierePlanAprobado — el seguimiento semanal no inicia sin APROBADO_DOCENTE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPracticaService {

    private static final long MAX_DOCUMENTO_BYTES = 10L * 1024 * 1024;
    private static final List<String> MIME_PERMITIDOS_PLAN = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final PlanPracticaRepository planRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaLogger auditoriaLogger;
    private final EmailService emailService;

    @Transactional
    public PlanPracticaResponse crearOActualizarPlan(Long instanciaId, CrearPlanRequest req, MultipartFile documento, CustomUserDetails actor) {
        if (actor.getRol() != Rol.ESTUDIANTE)
            throw new AccesoNoAutorizadoException("Solo el estudiante puede crear o actualizar su plan de práctica.");

        boolean tieneTexto = req != null && (esNoVacio(req.getObjetivos()) || esNoVacio(req.getCronograma()));
        boolean tieneDocumento = documento != null && !documento.isEmpty();
        if (!tieneTexto && !tieneDocumento)
            throw new OperacionNoPermitidaException("Debes proporcionar el contenido del plan: un documento o texto en los campos de objetivos/cronograma.");

        InstanciaPractica instancia = buscarInstanciaActiva(instanciaId);
        verificarPropiedadEstudiante(instancia, actor.getId());

        PlanPractica plan = planRepository
                .findTopByInstanciaPractica_IdOrderByCreadoEnDesc(instanciaId)
                .orElse(null);

        if (plan != null && (plan.getEstado() == EstadoPlan.APROBADO_DOCENTE || plan.getEstado() == EstadoPlan.APROBADO_TUTOR))
            throw new OperacionNoPermitidaException("El plan no puede modificarse en su estado actual (" + plan.getEstado() + ").");

        if (plan == null) {
            plan = PlanPractica.builder()
                    .instanciaPractica(instancia)
                    .objetivos(req != null ? req.getObjetivos() : null)
                    .cronograma(req != null ? req.getCronograma() : null)
                    .cargadoPorId(actor.getId())
                    .build();
        } else {
            if (req != null) {
                plan.setObjetivos(req.getObjetivos());
                plan.setCronograma(req.getCronograma());
            }
            if (plan.getEstado() == EstadoPlan.RECHAZADO) plan.resubmit();
        }

        if (tieneDocumento) {
            if (plan.getDocumentoRuta() != null) eliminarArchivoPlan(plan.getDocumentoRuta());
            String[] archivos = guardarArchivoPlan(instanciaId, documento);
            plan.setDocumentoNombre(archivos[0]);
            plan.setDocumentoRuta(archivos[1]);
        }

        planRepository.save(plan);
        registrarAuditoria(actor, TipoAccion.CREAR, plan.getId(), "PlanPractica",
                "{\"instanciaId\":" + instanciaId + ",\"accion\":\"cargado\"}");

        return PlanPracticaResponse.desde(plan);
    }

    public ResponseEntity<Resource> descargarDocumento(Long planId, CustomUserDetails actor) {
        PlanPractica plan = buscarPlan(planId);
        verificarAccesoLectura(plan.getInstanciaPractica().getId(), actor);
        if (plan.getDocumentoRuta() == null)
            throw new RecursoNoEncontradoException("Este plan no tiene documento adjunto.");
        try {
            Path rutaArchivo = Paths.get(plan.getDocumentoRuta()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(rutaArchivo.toUri());
            if (!resource.exists()) throw new RecursoNoEncontradoException("El archivo del plan no está disponible.");
            String contentType = determinarContentType(plan.getDocumentoNombre());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + plan.getDocumentoNombre() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RecursoNoEncontradoException("Error al acceder al archivo del plan.");
        }
    }

    @Transactional
    public PlanPracticaResponse obtenerPlanActual(Long instanciaId, CustomUserDetails actor) {
        verificarAccesoLectura(instanciaId, actor);
        return planRepository.findTopByInstanciaPractica_IdOrderByCreadoEnDesc(instanciaId)
                .map(PlanPracticaResponse::desde)
                .orElse(null);
    }

    @Transactional
    public List<PlanPracticaResponse> listarPlanes(Long instanciaId, CustomUserDetails actor) {
        verificarAccesoLectura(instanciaId, actor);
        return planRepository.findByInstanciaPractica_IdOrderByCreadoEnDesc(instanciaId)
                .stream()
                .map(PlanPracticaResponse::desde)
                .toList();
    }

    @Transactional
    public PlanPracticaResponse aprobarPorTutor(Long planId, CustomUserDetails actor) {
        if (actor.getRol() != Rol.TUTOR_EMPRESARIAL)
            throw new AccesoNoAutorizadoException("Solo el tutor empresarial puede aprobar el plan en esta etapa.");

        PlanPractica plan = buscarPlan(planId);
        verificarTutorDePlan(plan, actor);

        plan.aprobarPorTutor();
        planRepository.save(plan);

        registrarAuditoria(actor, TipoAccion.CAMBIO_ESTADO, planId, "PlanPractica",
                "{\"estado\":\"APROBADO_TUTOR\"}");
        notificarCambioEstadoPlan(plan, "aprobado por el tutor empresarial");
        return PlanPracticaResponse.desde(plan);
    }

    @Transactional
    public PlanPracticaResponse aprobarPorDocente(Long planId, CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR)
            throw new AccesoNoAutorizadoException("Solo el docente asesor puede dar la aprobación final del plan.");

        PlanPractica plan = buscarPlan(planId);
        verificarDocenteDePlan(plan, actor);

        plan.aprobarPorDocente();
        planRepository.save(plan);

        registrarAuditoria(actor, TipoAccion.CAMBIO_ESTADO, planId, "PlanPractica",
                "{\"estado\":\"APROBADO_DOCENTE\"}");
        notificarCambioEstadoPlan(plan, "aprobado por el docente asesor — puede iniciar seguimientos");
        return PlanPracticaResponse.desde(plan);
    }

    @Transactional
    public PlanPracticaResponse rechazarPlan(Long planId, AprobarRechazarPlanRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR && actor.getRol() != Rol.TUTOR_EMPRESARIAL)
            throw new AccesoNoAutorizadoException("Solo el docente asesor o tutor empresarial puede rechazar el plan.");
        if (req.getMotivo() == null || req.getMotivo().isBlank())
            throw new OperacionNoPermitidaException("El motivo de rechazo es obligatorio.");

        PlanPractica plan = buscarPlan(planId);
        plan.rechazar(req.getMotivo(), actor.getId());
        planRepository.save(plan);

        registrarAuditoria(actor, TipoAccion.CAMBIO_ESTADO, planId, "PlanPractica",
                "{\"estado\":\"RECHAZADO\",\"motivo\":\"" + req.getMotivo() + "\"}");
        notificarCambioEstadoPlan(plan, "rechazado: " + req.getMotivo());
        return PlanPracticaResponse.desde(plan);
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    private InstanciaPractica buscarInstanciaActiva(Long instanciaId) {
        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia de práctica no encontrada."));
        if (instancia.getEstado() != EstadoPractica.EN_CURSO)
            throw new OperacionNoPermitidaException("El plan solo puede gestionarse cuando la práctica está EN_CURSO.");
        return instancia;
    }

    private PlanPractica buscarPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan de práctica no encontrado."));
    }

    private void verificarPropiedadEstudiante(InstanciaPractica instancia, Long estudianteId) {
        Long ownerIdEnInstancia = instancia.getExpediente() != null
                && instancia.getExpediente().getEstudiante() != null
                ? instancia.getExpediente().getEstudiante().getId() : null;
        if (!estudianteId.equals(ownerIdEnInstancia))
            throw new AccesoNoAutorizadoException("Esta práctica no pertenece a tu expediente.");
    }

    private void verificarTutorDePlan(PlanPractica plan, CustomUserDetails actor) {
        Usuario tutor = usuarioRepository.findByCorreoAndActivoTrue(actor.getUsername()).orElse(null);
        if (tutor == null) throw new AccesoNoAutorizadoException("No se encontró tu perfil de tutor empresarial.");
        InstanciaPractica instancia = plan.getInstanciaPractica();
        if (instancia.getTutorEmpresarial() == null || !instancia.getTutorEmpresarial().getId().equals(tutor.getId()))
            throw new AccesoNoAutorizadoException("Solo el tutor asignado a esta práctica puede aprobarla.");
    }

    private void verificarDocenteDePlan(PlanPractica plan, CustomUserDetails actor) {
        InstanciaPractica instancia = plan.getInstanciaPractica();
        if (instancia.getDocenteAsesor() == null || !instancia.getDocenteAsesor().getId().equals(actor.getId()))
            throw new AccesoNoAutorizadoException("Solo el docente asesor asignado a esta práctica puede aprobarla.");
    }

    private void verificarAccesoLectura(Long instanciaId, CustomUserDetails actor) {
        if (actor.getRol() == Rol.ESTUDIANTE) {
            instanciaRepository.findById(instanciaId).ifPresent(i -> {
                if (i.getExpediente() == null || !i.getExpediente().getEstudiante().getId().equals(actor.getId()))
                    throw new AccesoNoAutorizadoException("No tienes acceso a esta instancia.");
            });
        }
    }

    private void notificarCambioEstadoPlan(PlanPractica plan, String descripcion) {
        InstanciaPractica instancia = plan.getInstanciaPractica();
        if (instancia == null || instancia.getExpediente() == null) return;
        var est = instancia.getExpediente().getEstudiante();
        if (est != null && est.getCorreo() != null) {
            String html = "<p>Estimado/a <strong>" + est.getNombre() + "</strong>,</p>"
                    + "<p>Tu plan de práctica ha sido <strong>" + descripcion + "</strong>.</p>";
            emailService.notificarAsignacion(est.getCorreo(), est.getNombre(), html, "Actualización de plan de práctica");
        }
    }

    private String[] guardarArchivoPlan(Long instanciaId, MultipartFile archivo) {
        if (archivo.getSize() > MAX_DOCUMENTO_BYTES)
            throw new OperacionNoPermitidaException("El archivo supera el tamaño máximo de 10 MB.");
        String mime = archivo.getContentType() != null ? archivo.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!MIME_PERMITIDOS_PLAN.contains(mime))
            throw new OperacionNoPermitidaException("Tipo de archivo no permitido. Solo PDF, DOC o DOCX.");
        String originalName = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "plan";
        String safeName = UUID.randomUUID() + "_" + Paths.get(originalName).getFileName();
        Path baseDir = Paths.get("uploads", "planes-practica", String.valueOf(instanciaId));
        Path target = baseDir.resolve(safeName);
        try {
            Files.createDirectories(baseDir);
            Files.write(target, archivo.getBytes());
        } catch (IOException e) {
            throw new OperacionNoPermitidaException("No fue posible guardar el archivo: " + e.getMessage());
        }
        return new String[]{originalName, target.toString().replace('\\', '/')};
    }

    private void eliminarArchivoPlan(String rutaArchivo) {
        try {
            Files.deleteIfExists(Paths.get(rutaArchivo));
        } catch (IOException e) {
            log.warn("No se pudo eliminar archivo del plan: {}", rutaArchivo);
        }
    }

    private String determinarContentType(String nombreArchivo) {
        if (nombreArchivo == null) return "application/octet-stream";
        String lower = nombreArchivo.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        return "application/octet-stream";
    }

    private boolean esNoVacio(String s) {
        return s != null && !s.isBlank();
    }

    private void registrarAuditoria(CustomUserDetails actor, TipoAccion accion, Long registroId, String tipo, String valores) {
        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .modulo("PlanPracticaService")
                .tipoAccion(accion)
                .registroAfectadoId(registroId)
                .registroAfectadoTipo(tipo)
                .valoresNuevos(valores));
    }
}
