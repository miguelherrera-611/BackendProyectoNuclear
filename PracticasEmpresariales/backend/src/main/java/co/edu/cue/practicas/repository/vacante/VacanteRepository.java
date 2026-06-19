package co.edu.cue.practicas.repository.vacante;

import co.edu.cue.practicas.model.entity.Vacante;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VacanteRepository extends JpaRepository<Vacante, Long> {
    List<Vacante> findByEmpresaId(Long empresaId);
    Page<Vacante> findByEmpresaId(Long empresaId, Pageable pageable);
    List<Vacante> findByEstado(EstadoVacante estado);
    Page<Vacante> findByEstado(EstadoVacante estado, Pageable pageable);
    List<Vacante> findByEmpresaIdAndEstado(Long empresaId, EstadoVacante estado);
    boolean existsByEmpresaIdAndEstadoIn(Long empresaId, List<EstadoVacante> estados);

    @Query("SELECT COUNT(v) FROM Vacante v WHERE v.estado = co.edu.cue.practicas.model.enums.EstadoVacante.DISPONIBLE AND v.cuposOcupados < v.cuposTotales")
    long countVacantesDisponibles();
}
