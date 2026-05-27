package co.edu.cue.practicas.repository.facultad;

import co.edu.cue.practicas.model.entity.Facultad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultadRepository extends JpaRepository<Facultad, Long> {

    Optional<Facultad> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Facultad> findByActivaTrue(Pageable pageable);
}
