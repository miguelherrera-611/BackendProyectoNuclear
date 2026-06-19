package co.edu.cue.practicas.repository.catalogo;

import co.edu.cue.practicas.model.entity.CatalogoPractica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogoPracticaRepository extends JpaRepository<CatalogoPractica, Long> {

    List<CatalogoPractica> findByPrograma_IdAndActivoTrue(Long programaId);

    List<CatalogoPractica> findByPrograma_Id(Long programaId);
    Page<CatalogoPractica> findByPrograma_Id(Long programaId, Pageable pageable);

    Optional<CatalogoPractica> findByPrograma_IdAndNumeroPracticaAndActivoTrue(
            Long programaId, int numeroPractica);

    boolean existsByPrograma_IdAndNumeroPractica(Long programaId, int numeroPractica);

    /** Cuenta cuántos estudiantes tienen una instancia activa sobre este catálogo */
    long countByIdAndActivoTrue(Long catalogoId);
}
