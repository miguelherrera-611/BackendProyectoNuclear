package co.edu.cue.practicas.controller.asignacion;

import co.edu.cue.practicas.dto.request.CancelarAsignacionRequest;
import co.edu.cue.practicas.dto.request.CrearAsignacionRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.asignacion.AsignacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GPE-157 / GPE-158 / GPE-159 — Asignaciones de estudiantes a vacantes.
 */
@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService asignacionService;

    /** GPE-157 — Asignar estudiante APTO a vacante disponible */
    @PostMapping
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> crear(
            @Valid @RequestBody CrearAsignacionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("Estudiante asignado correctamente.",
                asignacionService.asignar(request, user)));
    }

    /** GPE-158 — Listar asignaciones activas (con filtro opcional por estado) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InstanciaPracticaResponse>>> listar(
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("Asignaciones activas.",
                asignacionService.listarAsignaciones(estado, user)));
    }

    /** GPE-159 — Detalle de una asignación */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> detalle(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("Detalle de asignación.",
                asignacionService.obtenerAsignacion(id, user)));
    }

    /** GPE-158 — Cancelar asignación con motivo obligatorio (solo antes de vinculación) */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarAsignacionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        asignacionService.cancelarAsignacion(id, request.getMotivo(), user);
        return ResponseEntity.ok(ApiResponse.ok("Asignación cancelada.", null));
    }
}

