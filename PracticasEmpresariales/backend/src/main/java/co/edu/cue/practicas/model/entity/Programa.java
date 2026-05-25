package co.edu.cue.practicas.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "programas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Programa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facultad_id", nullable = false)
    private Facultad facultad;

    @Column(name = "numero_total_practicas", nullable = false)
    @Builder.Default
    private int numeroTotalPracticas = 1;

    @Column(name = "promedio_minimo_general")
    @Builder.Default
    private double promedioMinimoGeneral = 3.0;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "programa", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<RequisitosPractica> requisitos = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
