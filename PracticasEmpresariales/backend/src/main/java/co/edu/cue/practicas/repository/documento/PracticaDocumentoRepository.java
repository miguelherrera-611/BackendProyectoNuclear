package co.edu.cue.practicas.repository.documento;

import co.edu.cue.practicas.model.entity.PracticaDocumento;
import co.edu.cue.practicas.model.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticaDocumentoRepository extends JpaRepository<PracticaDocumento, Long> {

    List<PracticaDocumento> findByInstanciaPractica_IdOrderByCreadoEnDesc(Long instanciaPracticaId);

    long countByInstanciaPractica_Id(Long instanciaPracticaId);

    boolean existsByInstanciaPractica_IdAndTipo(Long instanciaPracticaId, TipoDocumento tipo);
}

