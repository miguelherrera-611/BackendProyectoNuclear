package co.edu.cue.practicas.controller.asignacion;

import co.edu.cue.practicas.dto.request.CrearAsignacionRequest;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.asignacion.AsignacionService;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST mínimo para la gestión de asignaciones (Sprint 3: GPE-157 / GPE-158)
 */
@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService asignacionService;

    @PostMapping
    public ResponseEntity<InstanciaPracticaResponse> crear(@Valid @RequestBody CrearAsignacionRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        InstanciaPracticaResponse dto = asignacionService.asignar(request, user);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id,
                                         @RequestParam(required = false) String motivo,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        asignacionService.cancelarAsignacion(id, motivo, user);
        return ResponseEntity.noContent().build();
    }

}

