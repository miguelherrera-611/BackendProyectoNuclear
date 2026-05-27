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
    private EstadoVacante estado = EstadoVacante.PENDIENTE;

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

    /** PENDIENTE → DISPONIBLE */
    public void aprobar() {
        validarEstado(EstadoVacante.PENDIENTE, "aprobar");
        this.estado = EstadoVacante.DISPONIBLE;
    }

    /** PENDIENTE → RECHAZADA */
    public void rechazar(String motivo) {
        if (motivo == null || motivo.isBlank())
            throw new OperacionNoPermitidaException("El motivo de rechazo es obligatorio.");
        validarEstado(EstadoVacante.PENDIENTE, "rechazar");
        this.estado = EstadoVacante.RECHAZADA;
        this.motivoRechazo = motivo;
    }

    /** DISPONIBLE → CERRADA (irreversible — OCL: cerradaNoAcepta) */
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

    private void validarEstado(EstadoVacante requerido, String operacion) {
        if (this.estado != requerido)
            throw new OperacionNoPermitidaException(
                    "Para " + operacion + " la vacante debe estar en "
                    + requerido + ". Estado actual: " + this.estado);
    }
}
