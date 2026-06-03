package co.edu.cue.practicas.repository.notificacion;

import co.edu.cue.practicas.model.entity.BitacoraCorreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BitacoraCorreoRepository extends JpaRepository<BitacoraCorreo, Long> {
    boolean existsByActorIdAndFechaEnvioBetween(Long actorId, LocalDateTime inicio, LocalDateTime fin);
}
