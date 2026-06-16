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

    /** Solo aplica al rol COORDINADOR_PRACTICAS. Informativa, no altera permisos. */
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

    /** Scope para COORDINADOR_PRACTICAS (y derivado para COORDINACION_ACADEMICA desde su programa) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facultad_id")
    private Facultad facultad;

    /** Scope para COORDINACION_ACADEMICA y ESTUDIANTE */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id")
    private Programa programa;

    /** Empresa a la que pertenece el tutor empresarial. Solo aplica a rol TUTOR_EMPRESARIAL. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // ===== Atributos exclusivos de ESTUDIANTE =====

    /** OCL: identificacionUnica — único en todo el sistema para estudiantes */
    @Column(name = "identificacion", unique = true, length = 20)
    private String identificacion;

    /** Semestre actual del estudiante al momento de crear el registro */
    @Column(name = "semestre")
    private Integer semestre;

    /** Contacto de emergencia: "Nombre - 3009876543" */
    @Column(name = "contacto_emergencia", length = 200)
    private String contactoEmergencia;

    /** Estado del estudiante. Solo aplica cuando rol = ESTUDIANTE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_estudiante", length = 15)
    @Builder.Default
    private EstadoEstudiante estadoEstudiante = EstadoEstudiante.NO_APTO;

    /** Motivo obligatorio cuando el estudiante se mantiene en NO_APTO */
    @Column(name = "motivo_no_apto", length = 500)
    private String motivoNoApto;

    /**
     * GPE-147 — indica que la Coordinación Académica ya envió formalmente
     * al estudiante al proceso de práctica. Solo cuando es true el
     * Coordinador de Prácticas puede ver al estudiante.
     * OCL: soloAptoPostulable — debe estar en estado APTO para poder enviarse.
     */
    @Column(name = "enviado_al_proceso", nullable = false)
    @Builder.Default
    private boolean enviadoAlProceso = false;

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
