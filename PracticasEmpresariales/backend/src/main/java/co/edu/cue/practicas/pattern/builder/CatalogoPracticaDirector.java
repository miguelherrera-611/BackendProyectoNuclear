package co.edu.cue.practicas.pattern.builder;

import co.edu.cue.practicas.dto.request.CrearCatalogoPracticaRequest;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.Programa;
import org.springframework.stereotype.Component;

/**
 * PATRÓN BUILDER — Director
 *
 * Conoce el orden correcto de construcción del catálogo y encapsula
 * la receta completa. CatalogoPracticaService lo usa para no saber
 * el detalle de cada paso de construcción.
 *
 * SOLID — SRP: solo dirige la construcción, no tiene lógica de negocio.
 * SOLID — DIP: depende de CatalogoPracticaBuilder, no de la entidad directamente.
 */
@Component
public class CatalogoPracticaDirector {

    public CatalogoPractica construirDesdeSolicitud(Programa programa,
                                                     CrearCatalogoPracticaRequest req) {
        return new CatalogoPracticaBuilder()
                .conPrograma(programa)
                .conNumero(req.numeroPractica())
                .conNombre(req.nombre())
                .conMateriaNucleo(req.materiaNucleo(), req.codigoMateria())
                .conCortes(req.numCortes())
                .conDuracion(req.duracionSemanas())
                .conDocumentos(req.documentosRequeridos())
                .build();
    }

}
