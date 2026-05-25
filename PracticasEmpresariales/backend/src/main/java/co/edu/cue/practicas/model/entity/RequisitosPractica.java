package co.edu.cue.practicas.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "requisitos_practica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitosPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    /** Número de práctica al que aplica este requisito (1, 2, 3...) */
    @Column(name = "numero_practica", nullable = false)
    private int numeroPractica;

    @Column(name = "creditos_minimos", nullable = false)
    @Builder.Default
    private int creditosMinimos = 0;

    @Column(name = "promedio_minimo", nullable = false)
    @Builder.Default
    private double promedioMinimo = 0.0;

    @Column(name = "requiere_practica_anterior_aprobada", nullable = false)
    @Builder.Default
    private boolean requierePracticaAnteriorAprobada = false;

    /** Documentos requeridos separados por coma */
    @Column(name = "documentos_requeridos", length = 1000)
    private String documentosRequeridos;
}
