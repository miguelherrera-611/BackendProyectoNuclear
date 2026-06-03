package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoEncuesta;
import co.edu.cue.practicas.model.enums.TipoEncuesta;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "encuestas_satisfaccion",
        indexes = {
                @Index(name = "idx_encuesta_instancia_tipo", columnList = "instancia_practica_id,tipo", unique = true),
                @Index(name = "idx_encuesta_estado", columnList = "estado")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncuestaSatisfaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_practica_id", nullable = false)
    private InstanciaPractica instanciaPractica;

    @Column(name = "titulo", nullable = false, length = 180)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 25)
    private TipoEncuesta tipo;

    @Column(name = "actor_asignado_id", nullable = false)
    private Long actorAsignadoId;

    @Column(name = "actor_asignado_correo", nullable = false, length = 150)
    private String actorAsignadoCorreo;

    @Column(name = "token_acceso", unique = true, length = 80)
    private String tokenAcceso;

    @ElementCollection
    @CollectionTable(name = "encuesta_preguntas", joinColumns = @JoinColumn(name = "encuesta_id"))
    @Column(name = "pregunta", length = 500)
    @Builder.Default
    private List<String> preguntas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "encuesta_respuestas", joinColumns = @JoinColumn(name = "encuesta_id"))
    @Column(name = "respuesta", length = 1000)
    @Builder.Default
    private List<String> respuestas = new ArrayList<>();

    @Column(name = "enviada", nullable = false)
    @Builder.Default
    private boolean enviada = false;

    @Column(name = "completada", nullable = false)
    @Builder.Default
    private boolean completada = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoEncuesta estado = EstadoEncuesta.PENDIENTE;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

    @Column(name = "ultimo_recordatorio")
    private LocalDate ultimoRecordatorio;

    public void enviar() {
        // SPRINT 4 - State: PENDIENTE queda enviada; no marca completada.
        this.enviada = true;
        this.fechaEnvio = LocalDateTime.now();
    }

    public void guardarBorrador(List<String> respuestas) {
        if (completada) {
            // SPRINT 4 - Proxy: encuesta completada no admite nuevas escrituras.
            throw new OperacionNoPermitidaException("La encuesta completada no permite modificaciones.");
        }
        if (!enviada) {
            throw new OperacionNoPermitidaException("La encuesta debe enviarse antes de responderse.");
        }
        this.respuestas = respuestas;
        this.estado = EstadoEncuesta.EN_BORRADOR;
    }

    public void completar(List<String> respuestas) {
        if (completada) {
            // SPRINT 4 - Proxy: respuestas inmutables despues del envio definitivo.
            throw new OperacionNoPermitidaException("La encuesta completada no permite modificaciones.");
        }
        if (!enviada) {
            throw new OperacionNoPermitidaException("La encuesta debe enviarse antes de completarse.");
        }
        this.respuestas = respuestas;
        // SPRINT 4 - State: PENDIENTE/EN_BORRADOR -> COMPLETADA.
        this.estado = EstadoEncuesta.COMPLETADA;
        this.completada = true;
        this.fechaCompletada = LocalDateTime.now();
    }
}
