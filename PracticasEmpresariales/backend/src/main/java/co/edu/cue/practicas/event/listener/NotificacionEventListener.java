package co.edu.cue.practicas.event.listener;

import co.edu.cue.practicas.event.UsuarioCreadoEvent;
import co.edu.cue.practicas.event.AsignacionCreadaEvent;
import co.edu.cue.practicas.event.AsignacionCanceladaEvent;
import co.edu.cue.practicas.event.VinculacionConfirmadaEvent;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.service.notificacion.EmailService;
import co.edu.cue.practicas.service.notificacion.NotificacionConfigurableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * PATRON OBSERVER — GPE-136, GPE-139
 *
 * Observador que reacciona a eventos de dominio publicados por los servicios.
 * Se ejecuta de forma asíncrona (@Async) para no bloquear el hilo principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionEventListener {

    private static final String SALUDO_HTML     = "<p>Estimado/a ";
    private static final String EMPRESA_DEFAULT = "(empresa)";
    private static final String ESTUDIANTE_DEFAULT = "(estudiante)";

    private final EmailService emailService;
    private final NotificacionConfigurableService notificacionService;

    @EventListener
    @Async
    public void manejarUsuarioCreado(UsuarioCreadoEvent evento) {
        var usuario = evento.getUsuario();
        var password = evento.getPasswordTemporal();

        log.info("[DEV] Contrasena temporal para {} ({}): {}", usuario.getNombre(), usuario.getCorreo(), password);

        emailService.enviarPasswordTemporal(usuario.getCorreo(), usuario.getNombre(), password);

        if (Rol.ESTUDIANTE.equals(usuario.getRol())) {
            log.info("[OBSERVER] Nuevo estudiante creado: {} — notificando a Coordinacion Academica", usuario.getNombre());
            emailService.notificarNuevoEstudiante(usuario);
        }
    }

    @EventListener
    @Async
    public void manejarAsignacionCreada(AsignacionCreadaEvent evento) {
        var instancia = evento.getInstancia();
        if (instancia == null) return;

        try {
            if (instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null) {
                var estudiante = instancia.getExpediente().getEstudiante();
                if (estudiante.getCorreo() != null) {
                    String nombreEmpresa = instancia.getEmpresa() != null
                            ? instancia.getEmpresa().getRazonSocial() : EMPRESA_DEFAULT;
                    String html = SALUDO_HTML + estudiante.getNombre() + ",</p>"
                            + "<p>Has sido asignado a la vacante en la empresa <strong>" + nombreEmpresa
                            + "</strong>. Por favor revisa el sistema para mas detalles.</p>";
                    emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, "Asignacion a vacante");
                }
            }

            var empresa = instancia.getEmpresa();
            if (empresa != null && empresa.getCorreo() != null) {
                String nombreEstudiante = instancia.getExpediente() != null
                        && instancia.getExpediente().getEstudiante() != null
                        ? instancia.getExpediente().getEstudiante().getNombre() : ESTUDIANTE_DEFAULT;
                String htmlEmpresa = "<p>Se ha asignado al estudiante <strong>" + nombreEstudiante
                        + "</strong> a la vacante publicada en su empresa.</p>";
                emailService.notificarAsignacion(empresa.getCorreo(), empresa.getRazonSocial(), htmlEmpresa, "Nuevo practicante asignado");
            }
        } catch (Exception e) {
            log.error("[NOTIFICACION] Error al procesar evento AsignacionCreada: {}", e.getMessage());
        }
    }

    @EventListener
    @Async
    public void manejarAsignacionCancelada(AsignacionCanceladaEvent evento) {
        var instancia = evento.getInstancia();
        if (instancia == null) return;

        try {
            if (instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null) {
                var estudiante = instancia.getExpediente().getEstudiante();
                if (estudiante.getCorreo() != null) {
                    String nombreEmpresa = instancia.getEmpresa() != null
                            ? instancia.getEmpresa().getRazonSocial() : EMPRESA_DEFAULT;
                    String html = SALUDO_HTML + estudiante.getNombre() + ",</p>"
                            + "<p>Tu asignacion a la vacante en la empresa <strong>" + nombreEmpresa
                            + "</strong> ha sido cancelada. Por favor contacta a tu coordinador para mas informacion.</p>";
                    emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, "Asignacion cancelada");
                }
            }
            var empresa = instancia.getEmpresa();
            if (empresa != null && empresa.getCorreo() != null) {
                String nombreEstudiante = instancia.getExpediente() != null
                        && instancia.getExpediente().getEstudiante() != null
                        ? instancia.getExpediente().getEstudiante().getNombre() : ESTUDIANTE_DEFAULT;
                String htmlEmpresa = "<p>Se ha cancelado la asignacion del estudiante <strong>" + nombreEstudiante
                        + "</strong> a la vacante publicada en su empresa.</p>";
                emailService.notificarAsignacion(empresa.getCorreo(), empresa.getRazonSocial(), htmlEmpresa, "Asignacion cancelada");
            }
        } catch (Exception e) {
            log.error("[NOTIFICACION] Error al procesar evento AsignacionCancelada: {}", e.getMessage());
        }
    }

    @EventListener
    @Async
    public void manejarVinculacionConfirmada(VinculacionConfirmadaEvent evento) {
        var instancia = evento.getInstancia();
        if (instancia == null) return;

        try {
            if (instancia.getExpediente() == null || instancia.getExpediente().getEstudiante() == null) return;

            var estudiante = instancia.getExpediente().getEstudiante();

            Map<String, String> vars = new HashMap<>();
            vars.put("nombre_estudiante", estudiante.getNombre());
            vars.put("empresa",           instancia.getEmpresa() != null ? instancia.getEmpresa().getRazonSocial() : "");
            vars.put("nombre_practica",   instancia.getNombre());
            vars.put("fecha_inicio",      instancia.getFechaInicio() != null ? instancia.getFechaInicio().toString() : "");
            vars.put("fecha_fin",         instancia.getFechaFin()    != null ? instancia.getFechaFin().toString()    : "");
            vars.put("nombre_docente",    instancia.getDocenteAsesor() != null ? instancia.getDocenteAsesor().getNombre() : "");
            vars.put("nombre_tutor",      instancia.getTutorEmpresarial() != null ? instancia.getTutorEmpresarial().getNombre() : "");
            vars.put("enlace_encuesta",   "");
            vars.put("resultado",         "");
            vars.put("nota_final",        "");

            // Notifica al estudiante
            if (estudiante.getCorreo() != null) {
                notificacionService.enviar(TipoEventoNotificacion.INICIO_PRACTICA,
                        estudiante.getId(), estudiante.getCorreo(), estudiante.getNombre(), vars);
            }

            // Notifica al docente asesor
            if (instancia.getDocenteAsesor() != null && instancia.getDocenteAsesor().getCorreo() != null) {
                var docente = instancia.getDocenteAsesor();
                notificacionService.enviar(TipoEventoNotificacion.INICIO_PRACTICA,
                        docente.getId(), docente.getCorreo(), docente.getNombre(), vars);
            }

            // Notifica al tutor empresarial
            if (instancia.getTutorEmpresarial() != null && instancia.getTutorEmpresarial().getCorreo() != null) {
                var tutor = instancia.getTutorEmpresarial();
                notificacionService.enviar(TipoEventoNotificacion.INICIO_PRACTICA,
                        tutor.getId(), tutor.getCorreo(), tutor.getNombre(), vars);
            }
        } catch (Exception e) {
            log.error("[NOTIFICACION] Error al procesar evento VinculacionConfirmada: {}", e.getMessage());
        }
    }
}