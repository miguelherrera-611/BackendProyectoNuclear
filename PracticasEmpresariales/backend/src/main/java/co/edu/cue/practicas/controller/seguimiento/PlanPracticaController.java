package co.edu.cue.practicas.controller.seguimiento;

import co.edu.cue.practicas.dto.request.AprobarRechazarPlanRequest;
import co.edu.cue.practicas.dto.request.CrearPlanRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.PlanPracticaResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.seguimiento.PlanPracticaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GPE-167 — Endpoints para gestión del plan de práctica.
 * SOLID — SRP: solo delega a PlanPracticaService.
 */
@RestController
@RequestMapping("/api/v1/planes-practica")
@RequiredArgsConstructor
public class PlanPracticaController {

    private final PlanPracticaService service;

    @PutMapping("/{instanciaId}")
    public ResponseEntity<ApiResponse<PlanPracticaResponse>> crearOActualizar(
            @PathVariable Long instanciaId,
            @Valid @RequestBody CrearPlanRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Plan de práctica guardado.",
                service.crearOActualizarPlan(instanciaId, request, actor)));
    }

    @GetMapping("/{instanciaId}/actual")
    public ResponseEntity<ApiResponse<PlanPracticaResponse>> actual(
            @PathVariable Long instanciaId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Plan de práctica actual.",
                service.obtenerPlanActual(instanciaId, actor)));
    }

    @GetMapping("/{instanciaId}/historial")
    public ResponseEntity<ApiResponse<List<PlanPracticaResponse>>> historial(
            @PathVariable Long instanciaId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Historial de planes.",
                service.listarPlanes(instanciaId, actor)));
    }

    @PatchMapping("/{planId}/aprobar-tutor")
    public ResponseEntity<ApiResponse<PlanPracticaResponse>> aprobarTutor(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Plan aprobado por tutor.",
                service.aprobarPorTutor(planId, actor)));
    }

    @PatchMapping("/{planId}/aprobar-docente")
    public ResponseEntity<ApiResponse<PlanPracticaResponse>> aprobarDocente(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Plan aprobado por docente.",
                service.aprobarPorDocente(planId, actor)));
    }

    @PatchMapping("/{planId}/rechazar")
    public ResponseEntity<ApiResponse<PlanPracticaResponse>> rechazar(
            @PathVariable Long planId,
            @RequestBody AprobarRechazarPlanRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Plan rechazado.",
                service.rechazarPlan(planId, request, actor)));
    }
}
