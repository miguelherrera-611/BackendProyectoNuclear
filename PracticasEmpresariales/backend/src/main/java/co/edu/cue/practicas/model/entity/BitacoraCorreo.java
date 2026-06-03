package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.model.enums.EstadoEnvioCorreo;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_correos", indexes = {
        @Index(name = "idx_correo_actor_fecha", columnList = "actor_id,fecha_envio"),
        @Index(name = "idx_correo_evento", columnList = "tipo_evento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitacoraCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 50)
    private TipoEventoNotificacion tipoEvento;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "destinatario", nullable = false, length = 180)
    private String destinatario;

    @Column(name = "asunto", nullable = false, length = 180)
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEnvioCorreo estado;

    @Column(name = "detalle", length = 1000)
    private String detalle;

    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEnvio = LocalDateTime.now();
}
