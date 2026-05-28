package co.edu.cue.practicas.pattern.prototype;

import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * PATRÓN PROTOTYPE — CatalogoPracticaPlantilla (ConcretePrototype)
 *
 * Clona la plantilla del catálogo vigente para crear la instancia de práctica
 * del estudiante al ser marcado como APTO.
 *
 * Los valores se COPIAN en la instancia para garantizar que cambios futuros
 * al catálogo no afecten prácticas ya iniciadas (Criterio de Aceptación GPE-141:
 * "las prácticas activas conservan la configuración con la que fueron creadas").
 *
 * SOLID — SRP: solo clona. La lógica de negocio (quién puede clonar y cuándo)
 *              está en EstudianteService.
 * SOLID — OCP: si se agregan campos al catálogo → solo actualizar clonar().
 */
@RequiredArgsConstructor
public class CatalogoPracticaPlantilla implements IPrototype<InstanciaPractica> {

    private final CatalogoPractica catalogo;
    private final ExpedienteEstudiante expediente;

    /**
     * Clona el catálogo como una nueva InstanciaPractica en estado ASIGNADA_PENDIENTE_INICIO.
     * Los campos del catálogo se copian como snapshot — no son referencias al catálogo.
     */
    @Override
    public InstanciaPractica clonar() {
        return InstanciaPractica.builder()
                .expediente(expediente)
                .catalogoPracticaId(catalogo.getId())
                .numeroPractica(catalogo.getNumeroPractica())
                .nombre(catalogo.getNombre())
                .materiaNucleo(catalogo.getMateriaNucleo())
                .codigoMateria(catalogo.getCodigoMateria())
                .numCortes(catalogo.getNumCortes())
                .duracionSemanas(catalogo.getDuracionSemanas())
                .documentosRequeridos(catalogo.getDocumentosRequeridos())
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(LocalDateTime.now())
                .build();
    }
}
