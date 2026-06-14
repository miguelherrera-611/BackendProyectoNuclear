package co.edu.cue.practicas.repository.auditoria;

import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, Long> {

    Page<BitacoraAuditoria> findByUsuario_Id(Long usuarioId, Pageable pageable);

    Page<BitacoraAuditoria> findByTipoAccion(TipoAccion tipoAccion, Pageable pageable);

    Page<BitacoraAuditoria> findByRolUsuario(Rol rol, Pageable pageable);

    Page<BitacoraAuditoria> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    @Query(value = "SELECT b FROM BitacoraAuditoria b WHERE " +
                   "(:usuarioId IS NULL OR b.usuario.id = :usuarioId) AND " +
                   "(:tipoAccion IS NULL OR b.tipoAccion = :tipoAccion) AND " +
                   "(:desde IS NULL OR b.fechaHora >= :desde) AND " +
                   "(:hasta IS NULL OR b.fechaHora <= :hasta) AND " +
                   "(:modulo IS NULL OR b.modulo = :modulo)",
           countQuery = "SELECT count(b) FROM BitacoraAuditoria b WHERE " +
                        "(:usuarioId IS NULL OR b.usuario.id = :usuarioId) AND " +
                        "(:tipoAccion IS NULL OR b.tipoAccion = :tipoAccion) AND " +
                        "(:desde IS NULL OR b.fechaHora >= :desde) AND " +
                        "(:hasta IS NULL OR b.fechaHora <= :hasta) AND " +
                        "(:modulo IS NULL OR b.modulo = :modulo)")
    Page<BitacoraAuditoria> filtrar(
            @Param("usuarioId") Long usuarioId,
            @Param("tipoAccion") TipoAccion tipoAccion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("modulo") String modulo,
            Pageable pageable);
}
