package co.edu.cue.practicas.repository.empresa;

import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    boolean existsByNit(String nit);
    Optional<Empresa> findByNit(String nit);
    List<Empresa> findByEstado(EstadoEmpresa estado);
    Page<Empresa> findByEstado(EstadoEmpresa estado, Pageable pageable);
    long countByEstado(EstadoEmpresa estado);
}
