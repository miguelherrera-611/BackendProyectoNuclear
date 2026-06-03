package co.edu.cue.practicas.repository.cierre;

import co.edu.cue.practicas.model.entity.SustentacionPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SustentacionPracticaRepository extends JpaRepository<SustentacionPractica, Long> {
    Optional<SustentacionPractica> findByInstanciaPractica_Id(Long instanciaId);
}
