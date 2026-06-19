package co.edu.cue.practicas.controller.catalogo;

import co.edu.cue.practicas.dto.request.ActualizarCatalogoPracticaRequest;
import co.edu.cue.practicas.dto.request.CrearCatalogoPracticaRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.CatalogoPracticaResponse;
import co.edu.cue.practicas.service.catalogo.CatalogoPracticaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GPE-141 — CatalogoPracticaController
 * SOLID — SRP: solo recibe peticiones HTTP y delega al servicio.
 */
@RestController
@RequestMapping("/api/v1/catalogo-practicas")
@RequiredArgsConstructor
public class CatalogoPracticaController {

    private final CatalogoPracticaService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoPracticaResponse>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.ok("Catálogos.", service.listarTodos()));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<CatalogoPracticaResponse>>> listarTodosPaginado(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Catalogos.", service.listarTodos(pageable)));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<CatalogoPracticaResponse>> crear(
            @Valid @RequestBody CrearCatalogoPracticaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Entrada del catálogo creada.", service.crearEntrada(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoPracticaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Catálogo obtenido.", service.obtenerPorId(id)));
    }

    @GetMapping("/programa/{programaId}")
    public ResponseEntity<ApiResponse<List<CatalogoPracticaResponse>>> listarPorPrograma(
            @PathVariable Long programaId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Catálogo del programa.", service.listarPorPrograma(programaId)));
    }

    @GetMapping("/programa/{programaId}/page")
    public ResponseEntity<ApiResponse<Page<CatalogoPracticaResponse>>> listarPorProgramaPaginado(
            @PathVariable Long programaId,
            @PageableDefault(size = 20, sort = "numeroPractica") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Catalogo del programa.", service.listarPorPrograma(programaId, pageable)));
    }
    @GetMapping("/programa/{programaId}/activos")
    public ResponseEntity<ApiResponse<List<CatalogoPracticaResponse>>> listarActivosPorPrograma(
            @PathVariable Long programaId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Entradas activas del catálogo.", service.listarActivosPorPrograma(programaId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoPracticaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCatalogoPracticaRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Catálogo actualizado.", service.actualizar(id, req)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<CatalogoPracticaResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Entrada del catálogo desactivada.", service.desactivar(id)));
    }
}
