package co.edu.cue.practicas.controller.empresa;

import co.edu.cue.practicas.dto.request.CrearEmpresaRequest;
import co.edu.cue.practicas.dto.request.RechazarRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.EmpresaResponse;
import co.edu.cue.practicas.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GPE-150 — EmpresaController
 *
 * SOLID — SRP: solo recibe peticiones HTTP y delega al servicio.
 *              No tiene lógica de negocio.
 */
@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmpresaResponse>> crear(
            @Valid @RequestBody CrearEmpresaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Empresa registrada.", empresaService.crearEmpresa(req)));
    }

    @PostMapping("/{id}/clonar")
    public ResponseEntity<ApiResponse<EmpresaResponse>> clonar(
            @PathVariable Long id,
            @RequestParam String razonSocial,
            @RequestParam String nit) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Empresa clonada.",
                        empresaService.crearDesdeEmpresaExistente(id, razonSocial, nit)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpresaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Empresa obtenida.", empresaService.obtenerPorId(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmpresaResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Listado de empresas.", empresaService.listarTodas()));
    }

    @GetMapping("/aprobadas")
    public ResponseEntity<ApiResponse<List<EmpresaResponse>>> listarAprobadas() {
        return ResponseEntity.ok(ApiResponse.ok("Empresas aprobadas.", empresaService.listarAprobadas()));
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<ApiResponse<EmpresaResponse>> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Empresa aprobada.", empresaService.aprobarEmpresa(id)));
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<ApiResponse<EmpresaResponse>> rechazar(
            @PathVariable Long id, @Valid @RequestBody RechazarRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Empresa rechazada.", empresaService.rechazarEmpresa(id, req)));
    }

    @PatchMapping("/{id}/inactivar")
    public ResponseEntity<ApiResponse<EmpresaResponse>> inactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Empresa inactivada.", empresaService.inactivarEmpresa(id)));
    }
}
