package co.edu.cue.practicas.audit.singleton;

import co.edu.cue.practicas.event.AuditoriaEvent;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.auditoria.BitacoraAuditoriaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * PATRON SINGLETON — GPE-139
 *
 * Logger global de auditoría. Instancia única que recibe eventos
 * de todos los módulos del sistema. Centraliza el registro de
 * acciones críticas garantizando trazabilidad completa.
 *
 * La bitácora es INMUTABLE: no se puede modificar ni eliminar
 * ninguna entrada una vez creada.
 */
@Slf4j
@Component
public class AuditoriaLogger {

    private static volatile AuditoriaLogger instance;

    private final BitacoraAuditoriaRepository bitacoraRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AuditoriaLogger(BitacoraAuditoriaRepository bitacoraRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.bitacoraRepository = bitacoraRepository;
        this.eventPublisher = eventPublisher;
        instance = this;
    }

    /** Punto de acceso global — garantiza instancia única */
    public static AuditoriaLogger getInstance() {
        return instance;
    }

    /**
     * Registra una acción en la bitácora.
     * Si falla el registro, lanza excepción para abortar la acción original
     * (regla de negocio: el sistema no puede continuar sin registrar la auditoría).
     */
    public void registrar(BitacoraAuditoria.BitacoraAuditoriaBuilder builder) {
        try {
            BitacoraAuditoria entrada = builder.build();
            bitacoraRepository.save(entrada);

            // Publica el evento para que los observadores reaccionen (Patrón Observer)
            eventPublisher.publishEvent(new AuditoriaEvent(this, entrada));

            log.info("[AUDITORIA] {} | {} | {} | ID:{} | Exitoso:{}",
                    entrada.getFechaHora(),
                    entrada.getNombreUsuario(),
                    entrada.getTipoAccion(),
                    entrada.getRegistroAfectadoId(),
                    entrada.isExitoso());

        } catch (Exception e) {
            log.error("[AUDITORIA-ERROR] No se pudo registrar la acción: {}", e.getMessage());
            throw new RuntimeException("Error crítico: no se pudo registrar en la bitácora de auditoría. Acción abortada.", e);
        }
    }

    /** Atajo para registrar intentos de acceso no autorizado */
    public void registrarAccesoNegado(Usuario usuario, String modulo, String ipOrigen) {
        BitacoraAuditoria entrada = BitacoraAuditoria.builder()
                .usuario(usuario)
                .nombreUsuario(usuario != null ? usuario.getNombre() : "Anónimo")
                .rolUsuario(usuario != null ? usuario.getRol() : null)
                .etiquetaCargoUsuario(usuario != null ? usuario.getEtiquetaCargo() : null)
                .modulo(modulo)
                .tipoAccion(TipoAccion.ACCESO_NO_AUTORIZADO)
                .ipOrigen(ipOrigen)
                .exitoso(false)
                .motivoFallo("Intento de acceso a módulo/recurso no autorizado")
                .build();

        bitacoraRepository.save(entrada);
        log.warn("[AUDITORIA-SEGURIDAD] Acceso negado: usuario={} módulo={} ip={}",
                entrada.getNombreUsuario(), modulo, ipOrigen);
    }
}
