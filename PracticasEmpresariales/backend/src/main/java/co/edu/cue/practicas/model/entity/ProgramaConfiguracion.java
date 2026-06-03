package co.edu.cue.practicas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "programa_configuraciones",
        indexes = {
                @Index(name = "idx_config_programa_vigente", columnList = "programa_id,vigente"),
                @Index(name = "idx_config_programa_fecha", columnList = "programa_id,creado_en")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramaConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    @Column(name = "numero_practicas", nullable = false)
    private int numeroPracticas;

    @Column(name = "semanas_seguimiento", nullable = false)
    private int semanasSeguimiento;

    @Column(name = "nota_minima_aprobacion", nullable = false)
    private double notaMinimaAprobacion;

    @Column(name = "requisitos_cierre", length = 1500)
    private String requisitosCierre;

    @Column(name = "umbral_inactividad_dias", nullable = false)
    private int umbralInactividadDias;

    @Column(name = "vigente", nullable = false)
    @Builder.Default
    private boolean vigente = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
