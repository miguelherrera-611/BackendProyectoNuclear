package co.edu.cue.practicas.controller.empresa;

import co.edu.cue.practicas.dto.request.ActualizarTelefonoTutorRequest;
import co.edu.cue.practicas.dto.request.CrearTutorRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.TutorEmpresarialResponse;
import co.edu.cue.practicas.service.tutor.TutorEmpresarialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** GPE-151 */
@RestController
@RequestMapping("/api/v1/tutores")
@RequiredArgsConstructor
public class TutorEmpresarialController {

    private final TutorEmpresarialService tutorService;

    @PostMapping
    public ResponseEntity<ApiResponse<TutorEmpresarialResponse>> crear(
            @Valid @RequestBody CrearTutorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tutor registrado.", tutorService.crearTutor(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TutorEmpresarialResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Tutor obtenido.", tutorService.obtenerPorId(id)));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<ApiResponse<List<TutorEmpresarialResponse>>> listarPorEmpresa(
            @PathVariable Long empresaId) {
        return ResponseEntity.ok(ApiResponse.ok("Tutores.", tutorService.listarPorEmpresa(empresaId)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<TutorEmpresarialResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Tutor desactivado.", tutorService.desactivarTutor(id)));
    }

    @PatchMapping("/{id}/telefono")
    public ResponseEntity<ApiResponse<TutorEmpresarialResponse>> actualizarTelefono(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarTelefonoTutorRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Teléfono actualizado.", tutorService.actualizarTelefono(id, req)));
    }
}
