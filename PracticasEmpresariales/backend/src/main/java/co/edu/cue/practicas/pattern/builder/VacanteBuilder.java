package co.edu.cue.practicas.pattern.builder;

import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.Vacante;
import co.edu.cue.practicas.model.enums.EstadoVacante;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PATRÓN BUILDER — VacanteBuilder (ConcreteBuilder)
 *
 * Construye una Vacante paso a paso.
 * Basado en el diagrama del documento:
 *   PracticeDirector → PracticeBuilder → PracticeInstance
 *
 * SOLID — SRP: responsabilidad única → construir Vacante.
 * SOLID — OCP: si se agregan nuevos campos a Vacante, solo
 *              se agrega un método withX() aquí.
 *
 * Uso:
 *   Vacante v = new VacanteBuilder()
 *       .conEmpresa(empresa)
 *       .conArea("Desarrollo")
 *       .conCupos(2)
 *       .build();
 */
public class VacanteBuilder {

    private Empresa empresa;
    private String area;
    private int cuposTotales;
    private LocalDate fechaPublicacion = LocalDate.now();

    public VacanteBuilder conEmpresa(Empresa empresa) {
        this.empresa = empresa;
        return this;
    }

    public VacanteBuilder conArea(String area) {
        this.area = area;
        return this;
    }

    public VacanteBuilder conCupos(int cuposTotales) {
        this.cuposTotales = cuposTotales;
        return this;
    }

    public VacanteBuilder conFechaPublicacion(LocalDate fecha) {
        this.fechaPublicacion = fecha;
        return this;
    }

    public Vacante build() {
        validar();
        return Vacante.builder()
                .empresa(empresa)
                .area(area)
                .cuposTotales(cuposTotales)
                .cuposOcupados(0)
                .estado(EstadoVacante.PENDIENTE)
                .fechaPublicacion(fechaPublicacion)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(LocalDateTime.now())
                .build();
    }

    private void validar() {
        if (empresa == null)
            throw new IllegalStateException("La empresa es obligatoria para crear una vacante.");
        if (area == null || area.isBlank())
            throw new IllegalStateException("El área es obligatoria.");
        if (cuposTotales < 1)
            throw new IllegalStateException("Los cupos deben ser al menos 1.");
    }
}
