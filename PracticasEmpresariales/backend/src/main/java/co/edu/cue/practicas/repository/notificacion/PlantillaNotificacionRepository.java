package co.edu.cue.practicas.repository.notificacion;

import co.edu.cue.practicas.model.entity.PlantillaNotificacion;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlantillaNotificacionRepository extends JpaRepository<PlantillaNotificacion, Long> {
    Optional<PlantillaNotificacion> findByTipoEvento(TipoEventoNotificacion tipoEvento);
}
