package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.model.enums.EtiquetaCargo;
import co.edu.cue.practicas.model.enums.EstadoCuenta;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", indexes = {
        @Index(name = "idx_usuario_correo", columnList = "correo", unique = true),
        @Index(name = "idx_usuario_rol", columnList = "rol")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 20)
    private String telefono;

    @Column(name = "foto_perfil", length = 500)
    private String fotoPerfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    /** Solo aplica al rol COORDINACION_ACADEMICA. Informativa, no altera permisos. */
    @Enumerated(EnumType.STRING)
    @Column(name = "etiqueta_cargo", length = 30)
    private EtiquetaCargo etiquetaCargo;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Obliga al usuario a cambiar la contraseña temporal en el primer login */
    @Column(name = "primer_ingreso", nullable = false)
    @Builder.Default
    private boolean primerIngreso = true;

    /**
     * PENDIENTE mientras el usuario nunca haya iniciado sesión.
     * Cambia a ACTIVO en el primer login exitoso.
     * Permite al DTI ver quién ya accedió con su password temporal.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false, length = 15)
    @Builder.Default
    private EstadoCuenta estadoCuenta = EstadoCuenta.PENDIENTE;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    // ===== SCOPES según rol =====

    /** Scope para COORDINACION_ACADEMICA */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facultad_id")
    private Facultad facultad;

    /** Scope para COORDINADOR_PRACTICAS */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id")
    private Programa programa;

    // ===== Atributos exclusivos de ESTUDIANTE =====

    /** Estado del estudiante. Solo aplica cuando rol = ESTUDIANTE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_estudiante", length = 15)
    @Builder.Default
    private EstadoEstudiante estadoEstudiante = EstadoEstudiante.NO_APTO;

    /** Motivo obligatorio cuando el estudiante se mantiene en NO_APTO */
    @Column(name = "motivo_no_apto", length = 500)
    private String motivoNoApto;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
