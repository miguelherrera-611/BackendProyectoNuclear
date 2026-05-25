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
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ScopeValidationAspect {

    private final AuditoriaLogger auditoriaLogger;

    /**
     * Intercepta métodos anotados con @RequiereRol.
     * Valida que el usuario autenticado tenga uno de los roles requeridos.
     */
    @Around("@annotation(co.edu.cue.practicas.security.annotation.RequiereRol)")
    public Object validarRol(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiereRol anotacion = method.getAnnotation(RequiereRol.class);

        CustomUserDetails userDetails = obtenerUsuarioActual();

        if (userDetails == null) {
            throw new AccesoNoAutorizadoException("No autenticado.");
        }

        boolean tieneRol = Arrays.asList(anotacion.roles()).contains(userDetails.getRol());

        if (!tieneRol) {
            registrarAccesoNegado(userDetails, method.getName());
            throw new AccesoNoAutorizadoException(
                    "Acceso denegado: el rol " + userDetails.getRol() + " no tiene permiso para esta operación.");
        }

        return joinPoint.proceed();
    }

    /**
     * Intercepta métodos no anotados como @SoloLectura que el rol DIRECCION
     * intenta ejecutar → bloqueados siempre.
     */
    @Around("@annotation(co.edu.cue.practicas.security.annotation.SoloLectura)")
    public Object bloquearEscrituraDireccion(ProceedingJoinPoint joinPoint) throws Throwable {
        CustomUserDetails userDetails = obtenerUsuarioActual();

        if (userDetails != null && Rol.DIRECCION.equals(userDetails.getRol())) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            registrarAccesoNegado(userDetails, signature.getMethod().getName());
            throw new AccesoNoAutorizadoException(
                    "El rol Dirección solo tiene acceso de lectura. Operación bloqueada.");
        }

        return joinPoint.proceed();
    }

    private CustomUserDetails obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof CustomUserDetails ud) return ud;
        return null;
    }

    private String obtenerIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) return attrs.getRequest().getRemoteAddr();
        } catch (Exception ignored) {}
        return "desconocida";
    }

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
