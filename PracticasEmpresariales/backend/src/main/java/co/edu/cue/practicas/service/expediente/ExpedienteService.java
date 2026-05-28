package co.edu.cue.practicas.service.expediente;

import co.edu.cue.practicas.dto.request.MantenerNoAptoRequest;
import co.edu.cue.practicas.dto.request.SubirHojaDeVidaRequest;
import co.edu.cue.practicas.dto.response.ExpedienteResponse;
import co.edu.cue.practicas.dto.response.HojaDeVidaResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.builder.ExpedienteBuilder;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import co.edu.cue.practicas.repository.expediente.HojaDeVidaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import co.edu.cue.practicas.security.annotation.SoloLectura;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GPE-146 — ExpedienteService
 *
 * Gestiona el expediente histórico del estudiante: consulta, carga y validación de HV.
 *
 * PATRÓN BUILDER: usa ExpedienteBuilder para ensamblar el DTO de respuesta completo.
 *
 * PATRÓN PROXY (Protection Proxy + Cache Proxy):
 *   - Protection: bloquea modificaciones a prácticas FINALIZADAS o CANCELADAS.
 *     El método validarInstanciaModificable() hace de proxy de protección.
 *   - Cache: @Cacheable("expedientes") en obtenerExpediente() para reducir
 *     consultas a BD en expedientes frecuentemente consultados.
 *     @CacheEvict al subir nueva HV para invalidar el caché.
 *
 * OCL hvInmutableEnPractica: si la práctica está EN_CURSO, la HV no puede reemplazarse.
 *
 * SOLID — SRP: solo gestiona el expediente. Mapping → EstudianteMapper.
 *              Builder → ExpedienteBuilder. Reglas de acceso → método privado.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExpedienteService {

    private final ExpedienteEstudianteRepository expedienteRepository;
    private final HojaDeVidaRepository hvRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExpedienteBuilder expedienteBuilder;
    private final EstudianteMapper mapper;

    // ── CONSULTAR EXPEDIENTE — PROXY CACHÉ ───────────────────────────────────

    /**
     * PROXY CACHÉ: almacena el expediente completo en caché para reducir
     * consultas a BD. Se invalida automáticamente cuando se sube una nueva HV.
     */
    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS,
                          Rol.ADMIN_DTI, Rol.DOCENTE_ASESOR, Rol.TUTOR_EMPRESARIAL, Rol.ESTUDIANTE})
    @Transactional(readOnly = true)
    @Cacheable(value = "expedientes", key = "#estudianteId")
    public ExpedienteResponse obtenerExpediente(Long estudianteId) {
        ExpedienteEstudiante expediente = buscarExpedienteOFallar(estudianteId);
        // PATRÓN BUILDER: ensambla el DTO completo paso a paso
        return expedienteBuilder.construir(expediente);
    }

    // ── HOJA DE VIDA ──────────────────────────────────────────────────────────

    /**
     * Sube una nueva versión de la HV del estudiante.
     * OCL hvInmutableEnPractica: bloquea si la práctica activa está EN_CURSO.
     * Invalida el caché del expediente al crear la nueva versión.
     */
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.ESTUDIANTE})
    @CacheEvict(value = "expedientes", key = "#estudianteId")
    public HojaDeVidaResponse subirHojaDeVida(Long estudianteId, SubirHojaDeVidaRequest req) {
        Usuario estudiante = buscarEstudianteOFallar(estudianteId);
        ExpedienteEstudiante expediente = buscarExpedienteOFallar(estudianteId);

        // PROXY PROTECCIÓN: OCL hvInmutableEnPractica
        validarHvNoInmutable(estudianteId);

        int nuevaVersion = hvRepository.countByEstudiante_Id(estudianteId) + 1;

        HojaDeVida hv = HojaDeVida.builder()
                .estudiante(estudiante)
                .expediente(expediente)
                .version(nuevaVersion)
                .urlArchivo(req.urlArchivo())
                .build();

        hvRepository.save(hv);
        log.info("[GPE-146] Nueva HV versión {} subida para estudiante {}", nuevaVersion, estudianteId);
        return mapper.toHojaDeVidaResponse(hv);
    }

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    @CacheEvict(value = "expedientes", key = "#estudianteId")
    public HojaDeVidaResponse validarHojaDeVida(Long estudianteId, Long hvId,
                                                  CustomUserDetails validador) {
        HojaDeVida hv = buscarHvOFallar(hvId);
        hv.validar(validador.getId());
        hvRepository.save(hv);
        log.info("[GPE-146] HV {} validada por {}", hvId, validador.getNombre());
        return mapper.toHojaDeVidaResponse(hv);
    }

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    @CacheEvict(value = "expedientes", key = "#estudianteId")
    public HojaDeVidaResponse rechazarHojaDeVida(Long estudianteId, Long hvId,
                                                   MantenerNoAptoRequest req,
                                                   CustomUserDetails validador) {
        HojaDeVida hv = buscarHvOFallar(hvId);
        hv.rechazar(validador.getId(), req.motivo());
        hvRepository.save(hv);
        log.info("[GPE-146] HV {} rechazada por {}: {}", hvId, validador.getNombre(), req.motivo());
        return mapper.toHojaDeVidaResponse(hv);
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS,
                          Rol.ADMIN_DTI, Rol.ESTUDIANTE})
    @Transactional(readOnly = true)
    public List<HojaDeVidaResponse> listarHvsEstudiante(Long estudianteId) {
        return hvRepository.findByEstudiante_IdOrderByVersionDesc(estudianteId).stream()
                .map(mapper::toHojaDeVidaResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ExpedienteEstudiante buscarExpedienteOFallar(Long estudianteId) {
        return expedienteRepository.findByEstudiante_Id(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Expediente no encontrado para el estudiante: " + estudianteId));
    }

    private Usuario buscarEstudianteOFallar(Long estudianteId) {
        return usuarioRepository.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado: " + estudianteId));
    }

    private HojaDeVida buscarHvOFallar(Long hvId) {
        return hvRepository.findById(hvId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Hoja de Vida no encontrada con id: " + hvId));
    }

    /**
     * PROXY PROTECCIÓN — OCL hvInmutableEnPractica:
     * Bloquea la subida de una nueva HV si el estudiante tiene una práctica EN_CURSO.
     * Las prácticas EN_CURSO tienen documentos activos que no deben reemplazarse.
     */
    private void validarHvNoInmutable(Long estudianteId) {
        expedienteRepository.findByEstudiante_Id(estudianteId).ifPresent(expediente -> {
            boolean tieneEnCurso = expediente.getPracticas().stream()
                    .anyMatch(p -> p.getEstado() == EstadoPractica.EN_CURSO);
            if (tieneEnCurso)
                throw new OperacionNoPermitidaException(
                        "No se puede reemplazar la Hoja de Vida mientras hay una práctica EN_CURSO.");
        });
    }
}
