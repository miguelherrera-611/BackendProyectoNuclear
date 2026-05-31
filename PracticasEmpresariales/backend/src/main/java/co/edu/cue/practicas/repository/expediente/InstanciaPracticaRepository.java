package co.edu.cue.practicas.repository.expediente;

import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanciaPracticaRepository extends JpaRepository<InstanciaPractica, Long> {

    List<InstanciaPractica> findByExpediente_Id(Long expedienteId);

    long countByEstado(EstadoPractica estado);

    long countByEstadoNotIn(List<EstadoPractica> estados);

    long countByExpediente_Estudiante_IdAndEstado(Long estudianteId, EstadoPractica estado);

    long countByExpediente_Estudiante_Programa_IdAndEstado(Long programaId, EstadoPractica estado);

    long countByDocenteAsesor_IdAndEstadoNotIn(Long docenteAsesorId, List<EstadoPractica> estados);

    long countByTutorEmpresarial_IdAndEstadoNotIn(Long tutorEmpresarialId, List<EstadoPractica> estados);

    /** Práctica anterior: la que tiene (numeroPractica - 1) para el mismo estudiante */
    @Query("SELECT i FROM InstanciaPractica i WHERE i.expediente.estudiante.id = :estudianteId " +
           "AND i.numeroPractica = :numeroPractica")
    Optional<InstanciaPractica> findPorEstudianteYNumero(
            @Param("estudianteId") Long estudianteId,
            @Param("numeroPractica") int numeroPractica);

    /** OCL: noDesactivarCatalogoCon­EstudiantesActivos */
    boolean existsByCatalogoPracticaIdAndEstadoNotIn(
            Long catalogoPracticaId, List<EstadoPractica> estadosFinales);
}
