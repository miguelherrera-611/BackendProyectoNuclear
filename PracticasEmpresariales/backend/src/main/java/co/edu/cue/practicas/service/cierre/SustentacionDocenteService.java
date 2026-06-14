package co.edu.cue.practicas.service.cierre;

import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import co.edu.cue.practicas.service.notificacion.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * SOLID — SRP: gestiona únicamente la programación de fecha de sustentación por el docente asesor.
 * PATRON OBSERVER: notifica al estudiante y tutor empresarial vía email al agendar o reagendar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SustentacionDocenteService {

    private final InstanciaPracticaRepository instanciaRepository;
    private final EmailService emailService;
    private final EstudianteMapper mapper;

    @Transactional
    public InstanciaPracticaResponse agendar(Long instanciaId, LocalDate fecha, CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR) {
            throw new AccesoNoAutorizadoException("Solo el Docente Asesor puede programar la sustentacion.");
        }

        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));

        if (instancia.getDocenteAsesor() == null || !instancia.getDocenteAsesor().getId().equals(actor.getId())) {
            throw new AccesoNoAutorizadoException("No eres el docente asesor asignado a esta practica.");
        }

        if (instancia.getEstado() != EstadoPractica.EN_CURSO) {
            throw new OperacionNoPermitidaException(
                "Solo se puede programar la sustentacion de practicas EN_CURSO. Estado actual: " + instancia.getEstado());
        }

        if (fecha.isBefore(LocalDate.now())) {
            throw new OperacionNoPermitidaException("La fecha de sustentacion no puede ser una fecha pasada.");
        }

        if (instancia.getFechaFin() != null && fecha.isAfter(instancia.getFechaFin())) {
            String limite = instancia.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            throw new OperacionNoPermitidaException(
                "La fecha de sustentacion no puede superar la fecha fin de la practica (" + limite + ").");
        }

        boolean esReagendamiento = instancia.getFechaSustentacion() != null;
        instancia.setFechaSustentacion(fecha);
        instanciaRepository.save(instancia);

        notificarActores(instancia, fecha, esReagendamiento);

        return mapper.toInstanciaPracticaResponse(instancia);
    }

    public InstanciaPracticaResponse obtener(Long instanciaId, CustomUserDetails actor) {
        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));

        if (actor.getRol() == Rol.DOCENTE_ASESOR) {
            if (instancia.getDocenteAsesor() == null || !instancia.getDocenteAsesor().getId().equals(actor.getId())) {
                throw new AccesoNoAutorizadoException("No eres el docente asesor asignado a esta practica.");
            }
        }

        return mapper.toInstanciaPracticaResponse(instancia);
    }

    private void notificarActores(InstanciaPractica instancia, LocalDate fecha, boolean esReagendamiento) {
        String fechaFmt = fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String nombrePractica = instancia.getNombre();
        String accion = esReagendamiento ? "reagendada" : "programada";
        String asunto = (esReagendamiento ? "Reagendamiento" : "Programación") + " de Sustentación — " + nombrePractica;

        try {
            var estudiante = instancia.getExpediente().getEstudiante();
            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2 style="color: #1a365d;">Sustentación %s</h2>
                        <p>Estimado/a <strong>%s</strong>,</p>
                        <p>Tu sustentación de la práctica <strong>%s</strong> ha sido <strong>%s</strong>.</p>
                        <div style="background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 8px; padding: 20px; margin: 20px 0;">
                            <p style="margin: 0; font-size: 18px;"><strong>📅 Fecha de sustentación: %s</strong></p>
                        </div>
                        <p>Tu docente asesor te contactará con los detalles adicionales.</p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                        <p style="color: #718096; font-size: 12px;">Este es un mensaje automático del sistema de prácticas.</p>
                    </div>
                    """.formatted(accion, estudiante.getNombre(), nombrePractica, accion, fechaFmt);
            emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, asunto);
        } catch (Exception e) {
            log.warn("[SUSTENTACION-DOCENTE] No se pudo notificar al estudiante: {}", e.getMessage());
        }

        try {
            var tutor = instancia.getTutorEmpresarial();
            if (tutor != null && tutor.getCorreo() != null) {
                String html = """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                            <h2 style="color: #1a365d;">Sustentación %s</h2>
                            <p>Estimado/a <strong>%s</strong>,</p>
                            <p>Le informamos que la sustentación de la práctica <strong>%s</strong> ha sido <strong>%s</strong>.</p>
                            <div style="background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 8px; padding: 20px; margin: 20px 0;">
                                <p style="margin: 0; font-size: 18px;"><strong>📅 Fecha de sustentación: %s</strong></p>
                            </div>
                            <p>Quedamos a su disposición para cualquier consulta.</p>
                            <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                            <p style="color: #718096; font-size: 12px;">Este es un mensaje automático del sistema de prácticas.</p>
                        </div>
                        """.formatted(accion, tutor.getNombre(), nombrePractica, accion, fechaFmt);
                emailService.notificarAsignacion(tutor.getCorreo(), tutor.getNombre(), html, asunto);
            }
        } catch (Exception e) {
            log.warn("[SUSTENTACION-DOCENTE] No se pudo notificar al tutor: {}", e.getMessage());
        }
    }
}
