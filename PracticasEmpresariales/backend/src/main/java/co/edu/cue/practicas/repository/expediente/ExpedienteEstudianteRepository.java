package co.edu.cue.practicas.repository.expediente;

import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpedienteEstudianteRepository extends JpaRepository<ExpedienteEstudiante, Long> {

    Optional<ExpedienteEstudiante> findByEstudiante_Id(Long estudianteId);

    boolean existsByEstudiante_Id(Long estudianteId);
}
