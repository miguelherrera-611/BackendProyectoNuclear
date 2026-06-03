package co.edu.cue.practicas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterioEvaluacion {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "peso", nullable = false)
    private double peso;

    @Column(name = "puntaje", nullable = false)
    private double puntaje;
}
