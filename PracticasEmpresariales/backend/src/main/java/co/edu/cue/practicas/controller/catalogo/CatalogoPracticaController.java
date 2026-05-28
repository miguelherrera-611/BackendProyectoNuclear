package co.edu.cue.practicas.controller.catalogo;

import co.edu.cue.practicas.dto.request.CrearCatalogoPracticaRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.CatalogoPracticaResponse;
import co.edu.cue.practicas.service.catalogo.CatalogoPracticaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/programa/{programaId}/activos")
    public ResponseEntity<ApiResponse<List<CatalogoPracticaResponse>>> listarActivosPorPrograma(
            @PathVariable Long programaId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Entradas activas del catálogo.", service.listarActivosPorPrograma(programaId)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<CatalogoPracticaResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Entrada del catálogo desactivada.", service.desactivar(id)));
    }
}
