package co.edu.cue.practicas.controller.auditoria;

import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.BitacoraResponse;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.auditoria.BitacoraAuditoriaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final BitacoraAuditoriaRepository bitacoraRepository;

    /**
     * La bitácora es de solo lectura para TODOS los roles incluido el DTI.
     * Solo el DTI puede acceder a este endpoint.
     */
    @GetMapping
    @RequiereRol(roles = {Rol.ADMIN_DTI})
    public ResponseEntity<ApiResponse<Page<BitacoraResponse>>> filtrar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) TipoAccion tipoAccion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String modulo,
            @PageableDefault(size = 50, sort = "fechaHora") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<BitacoraResponse> resultado = bitacoraRepository
                .filtrar(usuarioId, tipoAccion, desde, hasta, modulo, pageable)
                .map(BitacoraResponse::desde);

        return ResponseEntity.ok(ApiResponse.ok(resultado));
    }
}
