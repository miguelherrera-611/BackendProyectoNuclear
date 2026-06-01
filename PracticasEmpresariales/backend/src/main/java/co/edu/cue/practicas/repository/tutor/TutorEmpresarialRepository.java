package co.edu.cue.practicas.repository.tutor;

import co.edu.cue.practicas.model.entity.TutorEmpresarial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorEmpresarialRepository extends JpaRepository<TutorEmpresarial, Long> {
    List<TutorEmpresarial> findByEmpresaId(Long empresaId);
    List<TutorEmpresarial> findByEmpresaIdAndActivoTrue(Long empresaId);
    boolean existsByCorreo(String correo);
    Optional<TutorEmpresarial> findByCorreoAndActivoTrue(String correo);
}
