package co.edu.cue.practicas.repository.expediente;

import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.enums.EstadoHojaDeVida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HojaDeVidaRepository extends JpaRepository<HojaDeVida, Long> {

    List<HojaDeVida> findByEstudiante_IdOrderByVersionDesc(Long estudianteId);

    Optional<HojaDeVida> findTopByEstudiante_IdOrderByVersionDesc(Long estudianteId);

    Optional<HojaDeVida> findTopByEstudiante_IdAndEstadoOrderByVersionDesc(
            Long estudianteId, EstadoHojaDeVida estado);

    int countByEstudiante_Id(Long estudianteId);
}
