package co.edu.cue.practicas.service.seguimiento;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearSeguimientoRequest;
import co.edu.cue.practicas.dto.request.ObservacionDocenteRequest;
import co.edu.cue.practicas.dto.response.SeguimientoSemanalResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.SeguimientoSemanal;
import co.edu.cue.practicas.model.enums.EstadoPlan;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.PlanPracticaRepository;
import co.edu.cue.practicas.repository.seguimiento.SeguimientoSemanalRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.notificacion.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * GPE-168 / GPE-170 — Servicio de seguimientos semanales.
 *
 * PATRON PROXY: semanas anteriores revisadas son inmutables para el estudiante.
 * PATRON OBSERVER: nuevo seguimiento notifica al docente asesor.
 * PATRON DECORATOR (lógico): entrada base + evidencias + observaciones docente.
 *
 * SOLID — SRP: solo gestiona el ciclo de vida de los seguimientos semanales.
 * OCL: soloUltimoEditable, requierePlanAprobado, semanaUnica, soloCalificaSusEstudiantes.
 */
@Service
@RequiredArgsConstructor
public class SeguimientoSemanalService {

    private static final String MSG_INSTANCIA_NO_ENCONTRADA = "Instancia de practica no encontrada.";

    private final SeguimientoSemanalRepository seguimientoRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final PlanPracticaRepository planRepository;
    private final EvaluacionFinalRepository evaluacionFinalRepository;
    private final AuditoriaLogger auditoriaLogger;
    private final EmailService emailService;

    @Transactional
    public SeguimientoSemanalResponse crearSeguimiento(Long instanciaId, CrearSeguimientoRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.ESTUDIANTE)
            throw new AccesoNoAutorizadoException("Solo el estudiante puede crear seguimientos semanales.");

        verificarPracticaNoCongelada(instanciaId);
        InstanciaPractica instancia = buscarInstanciaEnCurso(instanciaId);
        verificarPropiedadEstudiante(instancia, actor.getId());
        verificarPlanAprobado(instanciaId);

        if (seguimientoRepository.existsByInstanciaPractica_IdAndSemana(instanciaId, req.getSemana()))
            throw new OperacionNoPermitidaException("Ya existe un seguimiento para la semana " + req.getSemana() + ".");

        SeguimientoSemanal seguimiento = SeguimientoSemanal.builder()
                .instanciaPractica(instancia)
                .semana(req.getSemana())
                .actividades(req.getActividades())
                .logros(req.getLogros())
                .dificultades(req.getDificultades())
                .evidencias(req.getEvidencias())
                .creadoPorId(actor.getId())
                .build();

