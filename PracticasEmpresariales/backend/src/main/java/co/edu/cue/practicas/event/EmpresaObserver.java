package co.edu.cue.practicas.event;

/**
 * PATRÓN OBSERVER — Interfaz del observador (IObserver).
 *
 * SOLID — DIP: todos los componentes dependen de esta abstracción,
 * nunca de implementaciones concretas.
 *
 * SOLID — OCP: se pueden agregar nuevos observers sin modificar
 * la entidad Empresa ni EmpresaService.
 *
 * Eventos posibles:
 *  "EMPRESA_APROBADA" | "EMPRESA_RECHAZADA" | "EMPRESA_INACTIVADA"
 */
public interface EmpresaObserver {
    void onEmpresaEvento(Long empresaId, String evento);
}
