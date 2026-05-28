package co.edu.cue.practicas.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * GPE-151 — Entidad TutorEmpresarial
 *
 * SOLID — SRP: solo contiene datos del tutor y la regla de desactivación.
 *              Validaciones externas → TutorEmpresarialValidator.
 *              Mapping → TutorEmpresarialMapper.
 */
@Entity
@Table(name = "tutores_empresariales", indexes = {
        @Index(name = "idx_tutor_correo", columnList = "correo", unique = true),
        @Index(name = "idx_tutor_empresa", columnList = "empresa_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorEmpresarial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @Email
    @NotBlank
    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "disponible", nullable = false)
    @Builder.Default
    private boolean disponible = true;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.actualizadoEn = LocalDateTime.now(); }

    /** OCL: PROHIBIDO eliminar — solo desactivar */
    public void desactivar() {
        this.activo = false;
        this.disponible = false;
    }
}
