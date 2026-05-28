package co.edu.cue.practicas.controller.estudiante;

import co.edu.cue.practicas.dto.request.EnviarAlProcesoRequest;
import co.edu.cue.practicas.dto.request.MarcarAptoRequest;
import co.edu.cue.practicas.dto.request.MantenerNoAptoRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.UsuarioResponse;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.estudiante.EstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GPE-143 / GPE-145 / GPE-147 — EstudianteController
 * SOLID — SRP: solo delega peticiones HTTP al servicio.
 */
@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listar(
            @AuthenticationPrincipal CustomUserDetails usuario,
            @RequestParam(required = false) EstadoEstudiante estado,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Listado de estudiantes.", service.listarEstudiantes(usuario, estado, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Estudiante obtenido.", service.obtenerPorId(id)));
    }

    @PatchMapping("/{id}/marcar-apto")
    public ResponseEntity<ApiResponse<UsuarioResponse>> marcarApto(
            @PathVariable Long id,
            @Valid @RequestBody MarcarAptoRequest req,
            @AuthenticationPrincipal CustomUserDetails ejecutor) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Estudiante marcado como APTO.", service.marcarApto(id, req, ejecutor)));
    }

    @PatchMapping("/{id}/mantener-no-apto")
    public ResponseEntity<ApiResponse<UsuarioResponse>> mantenerNoApto(
            @PathVariable Long id,
            @Valid @RequestBody MantenerNoAptoRequest req,
            @AuthenticationPrincipal CustomUserDetails ejecutor) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Estudiante mantenido en NO_APTO.", service.mantenerNoApto(id, req, ejecutor)));
    }

    @PostMapping("/enviar-al-proceso")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> enviarAlProceso(
            @Valid @RequestBody EnviarAlProcesoRequest req,
            @AuthenticationPrincipal CustomUserDetails ejecutor) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Estudiantes enviados al proceso.", service.enviarAlProceso(req, ejecutor)));
    }
}