        seguimientoRepository.save(seguimiento);
        registrarAuditoria(actor, TipoAccion.CREAR, seguimiento.getId(), "SeguimientoSemanal",
                "{\"instanciaId\":" + instanciaId + ",\"semana\":" + req.getSemana() + "}");
        notificarNuevoSeguimiento(instancia, req.getSemana());
        return SeguimientoSemanalResponse.desde(seguimiento);
    }

    @Transactional
    public SeguimientoSemanalResponse editarSeguimiento(Long seguimientoId, CrearSeguimientoRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.ESTUDIANTE)
            throw new AccesoNoAutorizadoException("Solo el estudiante puede editar sus seguimientos.");

        SeguimientoSemanal seguimiento = buscarSeguimiento(seguimientoId);
        verificarPracticaNoCongelada(seguimiento.getInstanciaPractica().getId());
        verificarPropiedadEstudiante(seguimiento.getInstanciaPractica(), actor.getId());

        // OCL: soloUltimoEditable
        SeguimientoSemanal ultimo = seguimientoRepository
                .findTopByInstanciaPractica_IdOrderBySemanaDesc(seguimiento.getInstanciaPractica().getId())
                .orElse(null);
        if (ultimo == null || !ultimo.getId().equals(seguimientoId))
            throw new OperacionNoPermitidaException("Solo el seguimiento de la semana más reciente puede editarse.");

        if (!seguimiento.esEditable())
            throw new OperacionNoPermitidaException("Solo se puede editar un seguimiento en estado RECHAZADO.");

        seguimiento.setActividades(req.getActividades());
        seguimiento.setLogros(req.getLogros());
        seguimiento.setDificultades(req.getDificultades());
        seguimiento.setEvidencias(req.getEvidencias());
        seguimiento.resubmit();
        seguimientoRepository.save(seguimiento);

        registrarAuditoria(actor, TipoAccion.EDITAR, seguimientoId, "SeguimientoSemanal",
                "{\"semana\":" + seguimiento.getSemana() + ",\"accion\":\"resubmit\"}");
        return SeguimientoSemanalResponse.desde(seguimiento);
    }

    @Transactional
    public List<SeguimientoSemanalResponse> listarPorInstancia(Long instanciaId, CustomUserDetails actor) {
        verificarAccesoSeguimientos(instanciaId, actor);
        return seguimientoRepository.findByInstanciaPractica_IdOrderBySemanaAsc(instanciaId)
                .stream()
                .map(SeguimientoSemanalResponse::desde)
                .toList();
    }

    @Transactional
    public SeguimientoSemanalResponse marcarRevisado(Long seguimientoId, CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR)
            throw new AccesoNoAutorizadoException("Solo el docente asesor puede marcar seguimientos como revisados.");

        SeguimientoSemanal seguimiento = buscarSeguimiento(seguimientoId);
        verificarPracticaNoCongelada(seguimiento.getInstanciaPractica().getId());
        verificarDocenteAsignado(seguimiento, actor.getId());

        seguimiento.revisar(actor.getId());
        seguimientoRepository.save(seguimiento);

        registrarAuditoria(actor, TipoAccion.CAMBIO_ESTADO, seguimientoId, "SeguimientoSemanal",
                "{\"estado\":\"REVISADO\",\"semana\":" + seguimiento.getSemana() + "}");
        notificarEstadoSeguimiento(seguimiento, "revisado por el docente");
        return SeguimientoSemanalResponse.desde(seguimiento);
    }

    @Transactional
    public SeguimientoSemanalResponse aprobar(Long seguimientoId, CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR)
            throw new AccesoNoAutorizadoException("Solo el docente asesor puede aprobar seguimientos.");

        SeguimientoSemanal seguimiento = buscarSeguimiento(seguimientoId);
        verificarDocenteAsignado(seguimiento, actor.getId());

        seguimiento.aprobar(actor.getId());
        seguimientoRepository.save(seguimiento);

        registrarAuditoria(actor, TipoAccion.CAMBIO_ESTADO, seguimientoId, "SeguimientoSemanal",
                "{\"estado\":\"APROBADO\",\"semana\":" + seguimiento.getSemana() + "}");
        notificarEstadoSeguimiento(seguimiento, "aprobado");
        return SeguimientoSemanalResponse.desde(seguimiento);
    }

    @Transactional
    public SeguimientoSemanalResponse rechazar(Long seguimientoId, ObservacionDocenteRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR)
            throw new AccesoNoAutorizadoException("Solo el docente asesor puede rechazar seguimientos.");

        SeguimientoSemanal seguimiento = buscarSeguimiento(seguimientoId);
        verificarPracticaNoCongelada(seguimiento.getInstanciaPractica().getId());
        verificarDocenteAsignado(seguimiento, actor.getId());

        seguimiento.rechazar(req.getObservacion(), actor.getId());
        seguimientoRepository.save(seguimiento);

        registrarAuditoria(actor, TipoAccion.CAMBIO_ESTADO, seguimientoId, "SeguimientoSemanal",
                "{\"estado\":\"RECHAZADO\",\"semana\":" + seguimiento.getSemana() + "}");
        notificarEstadoSeguimiento(seguimiento, "rechazado con observaciones: " + req.getObservacion());
        return SeguimientoSemanalResponse.desde(seguimiento);
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    private InstanciaPractica buscarInstanciaEnCurso(Long instanciaId) {
        InstanciaPractica i = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_INSTANCIA_NO_ENCONTRADA));
        if (i.getEstado() != EstadoPractica.EN_CURSO)
            throw new OperacionNoPermitidaException("Los seguimientos solo pueden registrarse en practicas EN_CURSO.");
        return i;
    }

    private SeguimientoSemanal buscarSeguimiento(Long id) {
        return seguimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Seguimiento semanal no encontrado."));
    }

    private void verificarPlanAprobado(Long instanciaId) {
        boolean aprobado = planRepository.existsByInstanciaPractica_IdAndEstado(instanciaId, EstadoPlan.APROBADO_DOCENTE);
        if (!aprobado)
            throw new OperacionNoPermitidaException("El plan de practica debe estar aprobado por el docente antes de iniciar seguimientos.");
    }

    private void verificarPropiedadEstudiante(InstanciaPractica instancia, Long estudianteId) {
        Long ownerId = instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null
                ? instancia.getExpediente().getEstudiante().getId() : null;
        if (!estudianteId.equals(ownerId))
            throw new AccesoNoAutorizadoException("Esta practica no pertenece a tu expediente.");
    }

    private void verificarDocenteAsignado(SeguimientoSemanal seg, Long docenteId) {
        InstanciaPractica instancia = seg.getInstanciaPractica();
        if (instancia.getDocenteAsesor() == null || !instancia.getDocenteAsesor().getId().equals(docenteId))
            throw new AccesoNoAutorizadoException("Solo el docente asesor asignado puede revisar este seguimiento.");
    }

    private void verificarAccesoSeguimientos(Long instanciaId, CustomUserDetails actor) {
        if (actor.getRol() == Rol.ESTUDIANTE) {
            instanciaRepository.findById(instanciaId).ifPresent(i -> {
                if (i.getExpediente() == null || !i.getExpediente().getEstudiante().getId().equals(actor.getId()))
                    throw new AccesoNoAutorizadoException("No tienes acceso a estos seguimientos.");
            });
        }
    }

    private void notificarNuevoSeguimiento(InstanciaPractica instancia, int semana) {
        if (instancia.getDocenteAsesor() != null && instancia.getDocenteAsesor().getCorreo() != null) {
            String html = "<p>Se ha registrado el seguimiento de la semana <strong>" + semana
                    + "</strong> y esta pendiente de tu revision.</p>";
            emailService.notificarAsignacion(instancia.getDocenteAsesor().getCorreo(),
                    instancia.getDocenteAsesor().getNombre(), html, "Nuevo seguimiento semanal pendiente");
        }
    }

    private void notificarEstadoSeguimiento(SeguimientoSemanal seg, String descripcion) {
        InstanciaPractica instancia = seg.getInstanciaPractica();
        if (instancia == null || instancia.getExpediente() == null) return;
        var est = instancia.getExpediente().getEstudiante();
        if (est != null && est.getCorreo() != null) {
            String html = "<p>Tu seguimiento de la semana <strong>" + seg.getSemana()
                    + "</strong> ha sido <strong>" + descripcion + "</strong>.</p>";
            emailService.notificarAsignacion(est.getCorreo(), est.getNombre(), html, "Estado de seguimiento semanal");
        }
    }

    private void verificarPracticaNoCongelada(Long instanciaId) {
        if (evaluacionFinalRepository.existsByInstanciaPractica_IdAndTipo(instanciaId, TipoEvaluacionFinal.DOCENTE_ASESOR))
            throw new OperacionNoPermitidaException("La práctica está calificada. No se pueden realizar más operaciones sobre seguimientos.");
    }

    private void registrarAuditoria(CustomUserDetails actor, TipoAccion accion, Long id, String tipo, String valores) {
        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .modulo("SeguimientoSemanalService")
                .tipoAccion(accion)
                .registroAfectadoId(id)
                .registroAfectadoTipo(tipo)
                .valoresNuevos(valores));
    }
}