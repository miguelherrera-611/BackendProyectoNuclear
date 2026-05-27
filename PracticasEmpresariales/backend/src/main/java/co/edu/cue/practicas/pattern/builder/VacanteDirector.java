package co.edu.cue.practicas.pattern.builder;

import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.Vacante;
import org.springframework.stereotype.Component;

/**
 * PATRÓN BUILDER — Director
 *
 * Equivalente al PracticeDirector del diagrama del documento PDF:
 *   PracticeDirector → PracticeBuilder → PracticeInstance
 *
 * El Director conoce el orden correcto de construcción y
 * encapsula las "recetas" para crear vacantes estándar.
 *
 * SOLID — SRP: solo dirige la construcción, no tiene lógica de negocio.
 * SOLID — DIP: depende de VacanteBuilder (abstracción del proceso),
 *              no de Vacante directamente.
 *
 * Uso en VacanteService:
 *   Vacante v = vacanteDirector.construirVacanteEstandar(empresa, area, cupos);
 */
@Component
public class VacanteDirector {

    /**
     * Construye una vacante estándar lista para entrar al proceso
     * de aprobación en estado PENDIENTE.
     */
    public Vacante construirVacanteEstandar(Empresa empresa, String area, int cupos) {
        return new VacanteBuilder()
                .conEmpresa(empresa)
                .conArea(area)
                .conCupos(cupos)
                .build();
    }

    /**
     * Construye una vacante con fecha de publicación específica
     * (ej. para vacantes programadas).
     */
    public Vacante construirVacanteConFecha(Empresa empresa, String area,
                                            int cupos, java.time.LocalDate fecha) {
        return new VacanteBuilder()
                .conEmpresa(empresa)
                .conArea(area)
                .conCupos(cupos)
                .conFechaPublicacion(fecha)
                .build();
    }
}
