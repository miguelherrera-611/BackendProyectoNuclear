package co.edu.cue.practicas.controller.encuesta;

import co.edu.cue.practicas.dto.request.EnviarEncuestaRequest;
import co.edu.cue.practicas.dto.request.ResponderEncuestaRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.EncuestaResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.encuesta.EncuestaSatisfaccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encuestas-satisfaccion")
@RequiredArgsConstructor
public class EncuestaSatisfaccionController {

    private final EncuestaSatisfaccionService service;

    @GetMapping("/mis-encuestas")
    public ResponseEntity<ApiResponse<java.util.List<EncuestaResponse>>> misEncuestas(
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Encuestas asignadas.",
                service.misEncuestas(actor)));
    }

    @GetMapping("/publica/{token}")
    public ResponseEntity<ApiResponse<EncuestaResponse>> consultarPublica(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok("Encuesta publica.",
                service.consultarPorToken(token)));
    }

    @PostMapping("/{instanciaId}/tutor/enviar")
    public ResponseEntity<ApiResponse<EncuestaResponse>> enviarTutor(
            @PathVariable Long instanciaId,
            @Valid @RequestBody EnviarEncuestaRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Encuesta enviada al tutor.",
                service.enviarATutor(instanciaId, request, actor)));
    }

    @PostMapping("/{instanciaId}/estudiante/enviar")
    public ResponseEntity<ApiResponse<EncuestaResponse>> enviarEstudiante(
            @PathVariable Long instanciaId,
            @Valid @RequestBody EnviarEncuestaRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Encuesta enviada al estudiante.",
                service.enviarAEstudiante(instanciaId, request, actor)));
    }

    @PatchMapping("/{encuestaId}/borrador")
    public ResponseEntity<ApiResponse<EncuestaResponse>> guardarBorrador(
            @PathVariable Long encuestaId,
            @Valid @RequestBody ResponderEncuestaRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Borrador guardado.",
                service.guardarBorrador(encuestaId, request, actor)));
    }

    @PatchMapping("/{encuestaId}/completar")
    public ResponseEntity<ApiResponse<EncuestaResponse>> completar(
            @PathVariable Long encuestaId,
            @Valid @RequestBody ResponderEncuestaRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Encuesta completada.",
                service.completar(encuestaId, request, actor)));
    }

    @PatchMapping("/publica/{token}/completar")
    public ResponseEntity<ApiResponse<EncuestaResponse>> completarPublica(
            @PathVariable String token,
            @Valid @RequestBody ResponderEncuestaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Encuesta completada.",
                service.completarPorToken(token, request)));
    }
}
