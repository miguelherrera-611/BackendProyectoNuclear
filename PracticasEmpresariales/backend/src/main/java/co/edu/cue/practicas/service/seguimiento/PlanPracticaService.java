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
import co.edu.cue.practicas.model.entity.TutorEmpresarial;
import co.edu.cue.practicas.model.enums.EstadoPlan;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.PlanPracticaRepository;
import co.edu.cue.practicas.repository.tutor.TutorEmpresarialRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.notificacion.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
@Service
@RequiredArgsConstructor
public class PlanPracticaService {

    private final PlanPracticaRepository planRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final TutorEmpresarialRepository tutorRepository;
    private final AuditoriaLogger auditoriaLogger;
    private final EmailService emailService;

    @Transactional
    public PlanPracticaResponse crearOActualizarPlan(Long instanciaId, CrearPlanRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.ESTUDIANTE)
            throw new AccesoNoAutorizadoException("Solo el estudiante puede crear o actualizar su plan de práctica.");

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
                    .objetivos(req.getObjetivos())
                    .cronograma(req.getCronograma())
                    .cargadoPorId(actor.getId())
                    .build();
        } else {
            plan.setObjetivos(req.getObjetivos());
            plan.setCronograma(req.getCronograma());
            if (plan.getEstado() == EstadoPlan.RECHAZADO) plan.resubmit();
        }

        planRepository.save(plan);
        registrarAuditoria(actor, TipoAccion.CREAR, plan.getId(), "PlanPractica",
                "{\"instanciaId\":" + instanciaId + ",\"accion\":\"cargado\"}");

        return PlanPracticaResponse.desde(plan);
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
        TutorEmpresarial tutor = tutorRepository.findByCorreoAndActivoTrue(actor.getUsername()).orElse(null);
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
