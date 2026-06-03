package co.edu.cue.practicas.event.listener;

import co.edu.cue.practicas.event.Sprint4DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Sprint4ObserverListener {

    @EventListener
    public void onSprint4Event(Sprint4DomainEvent event) {
        // SPRINT 4 - Observer: listener desacoplado para reaccionar a evaluaciones, encuestas, nota final y cierre.
        log.info("[SPRINT4][OBSERVER] Evento {} para practica {}. Checklist/dashboard quedan disponibles en tiempo real por consulta.",
                event.tipoEvento(), event.instanciaPracticaId());
    }
}
