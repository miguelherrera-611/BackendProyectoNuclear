package co.edu.cue.practicas.service.dashboard;

import co.edu.cue.practicas.dto.response.DashboardResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * PATRON MEDIATOR — GPE-131
 *
 * Delega la resolución del dashboard al DashboardMediator,
 * manteniendo el servicio delgado y el mediador enfocado
 * en la lógica de coordinación.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMediator dashboardMediator;

    public DashboardResponse obtenerDashboard(CustomUserDetails userDetails) {
        return dashboardMediator.resolverDashboard(userDetails);
    }
}
