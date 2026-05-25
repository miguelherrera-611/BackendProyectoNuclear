package co.edu.cue.practicas.security.annotation;

import java.lang.annotation.*;

/**
 * PATRON PROXY — GPE-137
 *
 * Marca un método como "solo lectura".
 * El ScopeValidationAspect bloquea este método si el usuario
 * tiene el rol DIRECCION intentando una operación de escritura.
 *
 * Complementa a @RequiereRol para el bloqueo granular del rol Dirección.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SoloLectura {
}
