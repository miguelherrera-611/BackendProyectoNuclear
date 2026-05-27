package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bitácora de auditoría inmutable. Nunca se edita ni elimina.
 * Registra toda acción crítica del sistema.
 */
@Entity
@Table(name = "bitacora_auditoria", indexes = {
        @Index(name = "idx_bitacora_fecha", columnList = "fecha_hora"),
        @Index(name = "idx_bitacora_usuario", columnList = "usuario_id"),
        @Index(name = "idx_bitacora_tipo", columnList = "tipo_accion")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que ejecutó la acción (puede ser null si no está autenticado) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Nombre del usuario en el momento de la acción (persiste aunque se desactive) */
    @Column(name = "nombre_usuario", length = 200)
    private String nombreUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_usuario", length = 30)
    private Rol rolUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "etiqueta_cargo_usuario", length = 30)
    private EtiquetaCargo etiquetaCargoUsuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(nullable = false, length = 100)
    private String modulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_accion", nullable = false, length = 30)
    private TipoAccion tipoAccion;

    @Column(name = "registro_afectado_id")
    private Long registroAfectadoId;

    @Column(name = "registro_afectado_tipo", length = 100)
    private String registroAfectadoTipo;

    /** JSON con los valores antes del cambio */
    @Column(name = "valores_anteriores", columnDefinition = "TEXT")
    private String valoresAnteriores;

    /** JSON con los valores después del cambio */
    @Column(name = "valores_nuevos", columnDefinition = "TEXT")
    private String valoresNuevos;

    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    @Column(name = "exitoso", nullable = false)
    @Builder.Default
    private boolean exitoso = true;

    @Column(name = "motivo_fallo", length = 500)
    private String motivoFallo;
}
