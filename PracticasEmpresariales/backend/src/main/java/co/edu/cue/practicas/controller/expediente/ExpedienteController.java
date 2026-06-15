package co.edu.cue.practicas.controller.expediente;

import co.edu.cue.practicas.dto.request.MantenerNoAptoRequest;
import co.edu.cue.practicas.dto.request.SubirHojaDeVidaRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.ExpedienteResponse;
import co.edu.cue.practicas.dto.response.HojaDeVidaResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.expediente.ExpedienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * GPE-146 — ExpedienteController
 * SOLID — SRP: solo delega peticiones HTTP al servicio.
 */
@RestController
@RequestMapping("/api/v1/expedientes")
@RequiredArgsConstructor
public class ExpedienteController {

    private final ExpedienteService service;

    @GetMapping("/{estudianteId}")
    public ResponseEntity<ApiResponse<ExpedienteResponse>> obtenerExpediente(
            @PathVariable Long estudianteId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Expediente obtenido.", service.obtenerExpediente(estudianteId)));
    }

    @GetMapping("/{estudianteId}/hoja-de-vida")
    public ResponseEntity<ApiResponse<List<HojaDeVidaResponse>>> listarHvs(
            @PathVariable Long estudianteId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Historial de HVs.", service.listarHvsEstudiante(estudianteId)));
    }

    @PostMapping("/{estudianteId}/hoja-de-vida")
    public ResponseEntity<ApiResponse<HojaDeVidaResponse>> subirHv(
            @PathVariable Long estudianteId,
            @Valid @RequestBody SubirHojaDeVidaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Hoja de Vida subida.", service.subirHojaDeVida(estudianteId, req)));
    }

    @PatchMapping("/{estudianteId}/hoja-de-vida/{hvId}/validar")
    public ResponseEntity<ApiResponse<HojaDeVidaResponse>> validarHv(
            @PathVariable Long estudianteId,
            @PathVariable Long hvId,
            @AuthenticationPrincipal CustomUserDetails validador) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Hoja de Vida validada.", service.validarHojaDeVida(estudianteId, hvId, validador)));
    }

    @PatchMapping("/{estudianteId}/hoja-de-vida/{hvId}/rechazar")
    public ResponseEntity<ApiResponse<HojaDeVidaResponse>> rechazarHv(
            @PathVariable Long estudianteId,
            @PathVariable Long hvId,
            @Valid @RequestBody MantenerNoAptoRequest req,
            @AuthenticationPrincipal CustomUserDetails validador) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Hoja de Vida rechazada.",
                service.rechazarHojaDeVida(estudianteId, hvId, req, validador)));
    }

    @GetMapping("/hoja-de-vida/{hvId}/archivo")
    public ResponseEntity<Resource> verArchivoHv(@PathVariable Long hvId) throws IOException {
        Resource resource = service.obtenerArchivoHv(hvId);
        String contentType = Files.probeContentType(Path.of(resource.getFile().getPath()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
