package co.edu.cue.practicas.controller.dashboard;

import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.DashboardResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * PATRON MEDIATOR — GPE-131
     *
     * El frontend llama a este endpoint único.
     * El DashboardMediator decide qué panel devolver según el rol.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> obtenerDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DashboardResponse dashboard = dashboardService.obtenerDashboard(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }
}
