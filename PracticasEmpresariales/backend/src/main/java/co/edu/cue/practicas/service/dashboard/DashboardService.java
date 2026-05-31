package co.edu.cue.practicas.service.dashboard;

import co.edu.cue.practicas.dto.response.DashboardResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Capa de servicio delgada que actúa como punto de entrada al DashboardMediator.
 * Mantiene SRP: solo coordina el cálculo de indicadores y la construcción del panel.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMediator dashboardMediator;
    private final DashboardIndicadorService dashboardIndicadorService;

    /**
     * Retorna la estructura del dashboard correspondiente al rol del usuario autenticado.
     * Delega completamente al DashboardMediator la decisión de qué panel construir.
     *
     * @param userDetails  usuario autenticado con su rol y datos de perfil
     * @return estructura del panel con título, secciones y permisos de escritura
     */
    public DashboardResponse obtenerDashboard(CustomUserDetails userDetails) {
        DashboardIndicadores indicadores = dashboardIndicadorService.obtenerIndicadores(userDetails);
        return dashboardMediator.resolverDashboard(userDetails, indicadores);
    }
}
