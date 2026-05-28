package co.edu.cue.practicas.pattern.builder;

import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.Programa;

import java.time.LocalDateTime;

/**
 * PATRÓN BUILDER — CatalogoPracticaBuilder (ConcreteBuilder)
 *
 * Construye una entrada del catálogo de prácticas paso a paso:
 *   número → nombre → materia núcleo → cortes → duración → documentos requeridos
 *
 * Garantiza que todos los campos obligatorios estén presentes antes de crear
 * la entidad, evitando objetos inconsistentes en la base de datos.
 *
 * SOLID — SRP: solo construye CatalogoPractica.
 * SOLID — OCP: nuevos campos → nuevo método conX(), sin tocar la entidad.
 */
public class CatalogoPracticaBuilder {

    private Programa programa;
    private int numeroPractica;
    private String nombre;
    private String materiaNucleo;
    private String codigoMateria;
    private int numCortes;
    private int duracionSemanas;
    private String documentosRequeridos;

    public CatalogoPracticaBuilder conPrograma(Programa programa) {
        this.programa = programa;
        return this;
    }

    public CatalogoPracticaBuilder conNumero(int numeroPractica) {
        this.numeroPractica = numeroPractica;
        return this;
    }

    public CatalogoPracticaBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public CatalogoPracticaBuilder conMateriaNucleo(String materiaNucleo, String codigoMateria) {
        this.materiaNucleo = materiaNucleo;
        this.codigoMateria = codigoMateria;
        return this;
    }

    public CatalogoPracticaBuilder conCortes(int numCortes) {
        this.numCortes = numCortes;
        return this;
    }

    public CatalogoPracticaBuilder conDuracion(int duracionSemanas) {
        this.duracionSemanas = duracionSemanas;
        return this;
    }

    public CatalogoPracticaBuilder conDocumentos(String documentosRequeridos) {
        this.documentosRequeridos = documentosRequeridos;
        return this;
    }

    public CatalogoPractica build() {
        validar();
        return CatalogoPractica.builder()
                .programa(programa)
                .numeroPractica(numeroPractica)
                .nombre(nombre)
                .materiaNucleo(materiaNucleo)
                .codigoMateria(codigoMateria)
                .numCortes(numCortes)
                .duracionSemanas(duracionSemanas)
                .documentosRequeridos(documentosRequeridos)
                .activo(true)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(LocalDateTime.now())
                .build();
    }

    private void validar() {
        if (programa == null)
            throw new IllegalStateException("El programa es obligatorio.");
        if (nombre == null || nombre.isBlank())
            throw new IllegalStateException("El nombre de la práctica es obligatorio.");
        if (materiaNucleo == null || materiaNucleo.isBlank())
            throw new IllegalStateException("La materia núcleo es obligatoria.");
        if (codigoMateria == null || codigoMateria.isBlank())
            throw new IllegalStateException("El código de la materia es obligatorio.");
        if (numCortes < 1)
            throw new IllegalStateException("El número de cortes debe ser al menos 1.");
        if (duracionSemanas < 1)
            throw new IllegalStateException("La duración en semanas debe ser al menos 1.");
    }
}
