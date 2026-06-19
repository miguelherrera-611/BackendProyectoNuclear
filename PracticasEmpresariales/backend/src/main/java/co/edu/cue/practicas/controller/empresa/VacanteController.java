package co.edu.cue.practicas.controller.empresa;

import co.edu.cue.practicas.dto.request.CrearVacanteRequest;
import co.edu.cue.practicas.dto.request.RechazarRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.VacanteResponse;
import co.edu.cue.practicas.service.vacante.VacanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** GPE-152 / GPE-153 */
@RestController
@RequestMapping("/api/v1/vacantes")
@RequiredArgsConstructor
public class VacanteController {

    private final VacanteService vacanteService;

    @PostMapping
    public ResponseEntity<ApiResponse<VacanteResponse>> crear(
            @Valid @RequestBody CrearVacanteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Vacante creada.", vacanteService.crearVacante(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VacanteResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacante.", vacanteService.obtenerPorId(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VacanteResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Vacantes.", vacanteService.listarTodas()));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<VacanteResponse>>> listarPaginado(
            @PageableDefault(size = 20, sort = "fechaPublicacion") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Vacantes.", vacanteService.listarTodas(pageable)));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<ApiResponse<List<VacanteResponse>>> pendientes() {
        return ResponseEntity.ok(ApiResponse.ok("Pendientes.", vacanteService.listarPendientes()));
    }

    @GetMapping("/pendientes/page")
    public ResponseEntity<ApiResponse<Page<VacanteResponse>>> pendientesPaginado(
            @PageableDefault(size = 20, sort = "fechaPublicacion") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Pendientes.", vacanteService.listarPendientes(pageable)));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<ApiResponse<List<VacanteResponse>>> disponibles() {
        return ResponseEntity.ok(ApiResponse.ok("Disponibles.", vacanteService.listarDisponibles()));
    }

    @GetMapping("/disponibles/page")
    public ResponseEntity<ApiResponse<Page<VacanteResponse>>> disponiblesPaginado(
            @PageableDefault(size = 20, sort = "fechaPublicacion") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Disponibles.", vacanteService.listarDisponibles(pageable)));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<ApiResponse<List<VacanteResponse>>> porEmpresa(@PathVariable Long empresaId) {
        return ResponseEntity.ok(ApiResponse.ok("Vacantes empresa.", vacanteService.listarPorEmpresa(empresaId)));
    }

    @GetMapping("/empresa/{empresaId}/page")
    public ResponseEntity<ApiResponse<Page<VacanteResponse>>> porEmpresaPaginado(
            @PathVariable Long empresaId,
            @PageableDefault(size = 20, sort = "fechaPublicacion") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Vacantes empresa.", vacanteService.listarPorEmpresa(empresaId, pageable)));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<VacanteResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacante activada.", vacanteService.activarVacante(id)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<VacanteResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacante desactivada.", vacanteService.desactivarVacante(id)));
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<ApiResponse<VacanteResponse>> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacante aprobada.", vacanteService.activarVacante(id)));
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<ApiResponse<VacanteResponse>> rechazar(
            @PathVariable Long id, @Valid @RequestBody RechazarRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Vacante rechazada.", vacanteService.rechazarVacante(id, req)));
    }

    @PatchMapping("/{id}/cerrar")
    public ResponseEntity<ApiResponse<VacanteResponse>> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacante cerrada.", vacanteService.cerrarVacante(id)));
    }
}
