package co.edu.cue.practicas.controller.reporte;

import co.edu.cue.practicas.dto.request.ReporteEstadoProcesoRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.ReporteEstadoProcesoResponse;
import co.edu.cue.practicas.dto.response.TableroGerencialResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.reporte.ReporteEstadoProcesoService;
import co.edu.cue.practicas.service.reporte.TableroGerencialDireccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes-sprint4")
@RequiredArgsConstructor
public class ReporteSprint4Controller {

    private final ReporteEstadoProcesoService reporteService;
    private final TableroGerencialDireccionService tableroService;

    @GetMapping("/estado-proceso")
    public ResponseEntity<ApiResponse<ReporteEstadoProcesoResponse>> estadoProceso(
            @ModelAttribute ReporteEstadoProcesoRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Reporte de estado del proceso.",
                reporteService.construir(request, actor)));
    }

    @GetMapping("/tablero-direccion")
    public ResponseEntity<ApiResponse<TableroGerencialResponse>> tableroDireccion(
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Tablero gerencial Direccion.",
                tableroService.consultar(actor)));
    }
}
