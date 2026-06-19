package co.edu.cue.practicas.controller.usuario;

import co.edu.cue.practicas.dto.request.CrearUsuarioRequest;
import co.edu.cue.practicas.dto.request.EditarUsuarioRequest;
import co.edu.cue.practicas.dto.request.VincularEmpresaRequest;
import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.UsuarioResponse;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.usuario.UsuarioService;
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
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
            @Valid @RequestBody CrearUsuarioRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UsuarioResponse creado = usuarioService.crearUsuario(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario creado exitosamente", creado));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(usuarioService.listarUsuarios(pageable)));
    }

    /** Lista todos los docentes asesores activos. Accesible por COORDINADOR_PRACTICAS y ADMIN_DTI. */
    @GetMapping("/docentes")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarDocentes(
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Docentes asesores activos.", usuarioService.listarDocentesActivos()));
    }

    @GetMapping("/docentes/page")
    public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listarDocentesPaginado(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Docentes asesores activos.", usuarioService.listarDocentesActivos(pageable)));
    }

    /** Lista todos los tutores empresariales activos (usuarios con rol TUTOR_EMPRESARIAL). */
    @GetMapping("/tutores")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarTutores(
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Tutores empresariales activos.", usuarioService.listarTutoresActivos()));
    }

    @GetMapping("/tutores/page")
    public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listarTutoresPaginado(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok("Tutores empresariales activos.", usuarioService.listarTutoresActivos(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> editar(
            @PathVariable Long id,
            @Valid @RequestBody EditarUsuarioRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok("Usuario actualizado",
                usuarioService.editarUsuario(id, request, userDetails)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        usuarioService.desactivarUsuario(id, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado", null));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<Void>> activar(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        usuarioService.activarUsuario(id, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Usuario activado", null));
    }

    /** Vincula o desvincula una empresa a un tutor empresarial. Solo COORDINADOR_PRACTICAS. */
    @PatchMapping("/{id}/empresa")
    public ResponseEntity<ApiResponse<UsuarioResponse>> vincularEmpresa(
            @PathVariable Long id,
            @RequestBody VincularEmpresaRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok("Empresa vinculada correctamente.",
                usuarioService.vincularEmpresa(id, request.getEmpresaId(), userDetails)));
    }
}
