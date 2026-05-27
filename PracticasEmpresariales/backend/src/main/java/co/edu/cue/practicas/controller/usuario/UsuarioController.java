package co.edu.cue.practicas.controller.usuario;

import co.edu.cue.practicas.dto.request.CrearUsuarioRequest;
import co.edu.cue.practicas.dto.request.EditarUsuarioRequest;
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
}
