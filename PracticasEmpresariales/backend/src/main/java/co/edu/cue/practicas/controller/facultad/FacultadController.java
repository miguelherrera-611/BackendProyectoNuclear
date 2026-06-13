package co.edu.cue.practicas.controller.facultad;

import co.edu.cue.practicas.dto.request.CrearFacultadRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.FacultadResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.facultad.FacultadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/facultades")
@RequiredArgsConstructor
public class FacultadController {

    private final FacultadService facultadService;

    @PostMapping
    public ResponseEntity<ApiResponse<FacultadResponse>> crear(
            @Valid @RequestBody CrearFacultadRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Facultad creada", facultadService.crearFacultad(request, userDetails)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FacultadResponse>>> listar(
            @RequestParam(defaultValue = "false") boolean incluirInactivas,
            @PageableDefault(sort = "nombre") Pageable pageable) {

        Page<FacultadResponse> resultado = incluirInactivas
                ? facultadService.listarTodas(pageable)
                : facultadService.listar(pageable);
        return ResponseEntity.ok(ApiResponse.ok(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacultadResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(facultadService.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FacultadResponse>> editar(
            @PathVariable Long id,
            @Valid @RequestBody CrearFacultadRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok("Facultad actualizada",
                facultadService.editarFacultad(id, request, userDetails)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        facultadService.desactivarFacultad(id, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Facultad desactivada", null));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<Void>> activar(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        facultadService.activarFacultad(id, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Facultad activada", null));
    }
}
