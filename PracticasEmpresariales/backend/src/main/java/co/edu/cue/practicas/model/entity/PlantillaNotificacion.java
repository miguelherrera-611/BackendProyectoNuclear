package co.edu.cue.practicas.model.entity;

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
@Table(name = "plantillas_notificacion",
        indexes = @Index(name = "idx_plantilla_evento", columnList = "tipo_evento", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 50, unique = true)
    private TipoEventoNotificacion tipoEvento;

    @Column(name = "asunto", nullable = false, length = 180)
    private String asunto;

    @Column(name = "cuerpo", nullable = false, length = 5000)
    private String cuerpo;

    @Column(name = "roles_receptores", length = 400)
    private String rolesReceptores;

    @Column(name = "frecuencia_recordatorio_dias", nullable = false)
    private int frecuenciaRecordatorioDias;

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
