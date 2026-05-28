package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * GPE-141 — Entidad CatalogoPractica
 *
 * Es la PLANTILLA BASE institucional que la Coordinación Académica configura
 * por programa. Al marcar un estudiante como APTO, el sistema clona esta
 * plantilla (PATRÓN PROTOTYPE) para crear la instancia de práctica.
 *
 * PATRÓN BUILDER: se construye paso a paso usando CatalogoPracticaBuilder.
 * PATRÓN PROTOTYPE: CatalogoPracticaPlantilla.clonar() usa este objeto como fuente.
 *
 * SOLID — SRP: solo gestiona los datos de la plantilla del catálogo.
 * Reglas OCL:
 *   - numeroPracticaUnicoEnPrograma: UNIQUE(numero_practica, programa_id)
 *   - noDesactivarConEstudiantesActivos: validado en CatalogoPracticaValidator
 *   - camposObligatorios: nombre y materiaNucleo @NotBlank
 */
@Entity
@Table(name = "catalogo_practicas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_catalogo_numero_programa",
                columnNames = {"numero_practica", "programa_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    /** Número secuencial de la práctica dentro del programa (1, 2, 3...) */
    @Min(1)
    @Column(name = "numero_practica", nullable = false)
    private int numeroPractica;

    @NotBlank
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    /** Nombre de la materia núcleo obligatoria */
    @NotBlank
    @Column(name = "materia_nucleo", nullable = false, length = 200)
    private String materiaNucleo;

    /** Código institucional de la materia núcleo */
    @NotBlank
    @Column(name = "codigo_materia", nullable = false, length = 20)
    private String codigoMateria;

    /** Número de cortes de seguimiento durante la práctica */
    @Min(1)
    @Column(name = "num_cortes", nullable = false)
    private int numCortes;

    /** Duración esperada de la práctica en semanas */
    @Min(1)
    @Column(name = "duracion_semanas", nullable = false)
    private int duracionSemanas;

    /** Documentos que el estudiante debe entregar (separados por coma) */
    @Column(name = "documentos_requeridos", length = 1000)
    private String documentosRequeridos;

    /** OCL: noDesactivarConEstudiantesActivos — validado externamente */
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

    /** OCL: noDesactivarConEstudiantesActivos — la validación real está en el service */
    public void desactivar() {
        if (!this.activo)
            throw new OperacionNoPermitidaException("El catálogo ya está inactivo.");
        this.activo = false;
    }
}
