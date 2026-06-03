package co.edu.cue.practicas.repository.evaluacion;

import co.edu.cue.practicas.model.entity.EvaluacionFinal;
import co.edu.cue.practicas.model.enums.EstadoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluacionFinalRepository extends JpaRepository<EvaluacionFinal, Long> {
    Optional<EvaluacionFinal> findByInstanciaPractica_IdAndTipo(Long instanciaId, TipoEvaluacionFinal tipo);
    boolean existsByInstanciaPractica_IdAndTipoAndEstado(
            Long instanciaId, TipoEvaluacionFinal tipo, EstadoEvaluacionFinal estado);
}
