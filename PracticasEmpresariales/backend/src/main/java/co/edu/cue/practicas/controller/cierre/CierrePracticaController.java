package co.edu.cue.practicas.controller.cierre;

import co.edu.cue.practicas.dto.request.EjecutarCierreRequest;
import co.edu.cue.practicas.dto.request.RegistrarResultadoSustentacionRequest;
import co.edu.cue.practicas.dto.request.RegistrarSustentacionRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.ChecklistCierreResponse;
import co.edu.cue.practicas.dto.response.CierreFormalResponse;
import co.edu.cue.practicas.dto.response.SustentacionResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.cierre.ChecklistCierreService;
import co.edu.cue.practicas.service.cierre.CierreFormalFacade;
import co.edu.cue.practicas.service.cierre.SustentacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cierre-practicas")
@RequiredArgsConstructor
public class CierrePracticaController {

    private final SustentacionService sustentacionService;
    private final ChecklistCierreService checklistService;
    private final CierreFormalFacade cierreFacade;

    @PostMapping("/{instanciaId}/sustentacion")
    public ResponseEntity<ApiResponse<SustentacionResponse>> programarSustentacion(
            @PathVariable Long instanciaId,
            @Valid @RequestBody RegistrarSustentacionRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Sustentacion programada.",
                sustentacionService.programar(instanciaId, request, actor)));
    }

    @PatchMapping("/{instanciaId}/sustentacion/resultado")
    public ResponseEntity<ApiResponse<SustentacionResponse>> resultadoSustentacion(
            @PathVariable Long instanciaId,
            @Valid @RequestBody RegistrarResultadoSustentacionRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Resultado de sustentacion registrado.",
                sustentacionService.registrarResultado(instanciaId, request, actor)));
    }

    @GetMapping("/{instanciaId}/checklist")
    public ResponseEntity<ApiResponse<ChecklistCierreResponse>> checklist(
            @PathVariable Long instanciaId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Checklist de cierre.",
                checklistService.generar(instanciaId, actor)));
    }

    @PostMapping("/{instanciaId}/ejecutar")
    public ResponseEntity<ApiResponse<CierreFormalResponse>> ejecutar(
            @PathVariable Long instanciaId,
            @Valid @RequestBody EjecutarCierreRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Cierre formal ejecutado.",
                cierreFacade.ejecutar(instanciaId, request, actor)));
    }
}
