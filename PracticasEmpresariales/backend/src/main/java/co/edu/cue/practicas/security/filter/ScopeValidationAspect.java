package co.edu.cue.practicas.security.filter;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import co.edu.cue.practicas.security.annotation.SoloLectura;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * PATRON PROXY (Protection Proxy) — GPE-137, GPE-139
 *
 * Intercepta CADA PETICIÓN antes de que llegue al servicio real.
 * Verifica rol y scope del usuario autenticado.
 * Si no tiene permisos → lanza excepción 403.
 * Registra automáticamente los intentos no autorizados en la bitácora.
 *
 * El módulo que llama al servicio no sabe que existe este proxy.
 *
 * Funciona mediante Spring AOP (Aspect-Oriented Programming):
 * los métodos anotados con @RequiereRol o @SoloLectura son interceptados
 * automáticamente sin que el servicio tenga que hacer nada extra.
 *
 * Hay dos interceptores definidos:
 *   1. @RequiereRol  → verifica que el usuario tenga uno de los roles permitidos
 *   2. @SoloLectura  → bloquea al rol DIRECCION si intenta ejecutar operaciones de escritura
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ScopeValidationAspect {

    // Necesitamos el logger de auditoría para registrar los intentos de acceso no autorizado
    private final AuditoriaLogger auditoriaLogger;

    /**
     * Intercepta todos los métodos anotados con @RequiereRol.
     *
     * Antes de ejecutar el método real, lee la anotación para saber qué roles
     * están permitidos y los compara con el rol del usuario autenticado.
     * Si el rol no coincide → registra el intento en la bitácora y lanza 403.
     * Si el rol sí coincide → deja pasar la ejecución con joinPoint.proceed().
     *
     * @param joinPoint  punto de intercepción que representa el método interceptado
     */
    @Around("@annotation(co.edu.cue.practicas.security.annotation.RequiereRol)")
    public Object validarRol(ProceedingJoinPoint joinPoint) throws Throwable {

        // Leemos la anotación @RequiereRol del método interceptado para saber qué roles acepta
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiereRol anotacion = method.getAnnotation(RequiereRol.class);

        // Obtenemos el usuario autenticado desde el contexto de seguridad de Spring
        CustomUserDetails userDetails = obtenerUsuarioActual();

        // Si no hay usuario autenticado (token inválido o expirado), bloqueamos
        if (userDetails == null) {
            throw new AccesoNoAutorizadoException("No autenticado.");
        }

        // Verificamos si el rol del usuario está dentro de los roles permitidos por la anotación
        boolean tieneRol = Arrays.asList(anotacion.roles()).contains(userDetails.getRol());

        if (!tieneRol) {
            // El usuario está autenticado pero no tiene el rol requerido → registramos y bloqueamos
            registrarAccesoNegado(userDetails, method.getName());
            throw new AccesoNoAutorizadoException(
                    "Acceso denegado: el rol " + userDetails.getRol() + " no tiene permiso para esta operación.");
        }

        // El usuario tiene el rol correcto → dejamos que el método se ejecute normalmente
        return joinPoint.proceed();
    }

    /**
     * Intercepta todos los métodos anotados con @SoloLectura.
     *
     * Si el usuario autenticado tiene el rol DIRECCION, se bloquea la operación
     * porque ese rol solo puede leer datos, nunca modificarlos.
     * Para cualquier otro rol, se deja pasar la ejecución normalmente.
     *
     * @param joinPoint  punto de intercepción que representa el método interceptado
     */
    @Around("@annotation(co.edu.cue.practicas.security.annotation.SoloLectura)")
    public Object bloquearEscrituraDireccion(ProceedingJoinPoint joinPoint) throws Throwable {
        CustomUserDetails userDetails = obtenerUsuarioActual();

        // Solo bloqueamos al rol DIRECCION; los demás roles pueden ejecutar el método
        if (userDetails != null && Rol.DIRECCION.equals(userDetails.getRol())) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            registrarAccesoNegado(userDetails, signature.getMethod().getName());
            throw new AccesoNoAutorizadoException(
                    "El rol Dirección solo tiene acceso de lectura. Operación bloqueada.");
        }

        // El usuario no es DIRECCION → dejamos que el método se ejecute normalmente
        return joinPoint.proceed();
    }

    /**
     * Obtiene el usuario autenticado desde el SecurityContext de Spring.
     * Retorna null si no hay sesión activa o si el principal no es del tipo esperado.
     */
    private CustomUserDetails obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof CustomUserDetails ud) return ud;
        return null;
    }

    /**
     * Extrae la IP del cliente desde el contexto de la petición HTTP actual.
     * Si no hay petición en curso (ej. llamada interna), retorna "desconocida".
     */
    private String obtenerIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) return attrs.getRequest().getRemoteAddr();
        } catch (Exception ignored) {}
        return "desconocida";
    }

    /**
     * Registra en la bitácora el intento de acceso no autorizado.
     * Si el propio registro falla, solo loguea el error pero no lanza excepción,
     * para que el 403 original siga propagándose sin enmascararse.
     *
     * @param userDetails  usuario que intentó el acceso
     * @param metodo       nombre del método al que intentó acceder
     */
    private void registrarAccesoNegado(CustomUserDetails userDetails, String metodo) {
        String modulo = "Service." + metodo;
        try {
            auditoriaLogger.registrarAccesoNegado(
                    userDetails.getUsuario(),
                    modulo,
                    obtenerIp()
            );
        } catch (Exception e) {
            log.error("[PROXY] Error registrando acceso negado en bitácora: {}", e.getMessage());
        }
    }
}
