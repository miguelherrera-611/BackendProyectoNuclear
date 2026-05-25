package co.edu.cue.practicas.security.annotation;

import co.edu.cue.practicas.model.enums.Rol;

import java.lang.annotation.*;

/**
 * PATRON PROXY — GPE-137
 *
 * Anotación para marcar métodos de servicio que requieren un rol específico.
 * El ScopeValidationAspect intercepta y valida antes de ejecutar el método.
 *
 * Uso:
 *   @RequiereRol(roles = {Rol.ADMIN_DTI})
 *   public void crearUsuario(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiereRol {
    Rol[] roles();
}
