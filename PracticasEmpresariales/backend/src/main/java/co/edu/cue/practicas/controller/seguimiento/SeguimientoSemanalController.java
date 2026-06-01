package co.edu.cue.practicas.controller.seguimiento;

import co.edu.cue.practicas.dto.request.CrearSeguimientoRequest;
import co.edu.cue.practicas.dto.request.ObservacionDocenteRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.SeguimientoSemanalResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.seguimiento.SeguimientoSemanalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GPE-168 / GPE-170 — Endpoints para seguimientos semanales.
 * SOLID — SRP: solo delega a SeguimientoSemanalService.
 */
@RestController
@RequestMapping("/api/v1/seguimientos")
@RequiredArgsConstructor
public class SeguimientoSemanalController {

    private final SeguimientoSemanalService service;

    @PostMapping("/{instanciaId}")
    public ResponseEntity<ApiResponse<SeguimientoSemanalResponse>> crear(
            @PathVariable Long instanciaId,
            @Valid @RequestBody CrearSeguimientoRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Seguimiento registrado.",
                service.crearSeguimiento(instanciaId, request, actor)));
    }

    @PutMapping("/{seguimientoId}/editar")
    public ResponseEntity<ApiResponse<SeguimientoSemanalResponse>> editar(
            @PathVariable Long seguimientoId,
            @Valid @RequestBody CrearSeguimientoRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Seguimiento actualizado.",
                service.editarSeguimiento(seguimientoId, request, actor)));
    }

    @GetMapping("/{instanciaId}")
    public ResponseEntity<ApiResponse<List<SeguimientoSemanalResponse>>> listar(
            @PathVariable Long instanciaId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Seguimientos de la práctica.",
                service.listarPorInstancia(instanciaId, actor)));
    }

    @PatchMapping("/{seguimientoId}/aprobar")
    public ResponseEntity<ApiResponse<SeguimientoSemanalResponse>> aprobar(
            @PathVariable Long seguimientoId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Seguimiento aprobado.",
                service.aprobar(seguimientoId, actor)));
    }

    @PatchMapping("/{seguimientoId}/rechazar")
    public ResponseEntity<ApiResponse<SeguimientoSemanalResponse>> rechazar(
            @PathVariable Long seguimientoId,
            @Valid @RequestBody ObservacionDocenteRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Seguimiento rechazado con observaciones.",
                service.rechazar(seguimientoId, request, actor)));
    }
}
