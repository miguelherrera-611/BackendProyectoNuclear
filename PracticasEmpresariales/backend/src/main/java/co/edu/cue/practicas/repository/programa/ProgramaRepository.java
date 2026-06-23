package co.edu.cue.practicas.repository.programa;

import co.edu.cue.practicas.model.entity.Programa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramaRepository extends JpaRepository<Programa, Long> {

    Page<Programa> findByActivoTrue(Pageable pageable);

    List<Programa> findByFacultad_IdAndActivoTrue(Long facultadId);

    boolean existsByNombreIgnoreCaseAndFacultad_Id(String nombre, Long facultadId);

    boolean existsByNombreIgnoreCaseAndFacultad_IdAndIdNot(String nombre, Long facultadId, Long id);
}
