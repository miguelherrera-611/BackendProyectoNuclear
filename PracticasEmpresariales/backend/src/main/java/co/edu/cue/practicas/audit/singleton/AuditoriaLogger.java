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
 *
 * Todos los servicios del sistema (AuthService, UsuarioService, FacultadService,
 * ProgramaService, ScopeValidationAspect) pasan por aquí para registrar
 * sus operaciones. Así se garantiza que ninguna acción quede sin traza.
 *
 * Regla de negocio: si falla el registro en la bitácora, la operación
 * original también se aborta (se lanza excepción). El sistema no puede
 * continuar operando sin registrar lo que hizo.
 */
@Slf4j
@Component
public class AuditoriaLogger {

    // Repositorio JPA para persistir cada entrada de la bitácora en la base de datos
    private final BitacoraAuditoriaRepository bitacoraRepository;

    // Publicador de eventos de Spring (PATRON OBSERVER): después de registrar en BD,
    // se emite un evento para que los listeners reaccionen (ej. alertas de seguridad)
    private final ApplicationEventPublisher eventPublisher;

    public AuditoriaLogger(BitacoraAuditoriaRepository bitacoraRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.bitacoraRepository = bitacoraRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registra una acción en la bitácora de auditoría.
     *
     * Recibe un Builder parcialmente construido desde el servicio llamador
     * (con usuario, módulo, tipo de acción, etc.) y lo finaliza aquí.
     * Después de guardar en BD, publica un AuditoriaEvent para que los
     * observadores (AuditoriaEventListener) puedan reaccionar (ej. alertas).
     *
     * Si falla el guardado en BD, lanza RuntimeException para abortar
     * la transacción del servicio llamador. Esto garantiza que nunca
     * se ejecute una acción sin que quede registrada.
     *
     * @param builder  builder de BitacoraAuditoria con los campos completados por el servicio
     */
    public void registrar(BitacoraAuditoria.BitacoraAuditoriaBuilder builder) {
        try {
            // Construimos el objeto final y lo persistimos en la base de datos
            BitacoraAuditoria entrada = builder.build();
            registrar(entrada);

        } catch (Exception e) {
            log.error("[AUDITORIA-ERROR] No se pudo registrar la acción: {}", e.getMessage());
            // Lanzamos excepción para que la transacción del servicio llamador se revierta
            throw new RuntimeException("Error crítico: no se pudo registrar en la bitácora de auditoría. Acción abortada.", e);
        }
    }

    /**
     * Sobrecarga conveniente para servicios que ya construyeron la entidad completa.
     */
    public void registrar(BitacoraAuditoria entrada) {
        bitacoraRepository.save(entrada);
        eventPublisher.publishEvent(new AuditoriaEvent(this, entrada));
        log.info("[AUDITORIA] {} | {} | {} | ID:{} | Exitoso:{}",
                entrada.getFechaHora(),
                entrada.getNombreUsuario(),
                entrada.getTipoAccion(),
                entrada.getRegistroAfectadoId(),
                entrada.isExitoso());
    }

    /**
     * Método especializado para registrar accesos no autorizados.
     * Lo usa el ScopeValidationAspect (Proxy) cuando detecta que un usuario
     * intenta ejecutar una operación para la que no tiene permiso.
     *
     * A diferencia de registrar(), este método no lanza excepción si falla,
     * porque ya se va a lanzar un 403 desde el Proxy de todas formas.
     *
     * @param usuario   usuario que intentó el acceso (puede ser null si no está autenticado)
     * @param modulo    nombre del módulo o método al que intentó acceder
     * @param ipOrigen  IP del cliente para trazabilidad
     */
    public void registrarAccesoNegado(Usuario usuario, String modulo, String ipOrigen) {
        BitacoraAuditoria entrada = BitacoraAuditoria.builder()
                .usuario(usuario)
                // Si el usuario es null (no autenticado), usamos "Anónimo" como nombre
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
