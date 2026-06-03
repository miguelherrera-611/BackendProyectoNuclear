package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.ResultadoPractica;
import jakarta.persistence.Column;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notas_finales_coordinador",
        indexes = @Index(name = "idx_nota_final_instancia", columnList = "instancia_practica_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaFinalCoordinador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_practica_id", nullable = false, unique = true)
    private InstanciaPractica instanciaPractica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinador_id", nullable = false)
    private Usuario coordinador;

    @Column(name = "nota_final", nullable = false)
    private double notaFinal;

    @Column(name = "nota_minima_aplicada", nullable = false)
    private double notaMinimaAplicada;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoPractica resultado;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "fecha", nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    public void actualizar(double notaFinal, double notaMinima, String observaciones) {
        if (instanciaPractica != null && instanciaPractica.esInmutable()) {
            throw new OperacionNoPermitidaException("La nota final es inmutable despues del cierre formal.");
        }
        if (notaFinal < 0.0 || notaFinal > 5.0) {
            throw new OperacionNoPermitidaException("La nota final debe estar entre 0.0 y 5.0.");
        }
        this.notaFinal = notaFinal;
        this.notaMinimaAplicada = notaMinima;
        this.resultado = notaFinal >= notaMinima ? ResultadoPractica.APROBADO : ResultadoPractica.NO_APROBADO;
        this.observaciones = observaciones;
        this.fecha = LocalDateTime.now();
    }
}
