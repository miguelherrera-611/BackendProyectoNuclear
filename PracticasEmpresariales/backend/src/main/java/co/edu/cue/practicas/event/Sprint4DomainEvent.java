package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;

public record Sprint4DomainEvent(Long instanciaPracticaId, TipoEventoNotificacion tipoEvento) {
}
