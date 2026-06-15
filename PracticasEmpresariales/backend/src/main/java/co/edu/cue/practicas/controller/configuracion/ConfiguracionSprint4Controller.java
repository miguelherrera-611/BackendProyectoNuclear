package co.edu.cue.practicas.controller.configuracion;

import co.edu.cue.practicas.dto.request.ConfigurarProgramaRequest;
import co.edu.cue.practicas.dto.request.PlantillaNotificacionRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.PlantillaNotificacionResponse;
import co.edu.cue.practicas.dto.response.ProgramaConfiguracionResponse;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.configuracion.ProgramaConfiguracionService;
import co.edu.cue.practicas.service.notificacion.NotificacionConfigurableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracion-sprint4")
@RequiredArgsConstructor
public class ConfiguracionSprint4Controller {

    private final ProgramaConfiguracionService programaConfiguracionService;
    private final NotificacionConfigurableService notificacionService;

    @PostMapping("/programas/{programaId}")
    public ResponseEntity<ApiResponse<ProgramaConfiguracionResponse>> configurarPrograma(
            @PathVariable Long programaId,
            @Valid @RequestBody ConfigurarProgramaRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Configuracion de programa registrada.",
                programaConfiguracionService.configurar(programaId, request, actor)));
    }

    @GetMapping("/programas/{programaId}")
    public ResponseEntity<ApiResponse<ProgramaConfiguracionResponse>> obtenerPrograma(@PathVariable Long programaId) {
        return ResponseEntity.ok(ApiResponse.ok("Configuracion vigente del programa.",
                programaConfiguracionService.obtener(programaId)));
    }

    @GetMapping("/notificaciones")
    public ResponseEntity<ApiResponse<java.util.List<PlantillaNotificacionResponse>>> listarPlantillas() {
        return ResponseEntity.ok(ApiResponse.ok("Plantillas configuradas.",
                notificacionService.listarPlantillas()));
    }

    @GetMapping("/notificaciones/{tipoEvento}")
    public ResponseEntity<ApiResponse<PlantillaNotificacionResponse>> obtenerPlantilla(
            @PathVariable TipoEventoNotificacion tipoEvento) {
        return ResponseEntity.ok(ApiResponse.ok("Plantilla consultada.",
                notificacionService.obtenerPlantilla(tipoEvento)));
    }

    @DeleteMapping("/notificaciones/{tipoEvento}")
    public ResponseEntity<ApiResponse<Void>> eliminarPlantilla(
            @PathVariable TipoEventoNotificacion tipoEvento,
            @AuthenticationPrincipal CustomUserDetails actor) {
        notificacionService.eliminarPlantilla(tipoEvento, actor);
        return ResponseEntity.ok(ApiResponse.ok("Plantilla eliminada.", null));
    }

    @PostMapping("/notificaciones")
    public ResponseEntity<ApiResponse<PlantillaNotificacionResponse>> guardarPlantilla(
            @Valid @RequestBody PlantillaNotificacionRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Plantilla de notificacion guardada.",
                notificacionService.guardarPlantilla(request, actor)));
    }

    @PostMapping("/notificaciones/previsualizar")
    public ResponseEntity<ApiResponse<String>> previsualizar(
            @Valid @RequestBody PlantillaNotificacionRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Previsualizacion generada.",
                notificacionService.previsualizar(request, Map.of(
                        "nombre_estudiante", "Estudiante Demo",
                        "empresa", "Empresa Demo",
                        "nombre_practica", "Practica Empresarial",
                        "enlace_encuesta", "https://cue.edu.co/encuesta",
                        "resultado", "APROBADO",
                        "nota_final", "4.5"), actor)));
    }
}
