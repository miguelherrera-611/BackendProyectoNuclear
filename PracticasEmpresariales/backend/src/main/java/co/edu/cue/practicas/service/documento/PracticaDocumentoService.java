package co.edu.cue.practicas.service.documento;

import co.edu.cue.practicas.dto.response.PracticaDocumentoResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.PracticaDocumento;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.model.enums.TipoDocumento;
import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.repository.documento.PracticaDocumentoRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticaDocumentoService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final List<String> MIME_PERMITIDOS = List.of("application/pdf", "image/jpeg", "image/png");

    private final PracticaDocumentoRepository documentoRepository;
    private final InstanciaPracticaRepository instanciaPracticaRepository;
    private final AuditoriaLogger auditoriaLogger;

    @Transactional
    public PracticaDocumentoResponse subirDocumento(Long instanciaPracticaId,
                                                    TipoDocumento tipo,
                                                    MultipartFile archivo,
                                                    CustomUserDetails actor) {
        validarAcceso(actor);
        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaPracticaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia de practica no encontrada."));

        if (instancia.esInmutable()) {
            throw new OperacionNoPermitidaException("Los documentos de una practica FINALIZADA o CANCELADA son inmutables.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new OperacionNoPermitidaException("El archivo es obligatorio.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new OperacionNoPermitidaException("El archivo supera el tamano maximo permitido de 10MB.");
        }

        // getContentType() puede retornar null — se valida explícitamente antes de usar
        String rawContentType = archivo.getContentType();
        String mimeType = rawContentType != null ? rawContentType.toLowerCase(Locale.ROOT) : "";
        if (mimeType.isEmpty() || !MIME_PERMITIDOS.contains(mimeType) || !tipo.admiteMimeType(mimeType)) {
            throw new OperacionNoPermitidaException("Tipo de archivo no permitido. Solo PDF, JPG o PNG.");
        }

        // getOriginalFilename() puede retornar null — se usa valor por defecto
        String originalFilename = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename() : "archivo";
        String safeName = UUID.randomUUID() + "_" + Paths.get(originalFilename).getFileName();
        Path baseDir = Paths.get("uploads", "practicas", String.valueOf(instanciaPracticaId));
        Path target = baseDir.resolve(safeName);

        try {
            Files.createDirectories(baseDir);
            Files.write(target, archivo.getBytes());
        } catch (IOException e) {
            throw new OperacionNoPermitidaException("No fue posible guardar el archivo: " + e.getMessage());
        }

        PracticaDocumento documento = PracticaDocumento.builder()
                .instanciaPractica(instancia)
                .tipo(tipo)
                .nombreOriginal(originalFilename)
                .rutaArchivo(target.toString().replace('\\', '/'))
                .mimeType(mimeType)
                .tamanoBytes(archivo.getSize())
                .build();

        documentoRepository.save(documento);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .modulo("PracticaDocumentoService")
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(documento.getId())
                .registroAfectadoTipo(PracticaDocumento.class.getSimpleName())
                .valoresNuevos("{\"instanciaPracticaId\":" + instanciaPracticaId + ",\"tipo\":\"" + tipo + "\"}"));

        return PracticaDocumentoResponse.desde(documento);
    }

    @Transactional
    public List<PracticaDocumentoResponse> listarDocumentos(Long instanciaPracticaId,
                                                            TipoDocumento tipo,
                                                            CustomUserDetails actor) {
        validarAcceso(actor);
        if (!instanciaPracticaRepository.existsById(instanciaPracticaId)) {
            throw new RecursoNoEncontradoException("Instancia de practica no encontrada.");
        }
        List<PracticaDocumento> docs = documentoRepository
                .findByInstanciaPractica_IdOrderByCreadoEnDesc(instanciaPracticaId);
        if (tipo != null) {
            docs = docs.stream().filter(d -> d.getTipo() == tipo).toList();
        }
        return docs.stream().map(PracticaDocumentoResponse::desde).toList();
    }

    @Transactional
    public List<PracticaDocumentoResponse> listarMisDocumentos(CustomUserDetails actor) {
        if (actor.getRol() != Rol.ESTUDIANTE)
            throw new AccesoNoAutorizadoException("Este endpoint es solo para estudiantes.");

        return instanciaPracticaRepository
                .findTopByExpediente_Estudiante_IdOrderByCreadoEnDesc(actor.getId())
                .map(instancia -> documentoRepository
                        .findByInstanciaPractica_IdOrderByCreadoEnDesc(instancia.getId())
                        .stream().map(PracticaDocumentoResponse::desde).toList())
                .orElse(List.of());
    }

    @Transactional
    public void eliminarDocumento(Long documentoId, CustomUserDetails actor) {
        PracticaDocumento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado."));

        Rol rol = actor.getRol();
        if (rol != Rol.COORDINADOR_PRACTICAS && rol != Rol.ADMIN_DTI) {
            if (rol != Rol.ESTUDIANTE)
                throw new AccesoNoAutorizadoException("No tiene permiso para eliminar documentos.");
            InstanciaPractica inst = doc.getInstanciaPractica();
            if (inst == null || inst.getExpediente() == null
                    || !inst.getExpediente().getEstudiante().getId().equals(actor.getId()))
                throw new AccesoNoAutorizadoException("No tiene permiso para eliminar este documento.");
        }

        documentoRepository.deleteById(documentoId);
        try {
            Files.deleteIfExists(Path.of(doc.getRutaArchivo()));
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo fisico: {}", doc.getRutaArchivo());
        }
    }

    private void validarAcceso(CustomUserDetails actor) {
        if (actor == null) {
            throw new AccesoNoAutorizadoException("Debe iniciar sesion para gestionar documentos.");
        }
        Rol rol = actor.getRol();
        if (rol != Rol.ESTUDIANTE && rol != Rol.COORDINADOR_PRACTICAS
                && rol != Rol.DOCENTE_ASESOR && rol != Rol.TUTOR_EMPRESARIAL
                && rol != Rol.COORDINACION_ACADEMICA && rol != Rol.ADMIN_DTI) {
            throw new AccesoNoAutorizadoException("No tiene permiso para gestionar documentos de practica.");
        }
    }
}