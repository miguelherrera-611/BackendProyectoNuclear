package co.edu.cue.practicas.repository.seguimiento;

import co.edu.cue.practicas.model.entity.SeguimientoSemanal;
import co.edu.cue.practicas.model.enums.EstadoSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeguimientoSemanalRepository extends JpaRepository<SeguimientoSemanal, Long> {

    List<SeguimientoSemanal> findByInstanciaPractica_IdOrderBySemanaAsc(Long instanciaId);

    Optional<SeguimientoSemanal> findTopByInstanciaPractica_IdOrderBySemanaDesc(Long instanciaId);

    boolean existsByInstanciaPractica_IdAndSemana(Long instanciaId, int semana);

    long countByInstanciaPractica_DocenteAsesor_IdAndEstado(Long docenteId, EstadoSeguimiento estado);

    long countByInstanciaPractica_Id(Long instanciaId);

    long countByInstanciaPractica_IdAndEstado(Long instanciaId, EstadoSeguimiento estado);

    @Query("SELECT s FROM SeguimientoSemanal s WHERE s.instanciaPractica.docenteAsesor.id = :docenteId AND s.estado = :estado ORDER BY s.creadoEn DESC")
    List<SeguimientoSemanal> findByDocenteIdAndEstado(
            @Param("docenteId") Long docenteId,
            @Param("estado") EstadoSeguimiento estado);
}
