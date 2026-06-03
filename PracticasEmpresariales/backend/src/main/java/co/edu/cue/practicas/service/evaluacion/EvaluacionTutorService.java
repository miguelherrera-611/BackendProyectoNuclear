package co.edu.cue.practicas.service.evaluacion;

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
public class EvaluacionTutorService extends AbstractEvaluacionFinalTemplate {

    public EvaluacionTutorService(EvaluacionFinalRepository evaluacionRepository,
                                  InstanciaPracticaRepository instanciaRepository,
                                  ApplicationEventPublisher eventPublisher) {
        super(evaluacionRepository, instanciaRepository, eventPublisher);
    }

    @Override
    protected TipoEvaluacionFinal tipo() {
        return TipoEvaluacionFinal.TUTOR_EMPRESARIAL;
    }

    @Override
    protected TipoEventoNotificacion eventoCompletado() {
        return TipoEventoNotificacion.EVALUACION_TUTOR_COMPLETADA;
    }

    @Override
    protected void validarEvaluador(InstanciaPractica instancia, CustomUserDetails actor) {
        // SPRINT 4 - Adapter: el tutor externo se adapta al modelo interno comparando su acceso por correo.
        boolean tutorPorCorreo = instancia.getTutorEmpresarial() != null
                && actor.getUsername().equalsIgnoreCase(instancia.getTutorEmpresarial().getCorreo());
        if (actor.getRol() != Rol.TUTOR_EMPRESARIAL || !tutorPorCorreo) {
            throw new AccesoNoAutorizadoException("Solo el tutor empresarial asignado puede evaluar esta practica.");
        }
    }
}
