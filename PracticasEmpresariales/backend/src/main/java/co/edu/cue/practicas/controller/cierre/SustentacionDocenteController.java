package co.edu.cue.practicas.controller.cierre;

import co.edu.cue.practicas.dto.request.AgendarSustentacionDocenteRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.cierre.SustentacionDocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sustentaciones-docente")
@RequiredArgsConstructor
public class SustentacionDocenteController {

    private final SustentacionDocenteService service;

    @PutMapping("/{instanciaId}")
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> agendar(
            @PathVariable Long instanciaId,
            @Valid @RequestBody AgendarSustentacionDocenteRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Fecha de sustentacion programada.",
                service.agendar(instanciaId, request.getFecha(), actor)));
    }

    @GetMapping("/{instanciaId}")
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> obtener(
            @PathVariable Long instanciaId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Datos de sustentacion.",
                service.obtener(instanciaId, actor)));
    }
}
