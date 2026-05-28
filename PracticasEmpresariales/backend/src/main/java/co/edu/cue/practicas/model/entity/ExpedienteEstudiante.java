package co.edu.cue.practicas.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GPE-146 — Entidad ExpedienteEstudiante
 *
 * Agrega toda la información académica y de prácticas de un estudiante
 * en un único contenedor. Se crea automáticamente vacío cuando el DTI
 * registra un nuevo estudiante (PATRÓN OBSERVER + listener).
 *
 * PATRÓN BUILDER: ExpedienteBuilder construye el DTO de respuesta completo
 *   a partir de este agregado.
 * PATRÓN PROXY (Protection + Cache): ExpedienteProxy valida que las prácticas
 *   FINALIZADAS o CANCELADAS sean de solo lectura, y cachea los expedientes
 *   más consultados.
 *
 * SOLID — SRP: solo actúa como raíz del agregado; no tiene lógica de negocio.
 * OCL: expedienteInmutable → prácticas FINALIZADAS/CANCELADAS nunca se modifican.
 *      eliminarProhibido → el expediente nunca se elimina.
 */
@Entity
@Table(name = "expedientes_estudiantes",
        indexes = @Index(name = "idx_expediente_estudiante", columnList = "estudiante_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relación 1:1 con el estudiante — cada estudiante tiene exactamente un expediente */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false, unique = true)
    private Usuario estudiante;

    /** Historial completo de prácticas (clonadas desde el catálogo al marcar APTO) */
    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<InstanciaPractica> practicas = new ArrayList<>();

    /** Historial de versiones de la hoja de vida */
    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("version DESC")
    @Builder.Default
    private List<HojaDeVida> historialHv = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    /** Agrega una instancia de práctica al expediente */
    public void agregarPractica(InstanciaPractica instancia) {
        instancia.setExpediente(this);
        this.practicas.add(instancia);
    }

    /** Agrega una nueva versión de HV al historial */
    public void agregarHojaDeVida(HojaDeVida hv) {
        this.historialHv.add(hv);
    }

    /** Retorna la versión más reciente de la HV, o null si no tiene ninguna */
    public HojaDeVida getHvActual() {
        return historialHv.isEmpty() ? null : historialHv.get(0);
    }
}
