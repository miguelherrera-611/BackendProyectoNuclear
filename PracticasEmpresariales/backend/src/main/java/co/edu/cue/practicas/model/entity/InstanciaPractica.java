package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * GPE-141 / GPE-145 — InstanciaPractica
 *
 * Representa la práctica concreta de un estudiante específico.
 * Se crea clonando la plantilla del CatalogoPractica vigente al marcar APTO
 * (PATRÓN PROTOTYPE). Los valores del catálogo se COPIAN en este registro
 * para que cambios futuros al catálogo no afecten instancias ya creadas.
 *
 * PATRÓN STATE: la entidad gestiona sus propias transiciones de estado.
 * Transiciones permitidas:
 *   ASIGNADA_PENDIENTE_INICIO → EN_CURSO → FINALIZADA
 *                                         ↘ CANCELADA
 *
 * SOLID — SRP: solo gestiona el estado y los datos de la práctica del estudiante.
 * OCL: practicaInmutableCuandoFinalizada — bloquea modificaciones si FINALIZADA o CANCELADA.
 */
@Entity
@Table(name = "instancias_practica",
        indexes = {
                @Index(name = "idx_instancia_expediente", columnList = "expediente_id"),
                @Index(name = "idx_instancia_estado", columnList = "estado")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanciaPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private ExpedienteEstudiante expediente;

    /** ID del catálogo del que fue clonada — para trazabilidad */
    @Column(name = "catalogo_practica_id", nullable = false)
    private Long catalogoPracticaId;

    // ── SNAPSHOT del catálogo vigente al momento de marcar APTO ──────────────
    // Los cambios posteriores al catálogo NO afectan instancias ya creadas.

    @Column(name = "numero_practica", nullable = false)
    private int numeroPractica;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "materia_nucleo", nullable = false, length = 200)
    private String materiaNucleo;

    @Column(name = "codigo_materia", nullable = false, length = 20)
    private String codigoMateria;

    @Column(name = "num_cortes", nullable = false)
    private int numCortes;

    @Column(name = "duracion_semanas", nullable = false)
    private int duracionSemanas;

    @Column(name = "documentos_requeridos", length = 1000)
    private String documentosRequeridos;

    // ── Estado — PATRÓN STATE ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoPractica estado = EstadoPractica.ASIGNADA_PENDIENTE_INICIO;

    // ── Vínculos asignados en Sprint 3 ────────────────────────────────────────

    /** Empresa donde realizará la práctica — se asigna en Sprint 3 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /** Docente asesor asignado — se asigna en Sprint 3 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_asesor_id")
    private Usuario docenteAsesor;

    /** Tutor empresarial asignado — se asigna en Sprint 3 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_empresarial_id")
    private TutorEmpresarial tutorEmpresarial;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.actualizadoEn = LocalDateTime.now(); }

    // ── PATRÓN STATE: transiciones ────────────────────────────────────────────

    public void iniciar() {
        validarEstado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO, "iniciar");
        this.estado = EstadoPractica.EN_CURSO;
    }

    public void finalizar() {
        validarEstado(EstadoPractica.EN_CURSO, "finalizar");
        this.estado = EstadoPractica.FINALIZADA;
    }

    public void cancelar() {
        if (this.estado == EstadoPractica.FINALIZADA)
            throw new OperacionNoPermitidaException("Una práctica FINALIZADA no puede cancelarse.");
        this.estado = EstadoPractica.CANCELADA;
    }

    /** OCL: practicaInmutableCuandoFinalizada */
    public boolean esInmutable() {
        return this.estado == EstadoPractica.FINALIZADA
                || this.estado == EstadoPractica.CANCELADA;
    }

    private void validarEstado(EstadoPractica requerido, String operacion) {
        if (this.estado != requerido)
            throw new OperacionNoPermitidaException(
                    "Para " + operacion + " la práctica debe estar en "
                    + requerido + ". Estado actual: " + this.estado);
    }
}
