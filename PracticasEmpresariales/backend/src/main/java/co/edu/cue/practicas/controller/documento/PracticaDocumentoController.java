package co.edu.cue.practicas.controller.documento;

import co.edu.cue.practicas.dto.response.ApiResponse;
import co.edu.cue.practicas.dto.response.PracticaDocumentoResponse;
import co.edu.cue.practicas.model.enums.TipoDocumento;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.documento.PracticaDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documentos-practica")
@RequiredArgsConstructor
public class PracticaDocumentoController {

	private final PracticaDocumentoService service;

	@PostMapping(value = "/{instanciaPracticaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<PracticaDocumentoResponse>> subir(
			@PathVariable Long instanciaPracticaId,
			@RequestParam TipoDocumento tipo,
			@RequestPart("archivo") MultipartFile archivo,
			@AuthenticationPrincipal CustomUserDetails actor) {
		return ResponseEntity.ok(ApiResponse.ok("Documento subido.",
				service.subirDocumento(instanciaPracticaId, tipo, archivo, actor)));
	}

	@GetMapping("/{instanciaPracticaId}")
	public ResponseEntity<ApiResponse<List<PracticaDocumentoResponse>>> listar(
			@PathVariable Long instanciaPracticaId,
			@RequestParam(required = false) TipoDocumento tipo,
			@AuthenticationPrincipal CustomUserDetails actor) {
		return ResponseEntity.ok(ApiResponse.ok("Documentos de práctica.",
				service.listarDocumentos(instanciaPracticaId, tipo, actor)));
	}

	/** Documentos de la práctica activa del estudiante autenticado. */
	@GetMapping("/mis-documentos")
	public ResponseEntity<ApiResponse<List<PracticaDocumentoResponse>>> misDocumentos(
			@AuthenticationPrincipal CustomUserDetails actor) {
		return ResponseEntity.ok(ApiResponse.ok("Mis documentos.",
				service.listarMisDocumentos(actor)));
	}

	/** Elimina un documento por ID (coordinador o estudiante dueño). */
	@DeleteMapping("/{documentoId}")
	public ResponseEntity<ApiResponse<Void>> eliminar(
			@PathVariable Long documentoId,
			@AuthenticationPrincipal CustomUserDetails actor) {
		service.eliminarDocumento(documentoId, actor);
		return ResponseEntity.ok(ApiResponse.ok("Documento eliminado.", null));
	}
}


