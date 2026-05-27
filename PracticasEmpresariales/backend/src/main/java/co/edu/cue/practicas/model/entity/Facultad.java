package co.edu.cue.practicas.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facultades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facultad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime actualizadaEn = LocalDateTime.now();

    @OneToMany(mappedBy = "facultad", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Programa> programas = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.actualizadaEn = LocalDateTime.now();
    }

    public boolean tieneRecursosActivos() {
        return programas.stream().anyMatch(p -> p.isActivo());
    }
}
