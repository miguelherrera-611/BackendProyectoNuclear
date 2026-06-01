package co.edu.cue.practicas.repository.seguimiento;

import co.edu.cue.practicas.model.entity.PlanPractica;
import co.edu.cue.practicas.model.enums.EstadoPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanPracticaRepository extends JpaRepository<PlanPractica, Long> {

    Optional<PlanPractica> findTopByInstanciaPractica_IdOrderByCreadoEnDesc(Long instanciaId);

    List<PlanPractica> findByInstanciaPractica_IdOrderByCreadoEnDesc(Long instanciaId);

    long countByInstanciaPractica_DocenteAsesor_IdAndEstadoIn(Long docenteId, List<EstadoPlan> estados);

    long countByInstanciaPractica_TutorEmpresarial_IdAndEstadoIn(Long tutorId, List<EstadoPlan> estados);

    long countByEstadoIn(List<EstadoPlan> estados);

    boolean existsByInstanciaPractica_IdAndEstado(Long instanciaId, EstadoPlan estado);
}
