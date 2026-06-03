package co.edu.cue.practicas.dto.response;

import co.edu.cue.practicas.model.entity.EncuestaSatisfaccion;
import co.edu.cue.practicas.model.enums.EstadoEncuesta;
import co.edu.cue.practicas.model.enums.TipoEncuesta;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class EncuestaResponse {
    private Long id;
    private Long instanciaPracticaId;
    private String titulo;
    private TipoEncuesta tipo;
    private Long actorAsignadoId;
    private String actorAsignadoCorreo;
    private String enlaceDirecto;
    private List<String> preguntas;
    private List<String> respuestas;
    private boolean enviada;
    private boolean completada;
    private EstadoEncuesta estado;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaCompletada;

    public static EncuestaResponse desde(EncuestaSatisfaccion e) {
        return EncuestaResponse.builder()
                .id(e.getId())
                .instanciaPracticaId(e.getInstanciaPractica().getId())
                .titulo(e.getTitulo())
                .tipo(e.getTipo())
                .actorAsignadoId(e.getActorAsignadoId())
                .actorAsignadoCorreo(e.getActorAsignadoCorreo())
                .enlaceDirecto(e.getTokenAcceso() != null ? "/api/v1/encuestas-satisfaccion/publica/" + e.getTokenAcceso() : null)
                .preguntas(e.getPreguntas() != null ? new ArrayList<>(e.getPreguntas()) : List.of())
                .respuestas(e.getRespuestas() != null ? new ArrayList<>(e.getRespuestas()) : List.of())
                .enviada(e.isEnviada())
                .completada(e.isCompletada())
                .estado(e.getEstado())
                .fechaEnvio(e.getFechaEnvio())
                .fechaCompletada(e.getFechaCompletada())
                .build();
    }
}
