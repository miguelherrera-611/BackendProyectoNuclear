package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoSeguimiento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * GPE-168 / GPE-170 — Seguimiento semanal del estudiante.
 *
 * PATRON STATE: encapsula transiciones PENDIENTE → APROBADO / RECHAZADO.
 * PATRON PROXY: semanas anteriores ya revisadas son inmutables para el estudiante.
 * PATRON DECORATOR (conceptual): entrada base + evidencias + observaciones docente.
 *
 * SOLID — SRP: solo gestiona el estado y datos del seguimiento de una semana.
 * OCL: soloUltimoEditable — solo el de semana más reciente puede re-editarse si RECHAZADO.
 *      semanaUnica — número de semana único dentro de la misma instancia de práctica.
 *      requierePlanAprobado — no se puede crear sin plan en APROBADO_DOCENTE.
 */
@Entity
@Table(name = "seguimientos_semanales",
        indexes = {
                @Index(name = "idx_seg_instancia", columnList = "instancia_practica_id"),
                @Index(name = "idx_seg_estado", columnList = "estado")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_seguimiento_semana",
                columnNames = {"instancia_practica_id", "semana"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguimientoSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_practica_id", nullable = false)
    private InstanciaPractica instanciaPractica;

    @Column(name = "semana", nullable = false)
    private int semana;

    @Column(name = "actividades", columnDefinition = "TEXT")
    private String actividades;

    @Column(name = "logros", columnDefinition = "TEXT")
    private String logros;

    @Column(name = "dificultades", columnDefinition = "TEXT")
    private String dificultades;

    /** URLs de evidencias separadas por coma */
    @Column(name = "evidencias", length = 2000)
    private String evidencias;

    @Column(name = "observaciones_docente", columnDefinition = "TEXT")
    private String observacionesDocente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoSeguimiento estado = EstadoSeguimiento.ENVIADO;

    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @Column(name = "revisado_por_id")
    private Long revisadoPorId;

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.actualizadoEn = LocalDateTime.now(); }

    // ── PATRON STATE ──────────────────────────────────────────────────────────

    /** El docente marca el seguimiento como revisado (ENVIADO → REVISADO). Solo cambia estado, sin nota. */
    public void revisar(Long docenteId) {
        if (this.estado != EstadoSeguimiento.ENVIADO)
            throw new OperacionNoPermitidaException("Solo se puede revisar un seguimiento en estado ENVIADO.");
        this.estado = EstadoSeguimiento.REVISADO;
        this.revisadoPorId = docenteId;
        this.revisadoEn = LocalDateTime.now();
    }

    /** El docente rechaza el seguimiento con observaciones (ENVIADO → RECHAZADO). */
    public void rechazar(String observaciones, Long docenteId) {
        if (this.estado != EstadoSeguimiento.ENVIADO && this.estado != EstadoSeguimiento.PENDIENTE)
            throw new OperacionNoPermitidaException("Solo se puede rechazar un seguimiento en estado ENVIADO.");
        this.estado = EstadoSeguimiento.RECHAZADO;
        this.observacionesDocente = observaciones;
        this.revisadoPorId = docenteId;
        this.revisadoEn = LocalDateTime.now();
    }

    /** Conservado para compatibilidad con registros anteriores (PENDIENTE → APROBADO). */
    public void aprobar(Long docenteId) {
        if (this.estado != EstadoSeguimiento.PENDIENTE)
            throw new OperacionNoPermitidaException("Solo se puede aprobar un seguimiento en estado PENDIENTE.");
        this.estado = EstadoSeguimiento.APROBADO;
        this.revisadoPorId = docenteId;
        this.revisadoEn = LocalDateTime.now();
    }

    /** Vuelve a ENVIADO para que el estudiante pueda re-editarlo tras un rechazo. */
    public void resubmit() {
        if (this.estado != EstadoSeguimiento.RECHAZADO)
            throw new OperacionNoPermitidaException("Solo se puede re-enviar un seguimiento RECHAZADO.");
        this.estado = EstadoSeguimiento.ENVIADO;
        this.revisadoPorId = null;
        this.revisadoEn = null;
    }

    public boolean esEditable() {
        return this.estado == EstadoSeguimiento.RECHAZADO;
    }
}
