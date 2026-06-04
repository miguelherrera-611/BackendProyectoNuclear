package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluaciones_finales",
        indexes = {
                @Index(name = "idx_eval_instancia_tipo", columnList = "instancia_practica_id,tipo", unique = true),
                @Index(name = "idx_eval_estado", columnList = "estado")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_practica_id", nullable = false)
    private InstanciaPractica instanciaPractica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoEvaluacionFinal tipo;

    @Column(name = "evaluador_id", nullable = false)
    private Long evaluadorId;

    @Column(name = "evaluador_nombre", nullable = false, length = 180)
    private String evaluadorNombre;

    @ElementCollection
    @CollectionTable(name = "evaluacion_final_criterios", joinColumns = @JoinColumn(name = "evaluacion_id"))
    @Builder.Default
    private List<CriterioEvaluacion> criterios = new ArrayList<>();

    @Column(name = "promedio_final", nullable = false)
    private double promedioFinal;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoEvaluacionFinal estado = EstadoEvaluacionFinal.PENDIENTE;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public void completar(List<CriterioEvaluacion> nuevosCriterios, String observaciones) {
        if (instanciaPractica != null && instanciaPractica.esInmutable()) {
            // SPRINT 4 - Proxy: la entidad protege la evaluacion ante intentos de modificacion post-cierre.
            throw new OperacionNoPermitidaException("La evaluacion es inmutable despues del cierre formal.");
        }
        this.criterios.clear();
        this.criterios.addAll(nuevosCriterios);
        this.promedioFinal = calcularPromedio(nuevosCriterios);
        this.observaciones = observaciones;
        this.estado = EstadoEvaluacionFinal.COMPLETADA;
        this.fecha = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    private double calcularPromedio(List<CriterioEvaluacion> criteriosParam) {
        // SPRINT 4 - Regla OCL promedioCalculado: el promedio nunca llega desde el cliente, se calcula aqui.
        if (criteriosParam == null || criteriosParam.isEmpty()) {
            throw new OperacionNoPermitidaException("Debe registrar al menos un criterio de evaluacion.");
        }
        double sumaPesos = criteriosParam.stream().mapToDouble(CriterioEvaluacion::getPeso).sum();
        if (Math.abs(sumaPesos - 1.0) > 0.01) {
            throw new OperacionNoPermitidaException("La suma de pesos debe ser 1.0.");
        }
        double promedio = criteriosParam.stream()
                .mapToDouble(c -> {
                    if (c.getPuntaje() < 0.0 || c.getPuntaje() > 5.0) {
                        throw new OperacionNoPermitidaException("Cada puntaje debe estar entre 0.0 y 5.0.");
                    }
                    return c.getPeso() * c.getPuntaje();
                })
                .sum();
        double redondeado = Math.round(promedio * 100.0) / 100.0;
        if (redondeado < 0.0 || redondeado > 5.0) {
            throw new OperacionNoPermitidaException("El promedio calculado debe estar entre 0.0 y 5.0.");
        }
        return redondeado;
    }
}