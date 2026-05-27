package co.edu.cue.practicas.service.dashboard;

import co.edu.cue.practicas.dto.response.DashboardResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * PATRON MEDIATOR — GPE-131
 *
 * Capa de servicio delgada que actúa como punto de entrada al DashboardMediator.
 * Separa la responsabilidad del controlador HTTP de la lógica de resolución del panel,
 * permitiendo que el DashboardMediator pueda usarse también desde otros contextos
 * sin pasar por el controlador REST.
 *
 * El DashboardMediator es quien contiene toda la lógica de qué panel
 * mostrar según el rol del usuario.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    // El Mediator concentra la lógica de qué panel corresponde a cada rol
    private final DashboardMediator dashboardMediator;

    /**
     * Retorna la estructura del dashboard correspondiente al rol del usuario autenticado.
     * Delega completamente al DashboardMediator la decisión de qué panel construir.
     *
     * @param userDetails  usuario autenticado con su rol y datos de perfil
     * @return estructura del panel con título, secciones y permisos de escritura
     */
    public DashboardResponse obtenerDashboard(CustomUserDetails userDetails) {
        return dashboardMediator.resolverDashboard(userDetails);
    }
}
