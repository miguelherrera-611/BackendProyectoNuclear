package co.edu.cue.practicas.service.notificacion;

import co.edu.cue.practicas.dto.request.PlantillaNotificacionRequest;
import co.edu.cue.practicas.dto.response.PlantillaNotificacionResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.model.entity.BitacoraCorreo;
import co.edu.cue.practicas.model.entity.PlantillaNotificacion;
import co.edu.cue.practicas.model.enums.EstadoEnvioCorreo;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.notificacion.BitacoraCorreoRepository;
import co.edu.cue.practicas.repository.notificacion.PlantillaNotificacionRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificacionConfigurableService {

    // SPRINT 4 - Singleton: Spring expone una unica instancia para enviar y configurar correos.
    private final PlantillaNotificacionRepository plantillaRepository;
    private final BitacoraCorreoRepository bitacoraRepository;
    private final EmailService emailService;

    public java.util.List<PlantillaNotificacionResponse> listarPlantillas() {
        return plantillaRepository.findAll().stream()
                .map(PlantillaNotificacionResponse::desde)
                .toList();
    }

    public PlantillaNotificacionResponse obtenerPlantilla(TipoEventoNotificacion tipo) {
        return plantillaRepository.findByTipoEvento(tipo)
                .map(PlantillaNotificacionResponse::desde)
                .orElse(null);
    }

    @Transactional
    public void eliminarPlantilla(TipoEventoNotificacion tipo, CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador de practicas puede eliminar plantillas de correo.");
        }
        plantillaRepository.findByTipoEvento(tipo).ifPresent(plantillaRepository::delete);
    }

    @Transactional
    public PlantillaNotificacionResponse guardarPlantilla(PlantillaNotificacionRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador de practicas puede configurar plantillas de correo.");
        }
        PlantillaNotificacion plantilla = plantillaRepository.findByTipoEvento(req.getTipoEvento())
                .orElseGet(PlantillaNotificacion::new);
        plantilla.setTipoEvento(req.getTipoEvento());
        plantilla.setAsunto(req.getAsunto());
        plantilla.setCuerpo(req.getCuerpo());
        plantilla.setRolesReceptores(req.getRolesReceptores());
        plantilla.setFrecuenciaRecordatorioDias(req.getFrecuenciaRecordatorioDias());
        plantilla.setActualizadoEn(LocalDateTime.now());
        return PlantillaNotificacionResponse.desde(plantillaRepository.save(plantilla));
    }

    public String previsualizar(PlantillaNotificacionRequest req, Map<String, String> variables, CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador de practicas puede previsualizar plantillas.");
        }
        return aplicarVariables(req.getCuerpo(), variables);
    }

    public void enviar(TipoEventoNotificacion tipo, Long actorId, String destinatario, String nombre,
                       Map<String, String> variables) {
        // SPRINT 4 - Template Method: construir plantilla -> personalizar -> enviar SMTP -> registrar bitacora.
        PlantillaNotificacion plantilla = plantillaRepository.findByTipoEvento(tipo)
                .orElseGet(() -> plantillaPorDefecto(tipo));
        String asunto = aplicarVariables(plantilla.getAsunto(), variables);
        String cuerpo = aplicarVariables(plantilla.getCuerpo(), variables);
        try {
            // SPRINT 4 - Adapter: EmailService encapsula el servidor SMTP institucional externo.
            emailService.notificarAsignacion(destinatario, nombre, cuerpo, asunto);
            registrar(tipo, actorId, destinatario, asunto, EstadoEnvioCorreo.ENVIADO, "Envio programado via SMTP institucional.");
        } catch (Exception e) {
            registrar(tipo, actorId, destinatario, asunto, EstadoEnvioCorreo.FALLIDO, e.getMessage());
        }
    }

    public boolean puedeEnviarRecordatorio(Long actorId) {
        LocalDate hoy = LocalDate.now();
        return !bitacoraRepository.existsByActorIdAndFechaEnvioBetween(
                actorId, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay().minusNanos(1));
    }

    public boolean puedeEnviarRecordatorio(Long actorId, TipoEventoNotificacion tipo, LocalDate ultimoRecordatorio) {
        int frecuencia = plantillaRepository.findByTipoEvento(tipo)
                .map(PlantillaNotificacion::getFrecuenciaRecordatorioDias)
                .orElse(1);
        if (ultimoRecordatorio == null) {
            // Nunca se ha enviado recordatorio — verificar solo que no se haya enviado hoy
            LocalDate hoy = LocalDate.now();
            return !bitacoraRepository.existsByActorIdAndFechaEnvioBetween(
                    actorId, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay().minusNanos(1));
        }
        // Se enviaron recordatorios antes — respetar la frecuencia configurada
        return !LocalDate.now().isBefore(ultimoRecordatorio.plusDays(frecuencia));
    }

    private String aplicarVariables(String texto, Map<String, String> variables) {
        // SPRINT 4 - Decorator: agrega variables dinamicas sobre la plantilla base editable.
        String resultado = texto == null ? "" : texto;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                resultado = resultado.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return resultado;
    }

    private PlantillaNotificacion plantillaPorDefecto(TipoEventoNotificacion tipo) {
        return PlantillaNotificacion.builder()
                .tipoEvento(tipo)
                .asunto("Notificacion de practicas")
                .cuerpo("<p>{{nombre_estudiante}}</p><p>{{nombre_practica}}</p><p>{{resultado}} {{nota_final}}</p><p>{{enlace_encuesta}}</p>")
                .frecuenciaRecordatorioDias(1)
                .build();
    }

    private void registrar(TipoEventoNotificacion tipo, Long actorId, String destinatario, String asunto,
                           EstadoEnvioCorreo estado, String detalle) {
        bitacoraRepository.save(BitacoraCorreo.builder()
                .tipoEvento(tipo)
                .actorId(actorId)
                .destinatario(destinatario)
                .asunto(asunto)
                .estado(estado)
                .detalle(detalle)
                .build());
    }
}
