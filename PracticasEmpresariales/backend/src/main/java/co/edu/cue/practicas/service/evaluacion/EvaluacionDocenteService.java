package co.edu.cue.practicas.service.evaluacion;

import co.edu.cue.practicas.event.Sprint4DomainEvent;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EvaluacionDocenteService extends AbstractEvaluacionFinalTemplate {

    public EvaluacionDocenteService(EvaluacionFinalRepository evaluacionRepository,
                                    InstanciaPracticaRepository instanciaRepository,
                                    ApplicationEventPublisher eventPublisher) {
        super(evaluacionRepository, instanciaRepository, eventPublisher);
    }

    @Override
    protected TipoEvaluacionFinal tipo() {
        return TipoEvaluacionFinal.DOCENTE_ASESOR;
    }

    @Override
    protected TipoEventoNotificacion eventoCompletado() {
        return TipoEventoNotificacion.EVALUACION_DOCENTE_COMPLETADA;
    }

    @Override
    protected void validarEvaluador(InstanciaPractica instancia, CustomUserDetails actor) {
        // SPRINT 4 - Especializacion del Template Method: solo el docente asesor asignado completa esta variante.
        if (actor.getRol() != Rol.DOCENTE_ASESOR || instancia.getDocenteAsesor() == null
                || !actor.getId().equals(instancia.getDocenteAsesor().getId())) {
            throw new AccesoNoAutorizadoException("Solo el docente asesor asignado puede evaluar esta practica.");
        }
    }
}
