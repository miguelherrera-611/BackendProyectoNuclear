package co.edu.cue.practicas.service.programa;

import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.RequisitosPractica;
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
 *
 * Ejemplo de uso desde ProgramaService:
 *
 *   ProgramaBuilder.nuevo()
 *       .conNombre("Ingeniería de Sistemas")
 *       .enFacultad(facultad)
 *       .conNumeroDePracticas(2)
 *       .agregarRequisitoPractica(1, 80, 3.2, false, "Hoja de vida")
 *       .agregarRequisitoPractica(2, 120, 3.5, true,  "Carta de intención")
 *       .construir();
 *
 * El método construir() valida los campos obligatorios antes de crear el objeto
 * y lanza IllegalStateException si falta alguno.
 */
public class ProgramaBuilder {

    // Campos del programa que se van llenando paso a paso con cada método del Builder
    private String nombre;
    private String descripcion;
    private Facultad facultad;
    private int numeroTotalPracticas = 1;        // valor por defecto: al menos 1 práctica
    private double promedioMinimoGeneral = 3.0;  // valor por defecto según reglamento CUE

    // Lista acumulativa de requisitos; se agrega uno por cada práctica del programa
    // (ej. requisitos de práctica 1, requisitos de práctica 2, etc.)
    private final List<RequisitosPractica> requisitos = new ArrayList<>();

    /**
     * Punto de entrada del Builder — crea una instancia nueva y limpia.
     * Se define como método estático para que el código llamador no necesite
     * escribir "new ProgramaBuilder()" directamente.
     */
    public static ProgramaBuilder nuevo() {
        return new ProgramaBuilder();
    }

    /** Define el nombre del programa académico. Campo obligatorio. */
    public ProgramaBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    /** Define la descripción del programa. Campo opcional. */
    public ProgramaBuilder conDescripcion(String descripcion) {
        this.descripcion = descripcion;
        return this;
    }

    /** Asigna la facultad a la que pertenece el programa. Campo obligatorio. */
    public ProgramaBuilder enFacultad(Facultad facultad) {
        this.facultad = facultad;
        return this;
    }

    /** Define cuántas prácticas empresariales tiene el programa en total. Mínimo 1. */
    public ProgramaBuilder conNumeroDePracticas(int total) {
        this.numeroTotalPracticas = total;
        return this;
    }

    /** Define el promedio académico mínimo general que necesita el estudiante para hacer práctica. */
    public ProgramaBuilder conPromedioMinimoGeneral(double promedio) {
        this.promedioMinimoGeneral = promedio;
        return this;
    }

    /**
     * Agrega los requisitos específicos para una práctica numerada del programa.
     * Se puede llamar varias veces — una por cada práctica (práctica 1, práctica 2, etc.).
     * Cada llamada crea un objeto RequisitosPractica y lo agrega a la lista interna.
     *
     * @param numeroPractica                    número de la práctica a la que aplican estos requisitos (1, 2...)
     * @param creditosMinimos                   créditos aprobados que debe tener el estudiante
     * @param promedioMinimo                    promedio mínimo específico para esta práctica
     * @param requierePracticaAnteriorAprobada  si true, el estudiante debe haber aprobado la práctica anterior
     * @param documentosRequeridos              texto con los documentos que debe presentar el estudiante
     */
    public ProgramaBuilder agregarRequisitoPractica(
            int numeroPractica,
            int creditosMinimos,
            double promedioMinimo,
            boolean requierePracticaAnteriorAprobada,
            String documentosRequeridos) {

        // Construimos el objeto de requisito y lo guardamos en la lista
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

    /**
     * Valida los campos obligatorios y construye el objeto Programa final.
     * Debe llamarse al final de la cadena, después de configurar todos los datos.
     *
     * Lanza IllegalStateException si falta nombre, facultad o si el número
     * de prácticas es menor a 1.
     *
     * Al construir el programa, asocia cada requisito de la lista al programa
     * para que la relación quede completa antes de persistir en base de datos.
     *
     * @return objeto Programa listo para guardar con programaRepository.save()
     */
    public Programa construir() {

        // Verificamos que los datos mínimos obligatorios estén presentes
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException("El nombre del programa es obligatorio.");
        }
        if (facultad == null) {
            throw new IllegalStateException("La facultad del programa es obligatoria.");
        }
        if (numeroTotalPracticas < 1) {
            throw new IllegalStateException("El programa debe tener al menos 1 práctica.");
        }

        // Creamos el objeto Programa con los datos configurados en el Builder
        Programa programa = Programa.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .facultad(facultad)
                .numeroTotalPracticas(numeroTotalPracticas)
                .promedioMinimoGeneral(promedioMinimoGeneral)
                .activo(true)
                .build();

        // Vinculamos cada requisito al programa recién creado y los asignamos a su lista
        // Esto es necesario para que JPA maneje correctamente la relación bidireccional
        requisitos.forEach(r -> r.setPrograma(programa));
        programa.setRequisitos(requisitos);

        return programa;
    }
}
