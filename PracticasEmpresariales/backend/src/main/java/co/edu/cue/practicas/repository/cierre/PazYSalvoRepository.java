package co.edu.cue.practicas.repository.cierre;

import co.edu.cue.practicas.model.entity.PazYSalvo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PazYSalvoRepository extends JpaRepository<PazYSalvo, Long> {
    Optional<PazYSalvo> findByInstanciaPractica_Id(Long instanciaId);
}
