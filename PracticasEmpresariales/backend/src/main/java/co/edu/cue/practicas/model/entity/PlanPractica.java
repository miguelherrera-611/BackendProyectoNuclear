package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoPlan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * GPE-167 — Plan de práctica del estudiante.
 *
 * PATRON STATE: gestiona sus propias transiciones de estado.
 *   BORRADOR → APROBADO_TUTOR → APROBADO_DOCENTE
 *            ↘ RECHAZADO ←—————————————————————↗
 *
 * SOLID — SRP: solo almacena y valida el plan; no notifica ni audita.
 * OCL: requierePlanAprobado — el seguimiento semanal no puede iniciarse
 *      sin que este plan esté en APROBADO_DOCENTE.
 */
@Entity
@Table(name = "planes_practica",
        indexes = @Index(name = "idx_plan_instancia", columnList = "instancia_practica_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_practica_id", nullable = false)
    private InstanciaPractica instanciaPractica;

    @Column(name = "objetivos", columnDefinition = "TEXT")
    private String objetivos;

    @Column(name = "cronograma", columnDefinition = "TEXT")
    private String cronograma;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoPlan estado = EstadoPlan.BORRADOR;

    @Column(name = "cargado_por_id")
    private Long cargadoPorId;

    @Column(name = "aprobado_por_tutor_en")
    private LocalDateTime aprobadoPorTutorEn;

    @Column(name = "aprobado_por_docente_en")
    private LocalDateTime aprobadoPorDocenteEn;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "rechazado_por_id")
    private Long rechazadoPorId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.actualizadoEn = LocalDateTime.now(); }

    // ── PATRON STATE: transiciones ────────────────────────────────────────────

    public void aprobarPorTutor() {
        if (this.estado != EstadoPlan.BORRADOR && this.estado != EstadoPlan.RECHAZADO)
            throw new OperacionNoPermitidaException("El plan debe estar en BORRADOR o RECHAZADO para ser aprobado por el tutor.");
        this.estado = EstadoPlan.APROBADO_TUTOR;
        this.aprobadoPorTutorEn = LocalDateTime.now();
        this.motivoRechazo = null;
    }

    public void aprobarPorDocente() {
        if (this.estado != EstadoPlan.APROBADO_TUTOR && this.estado != EstadoPlan.BORRADOR)
            throw new OperacionNoPermitidaException("El plan debe estar en BORRADOR o APROBADO_TUTOR para ser aprobado por el docente.");
        this.estado = EstadoPlan.APROBADO_DOCENTE;
        this.aprobadoPorDocenteEn = LocalDateTime.now();
        this.motivoRechazo = null;
    }

    public void rechazar(String motivo, Long rechazadoPorId) {
        if (this.estado == EstadoPlan.APROBADO_DOCENTE)
            throw new OperacionNoPermitidaException("Un plan ya aprobado por el docente no puede rechazarse.");
        this.estado = EstadoPlan.RECHAZADO;
        this.motivoRechazo = motivo;
        this.rechazadoPorId = rechazadoPorId;
    }

    public void resubmit() {
        if (this.estado != EstadoPlan.RECHAZADO)
            throw new OperacionNoPermitidaException("Solo se puede re-enviar un plan en estado RECHAZADO.");
        this.estado = EstadoPlan.BORRADOR;
        this.motivoRechazo = null;
        this.rechazadoPorId = null;
    }

    public boolean estaAprobadoParaSeguimiento() {
        return this.estado == EstadoPlan.APROBADO_DOCENTE;
    }
}
