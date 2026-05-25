package co.edu.cue.practicas.service.programa;

import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.RequisitosPractica;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PATRON BUILDER — GPE-140
 *
 * Construye paso a paso la configuración compleja de un programa académico:
 * facultad → número de prácticas → nota mínima → requisitos por práctica
 * → documentos requeridos.
 *
 * Permite construir configuraciones incompletas y completarlas sin errores.
 * Cada método retorna el propio Builder para encadenamiento fluido.
 */
@Component
public class ProgramaBuilder {

    private String nombre;
    private String descripcion;
    private Facultad facultad;
    private int numeroTotalPracticas = 1;
    private double promedioMinimoGeneral = 3.0;
    private final List<RequisitosPractica> requisitos = new ArrayList<>();

    public static ProgramaBuilder nuevo() {
        return new ProgramaBuilder();
    }

    public ProgramaBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public ProgramaBuilder conDescripcion(String descripcion) {
        this.descripcion = descripcion;
        return this;
    }

    public ProgramaBuilder enFacultad(Facultad facultad) {
        this.facultad = facultad;
        return this;
    }

    public ProgramaBuilder conNumeroDePracticas(int total) {
        this.numeroTotalPracticas = total;
        return this;
    }

    public ProgramaBuilder conPromedioMinimoGeneral(double promedio) {
        this.promedioMinimoGeneral = promedio;
        return this;
    }

    public ProgramaBuilder agregarRequisitoPractica(
            int numeroPractica,
            int creditosMinimos,
            double promedioMinimo,
            boolean requierePracticaAnteriorAprobada,
            String documentosRequeridos) {

        RequisitosPractica req = RequisitosPractica.builder()
                .numeroPractica(numeroPractica)
                .creditosMinimos(creditosMinimos)
                .promedioMinimo(promedioMinimo)
                .requierePracticaAnteriorAprobada(requierePracticaAnteriorAprobada)
                .documentosRequeridos(documentosRequeridos)
                .build();

        this.requisitos.add(req);
        return this;
    }

    public Programa construir() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException("El nombre del programa es obligatorio.");
        }
        if (facultad == null) {
            throw new IllegalStateException("La facultad del programa es obligatoria.");
        }
        if (numeroTotalPracticas < 1) {
            throw new IllegalStateException("El programa debe tener al menos 1 práctica.");
        }

        Programa programa = Programa.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .facultad(facultad)
                .numeroTotalPracticas(numeroTotalPracticas)
                .promedioMinimoGeneral(promedioMinimoGeneral)
                .activo(true)
                .build();

        // Asociar los requisitos al programa construido
        requisitos.forEach(r -> r.setPrograma(programa));
        programa.setRequisitos(requisitos);

        return programa;
    }

    /** Resetea el builder para reutilizarlo */
    public ProgramaBuilder reset() {
        this.nombre = null;
        this.descripcion = null;
        this.facultad = null;
        this.numeroTotalPracticas = 1;
        this.promedioMinimoGeneral = 3.0;
        this.requisitos.clear();
        return this;
    }
}
