package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GPE-152 / GPE-153 — Entidad Vacante
 *
 * PATRÓN STATE: la entidad encapsula todas las transiciones de estado.
 * Cada método de transición valida su precondición y lanza excepción
 * si la transición no está permitida.
 *
 * SOLID — SRP: solo gestiona estado y cupos de la vacante.
 *              Mapping → VacanteMapper. Validaciones externas → VacanteValidator.
 *
 * Reglas OCL: cuposValidos, cuposPositivos, cerradaNoAcepta, PROHIBIDO eliminar.
 */
@Entity
@Table(name = "vacantes", indexes = {
        @Index(name = "idx_vacante_empresa", columnList = "empresa_id"),
        @Index(name = "idx_vacante_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vacante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @NotBlank
    @Column(name = "area", nullable = false, length = 200)
    private String area;

    @Min(1)
    @Column(name = "cupos_totales", nullable = false)
    private int cuposTotales;

    @Column(name = "cupos_ocupados", nullable = false)
    @Builder.Default
    private int cuposOcupados = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoVacante estado = EstadoVacante.DISPONIBLE;

    @Column(name = "fecha_publicacion")
    @Builder.Default
    private LocalDate fechaPublicacion = LocalDate.now();

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.actualizadoEn = LocalDateTime.now(); }

    // ── PATRÓN STATE: transiciones ────────────────────────────────────────

    /** Cualquier estado → DISPONIBLE. Reactiva vacantes inactivas o heredadas. */
    public void activar() {
        if (this.estado == EstadoVacante.DISPONIBLE)
            throw new OperacionNoPermitidaException("La vacante ya está activa.");
        this.estado = EstadoVacante.DISPONIBLE;
        this.motivoRechazo = null;
    }

    /** DISPONIBLE/PENDIENTE → CERRADA (inactiva manualmente). */
    public void desactivar() {
        if (this.estado == EstadoVacante.CERRADA)
            throw new OperacionNoPermitidaException("La vacante ya está inactiva.");
        this.estado = EstadoVacante.CERRADA;
    }

    /** @deprecated Usar activar(). Mantenido para compatibilidad interna. */
    public void aprobar() {
        this.estado = EstadoVacante.DISPONIBLE;
    }

    /** @deprecated Usar desactivar(). Mantenido para compatibilidad interna. */
    public void rechazar(String motivo) {
        this.estado = EstadoVacante.RECHAZADA;
        this.motivoRechazo = motivo;
    }

    /** DISPONIBLE → CERRADA. Usado internamente por ocuparCupo(). */
    public void cerrar() {
        validarEstado(EstadoVacante.DISPONIBLE, "cerrar");
        this.estado = EstadoVacante.CERRADA;
    }

    /** OCL: cerradaNoAcepta — usado en Sprint 3 */
    public boolean puedeAceptarPracticante() {
        return this.estado == EstadoVacante.DISPONIBLE && cuposOcupados < cuposTotales;
    }

    /** OCL: cuposValidos — usado en Sprint 3 al asignar */
    public void ocuparCupo() {
        if (cuposOcupados >= cuposTotales)
            throw new OperacionNoPermitidaException("No hay cupos disponibles.");
        this.cuposOcupados++;
        if (this.cuposOcupados == this.cuposTotales) cerrar();
    }

    /** Libera un cupo ocupado (usado cuando se cancela una asignación antes de iniciar) */
    public void liberarCupo() {
        if (this.cuposOcupados <= 0) return;
        this.cuposOcupados--;
        if (this.estado == EstadoVacante.CERRADA) {
            // Si estaba cerrada por estar llena, al liberar un cupo la vacante vuelve a DISPONIBLE
            this.estado = EstadoVacante.DISPONIBLE;
        }
    }

    private void validarEstado(EstadoVacante requerido, String operacion) {
        if (this.estado != requerido)
            throw new OperacionNoPermitidaException(
                    "Para " + operacion + " la vacante debe estar en "
                    + requerido + ". Estado actual: " + this.estado);
    }
}
