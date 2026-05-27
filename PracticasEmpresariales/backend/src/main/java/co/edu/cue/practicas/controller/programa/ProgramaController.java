package co.edu.cue.practicas.controller.programa;

import co.edu.cue.practicas.dto.request.CrearProgramaRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.ProgramaResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.programa.ProgramaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/programas")
@RequiredArgsConstructor
public class ProgramaController {

    private final ProgramaService programaService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProgramaResponse>> crear(
            @Valid @RequestBody CrearProgramaRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Programa creado", programaService.crearPrograma(request, userDetails)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProgramaResponse>>> listar(
            @PageableDefault(sort = "nombre") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(programaService.listar(pageable)));
    }

    @GetMapping("/por-facultad/{facultadId}")
    public ResponseEntity<ApiResponse<List<ProgramaResponse>>> listarPorFacultad(@PathVariable Long facultadId) {
        return ResponseEntity.ok(ApiResponse.ok(programaService.listarPorFacultad(facultadId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgramaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(programaService.obtenerPorId(id)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        programaService.desactivarPrograma(id, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Programa desactivado", null));
    }
}
