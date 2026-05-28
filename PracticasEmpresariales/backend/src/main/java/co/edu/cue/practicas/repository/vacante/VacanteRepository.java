package co.edu.cue.practicas.repository.vacante;

import co.edu.cue.practicas.model.entity.Vacante;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VacanteRepository extends JpaRepository<Vacante, Long> {
    List<Vacante> findByEmpresaId(Long empresaId);
    List<Vacante> findByEstado(EstadoVacante estado);
    List<Vacante> findByEmpresaIdAndEstado(Long empresaId, EstadoVacante estado);
    boolean existsByEmpresaIdAndEstadoIn(Long empresaId, List<EstadoVacante> estados);
}
