package co.edu.cue.practicas.repository.evaluacion;

import co.edu.cue.practicas.model.entity.NotaFinalCoordinador;
import co.edu.cue.practicas.model.enums.ResultadoPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotaFinalCoordinadorRepository extends JpaRepository<NotaFinalCoordinador, Long> {
    Optional<NotaFinalCoordinador> findByInstanciaPractica_Id(Long instanciaId);
    boolean existsByInstanciaPractica_Id(Long instanciaId);
    long countByResultado(ResultadoPractica resultado);
}
