package co.edu.cue.practicas.controller.evaluacion;

import co.edu.cue.practicas.dto.request.RegistrarEvaluacionFinalRequest;
import co.edu.cue.practicas.dto.request.RegistrarNotaFinalRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.EvaluacionFinalResponse;
import co.edu.cue.practicas.dto.response.NotaFinalResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.evaluacion.EvaluacionDocenteService;
import co.edu.cue.practicas.service.evaluacion.EvaluacionTutorService;
import co.edu.cue.practicas.service.evaluacion.NotaFinalCoordinadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/evaluaciones-finales")
@RequiredArgsConstructor
public class EvaluacionFinalController {

    private final EvaluacionDocenteService docenteService;
    private final EvaluacionTutorService tutorService;
    private final NotaFinalCoordinadorService notaFinalService;

    @PostMapping("/{instanciaId}/docente")
    public ResponseEntity<ApiResponse<EvaluacionFinalResponse>> registrarDocente(
            @PathVariable Long instanciaId,
            @Valid @RequestBody RegistrarEvaluacionFinalRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluacion del docente registrada.",
                docenteService.registrar(instanciaId, request, actor)));
    }

    @PostMapping("/{instanciaId}/tutor")
    public ResponseEntity<ApiResponse<EvaluacionFinalResponse>> registrarTutor(
            @PathVariable Long instanciaId,
            @Valid @RequestBody RegistrarEvaluacionFinalRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluacion del tutor registrada.",
                tutorService.registrar(instanciaId, request, actor)));
    }

    @PostMapping("/{instanciaId}/coordinador")
    public ResponseEntity<ApiResponse<NotaFinalResponse>> registrarNotaFinal(
            @PathVariable Long instanciaId,
            @Valid @RequestBody RegistrarNotaFinalRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Nota final registrada.",
                notaFinalService.registrar(instanciaId, request, actor)));
    }

    @GetMapping("/{instanciaId}/referencias")
    public ResponseEntity<ApiResponse<Map<String, Object>>> referencias(
            @PathVariable Long instanciaId,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluaciones de referencia.",
                notaFinalService.referencias(instanciaId, actor)));
    }
}
