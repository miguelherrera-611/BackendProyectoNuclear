package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.ResultadoSustentacion;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sustentaciones_practica",
        indexes = @Index(name = "idx_sustentacion_instancia", columnList = "instancia_practica_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SustentacionPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_practica_id", nullable = false, unique = true)
    private InstanciaPractica instanciaPractica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinador_id", nullable = false)
    private Usuario coordinador;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ElementCollection
    @CollectionTable(name = "sustentacion_jurados", joinColumns = @JoinColumn(name = "sustentacion_id"))
    @Column(name = "jurado", length = 180)
    @Builder.Default
    private List<String> jurados = new ArrayList<>();

    @Column(name = "acta_url", length = 1000)
    private String actaUrl;

    @Column(name = "acta_firmada", nullable = false)
    @Builder.Default
    private boolean actaFirmada = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 20)
    private ResultadoSustentacion resultado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    public void registrarResultado(ResultadoSustentacion resultado, String actaUrl, boolean actaFirmada) {
        if (jurados == null || jurados.isEmpty()) {
            throw new OperacionNoPermitidaException("La sustentacion requiere minimo un jurado.");
        }
        if (actaUrl == null || actaUrl.isBlank() || !actaFirmada) {
            throw new OperacionNoPermitidaException("El acta firmada es obligatoria antes del resultado.");
        }
        this.resultado = resultado;
        this.actaUrl = actaUrl;
        this.actaFirmada = true;
    }

    public boolean estaCompleta() {
        return resultado != null && actaFirmada && actaUrl != null && !actaUrl.isBlank()
                && jurados != null && !jurados.isEmpty();
    }
}
