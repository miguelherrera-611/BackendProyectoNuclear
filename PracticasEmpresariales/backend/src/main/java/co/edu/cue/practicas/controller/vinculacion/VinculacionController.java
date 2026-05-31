package co.edu.cue.practicas.controller.vinculacion;

import co.edu.cue.practicas.dto.request.ConfirmarVinculacionRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.vinculacion.VinculacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vinculaciones")
@RequiredArgsConstructor
public class VinculacionController {

    private final VinculacionService service;

    @PatchMapping("/{instanciaId}/confirmar")
    public ResponseEntity<ApiResponse<InstanciaPractica>> confirmar(
            @PathVariable Long instanciaId,
            @Valid @RequestBody ConfirmarVinculacionRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Vinculación confirmada.",
                service.confirmarVinculacion(instanciaId, request, actor)));
    }
}

