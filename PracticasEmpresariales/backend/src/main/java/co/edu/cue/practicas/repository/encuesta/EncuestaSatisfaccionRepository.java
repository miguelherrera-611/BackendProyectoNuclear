package co.edu.cue.practicas.repository.encuesta;

import co.edu.cue.practicas.model.entity.EncuestaSatisfaccion;
import co.edu.cue.practicas.model.enums.EstadoEncuesta;
import co.edu.cue.practicas.model.enums.TipoEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EncuestaSatisfaccionRepository extends JpaRepository<EncuestaSatisfaccion, Long> {
    Optional<EncuestaSatisfaccion> findByInstanciaPractica_IdAndTipo(Long instanciaId, TipoEncuesta tipo);
    Optional<EncuestaSatisfaccion> findByTokenAcceso(String tokenAcceso);
    List<EncuestaSatisfaccion> findByActorAsignadoIdOrderByFechaEnvioDesc(Long actorAsignadoId);
    List<EncuestaSatisfaccion> findByActorAsignadoCorreoIgnoreCaseOrderByFechaEnvioDesc(String actorAsignadoCorreo);
    boolean existsByInstanciaPractica_IdAndTipoAndEstado(Long instanciaId, TipoEncuesta tipo, EstadoEncuesta estado);
    List<EncuestaSatisfaccion> findByEstadoNotAndEnviadaTrue(EstadoEncuesta estado);
    boolean existsByActorAsignadoIdAndUltimoRecordatorio(Long actorAsignadoId, LocalDate ultimoRecordatorio);
}
