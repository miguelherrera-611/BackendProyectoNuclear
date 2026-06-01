package co.edu.cue.practicas.controller.vinculacion;

import co.edu.cue.practicas.dto.request.ConfirmarVinculacionRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.vinculacion.VinculacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GPE-163 / GPE-164 / GPE-167 — Vinculación, tablero de seguimiento y práctica del estudiante.
 */
@RestController
@RequestMapping("/api/v1/vinculaciones")
@RequiredArgsConstructor
public class VinculacionController {

    private final VinculacionService service;

    /** GPE-164 — Confirma vinculación y activa EN_CURSO */
    @PatchMapping("/{instanciaId}/confirmar")
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> confirmar(
            @PathVariable Long instanciaId,
            @Valid @RequestBody ConfirmarVinculacionRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Vinculación confirmada.",
                service.confirmarVinculacion(instanciaId, request, actor)));
    }

    /** GPE-163 — Registra una firma individual */
    @PatchMapping("/{instanciaId}/firmas/{tipoFirma}")
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> registrarFirma(
            @PathVariable Long instanciaId,
            @PathVariable String tipoFirma,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Firma registrada.",
                service.registrarFirma(instanciaId, tipoFirma, actor)));
    }

    /** GPE-167 — Tablero de seguimiento general */
    @GetMapping("/tablero")
    public ResponseEntity<ApiResponse<List<InstanciaPracticaResponse>>> tablero(
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Tablero de seguimiento.",
                service.listarPracticasEnCurso(actor)));
    }

    /** GPE-168 — Prácticas activas del docente asesor */
    @GetMapping("/mis-practicantes")
    public ResponseEntity<ApiResponse<List<InstanciaPracticaResponse>>> misPracticantes(
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Practicantes asignados.",
                service.listarPracticasDeDocente(actor)));
    }

    /** GPE-132 — Práctica activa del estudiante autenticado */
    @GetMapping("/mi-practica")
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> miPractica(
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Práctica actual.",
                service.obtenerMiPractica(actor)));
    }

    /** Detalle de una instancia (para coordinador, docente o estudiante con acceso) */
    @GetMapping("/{instanciaId}")
    public ResponseEntity<ApiResponse<InstanciaPracticaResponse>> detalle(
            @PathVariable Long instanciaId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Detalle de instancia.",
                service.obtenerInstancia(instanciaId, actor)));
    }
}

