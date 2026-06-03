package co.edu.cue.practicas.repository.programa;

import co.edu.cue.practicas.model.entity.ProgramaConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramaConfiguracionRepository extends JpaRepository<ProgramaConfiguracion, Long> {
    Optional<ProgramaConfiguracion> findTopByPrograma_IdAndVigenteTrueOrderByCreadoEnDesc(Long programaId);
    List<ProgramaConfiguracion> findByPrograma_IdAndVigenteTrue(Long programaId);
}
