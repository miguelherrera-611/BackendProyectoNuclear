package co.edu.cue.practicas.service.evaluacion;

import co.edu.cue.practicas.dto.request.CriterioEvaluacionRequest;
import co.edu.cue.practicas.dto.request.RegistrarEvaluacionFinalRequest;
import co.edu.cue.practicas.dto.response.EvaluacionFinalResponse;
import co.edu.cue.practicas.event.Sprint4DomainEvent;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.CriterioEvaluacion;
import co.edu.cue.practicas.model.entity.EvaluacionFinal;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

public abstract class AbstractEvaluacionFinalTemplate {

    private final EvaluacionFinalRepository evaluacionRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final ApplicationEventPublisher eventPublisher;

    protected AbstractEvaluacionFinalTemplate(EvaluacionFinalRepository evaluacionRepository,
                                              InstanciaPracticaRepository instanciaRepository,
                                              ApplicationEventPublisher eventPublisher) {
        this.evaluacionRepository = evaluacionRepository;
        this.instanciaRepository = instanciaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EvaluacionFinalResponse registrar(Long instanciaId, RegistrarEvaluacionFinalRequest req, CustomUserDetails actor) {
        // SPRINT 4 - Template Method: flujo fijo para toda evaluacion final:
        // validar expediente editable -> validar evaluador -> calcular/guardar -> notificar.
        InstanciaPractica instancia = buscarInstanciaEditable(instanciaId);
        validarEvaluador(instancia, actor);
        EvaluacionFinal evaluacion = evaluacionRepository.findByInstanciaPractica_IdAndTipo(instanciaId, tipo())
                .orElseGet(() -> nuevaEvaluacion(instancia, actor));
        evaluacion.completar(mapearCriterios(req.getCriterios()), req.getObservaciones());
        EvaluacionFinal guardada = evaluacionRepository.save(evaluacion);
        // SPRINT 4 - Observer: publica evento para que checklist, dashboard y notificaciones reaccionen desacoplados.
        eventPublisher.publishEvent(new Sprint4DomainEvent(instanciaId, eventoCompletado()));
        return EvaluacionFinalResponse.desde(guardada);
    }

    protected abstract TipoEvaluacionFinal tipo();
    protected abstract TipoEventoNotificacion eventoCompletado();
    protected abstract void validarEvaluador(InstanciaPractica instancia, CustomUserDetails actor);

    private InstanciaPractica buscarInstanciaEditable(Long instanciaId) {
        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));
        if (instancia.getEstado() != EstadoPractica.EN_CURSO) {
            throw new OperacionNoPermitidaException("La evaluacion debe registrarse antes del cierre y con practica EN_CURSO.");
        }
        if (instancia.esInmutable()) {
            // SPRINT 4 - Proxy: bloquea escritura cuando el cierre formal vuelve inmutable el expediente.
            throw new OperacionNoPermitidaException("El expediente esta inmutable por cierre formal.");
        }
        return instancia;
    }

    private EvaluacionFinal nuevaEvaluacion(InstanciaPractica instancia, CustomUserDetails actor) {
        return EvaluacionFinal.builder()
                .instanciaPractica(instancia)
                .tipo(tipo())
                .evaluadorId(actor.getId())
                .evaluadorNombre(actor.getNombre())
                .build();
    }

    private List<CriterioEvaluacion> mapearCriterios(List<CriterioEvaluacionRequest> criterios) {
        return criterios.stream()
                .map(c -> CriterioEvaluacion.builder()
                        .nombre(c.getNombre())
                        .peso(c.getPeso())
                        .puntaje(c.getPuntaje())
                        .build())
                .toList();
    }
}
