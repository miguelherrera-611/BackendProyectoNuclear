package co.edu.cue.practicas.service.vinculacion;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.ConfirmarVinculacionRequest;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.event.VinculacionConfirmadaEvent;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * GPE-163 / GPE-164 — Vinculación y activación EN_CURSO.
 * GPE-167 — Tablero de seguimiento (lista prácticas EN_CURSO).
 *
 * PATRON FACADE: activa + asigna docente + notifica en un solo punto.
 * PATRON STATE: delega transición a InstanciaPractica.
 * PATRON OBSERVER: publica VinculacionConfirmadaEvent → NotificacionEventListener notifica a todos los actores.
 */
@Service
@RequiredArgsConstructor
public class VinculacionService {

    private final InstanciaPracticaRepository instanciaPracticaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteMapper mapper;
    private final AuditoriaLogger auditoriaLogger;
    private final ApplicationEventPublisher eventPublisher;

    /** GPE-164 — Confirma vinculación y activa EN_CURSO con las tres firmas. */
    @Transactional
    public InstanciaPracticaResponse confirmarVinculacion(Long instanciaId,
                                                          ConfirmarVinculacionRequest req,
                                                          CustomUserDetails actor) {
        if (actor == null || actor.getRol() != Rol.COORDINADOR_PRACTICAS)
            throw new AccesoNoAutorizadoException("Solo el Coordinador de Prácticas puede confirmar la vinculación.");

        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia de práctica no encontrada."));

        if (req.docenteAsesorId() != null && instancia.getDocenteAsesor() == null) {
            Usuario docente = usuarioRepository.findById(req.docenteAsesorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Docente asesor no encontrado."));
            if (docente.getRol() != Rol.DOCENTE_ASESOR)
                throw new OperacionNoPermitidaException("El usuario indicado no es Docente Asesor.");
            instancia.setDocenteAsesor(docente);
        }

        instancia.confirmarVinculacion(
                req.fechaInicio(),
                req.fechaFin(),
                Boolean.TRUE.equals(req.firmaTutor()),
                Boolean.TRUE.equals(req.firmaDocente()),
                Boolean.TRUE.equals(req.firmaEstudiante())
        );

        instanciaPracticaRepository.save(instancia);

        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .modulo("VinculacionService")
                .tipoAccion(TipoAccion.CAMBIO_ESTADO)
                .registroAfectadoId(instancia.getId())
                .registroAfectadoTipo(InstanciaPractica.class.getSimpleName())
                .valoresNuevos("{\"estado\":\"EN_CURSO\",\"fechaInicio\":\"" + req.fechaInicio()
                        + "\",\"fechaFin\":\"" + req.fechaFin() + "\"}"));

        eventPublisher.publishEvent(new VinculacionConfirmadaEvent(this, instancia));
        return mapper.toInstanciaPracticaResponse(instancia);
    }

    /** GPE-163 — Registra una firma individual (TUTOR / DOCENTE / ESTUDIANTE) antes de la confirmación final. */
    @Transactional
    public InstanciaPracticaResponse registrarFirma(Long instanciaId, String tipoFirma, CustomUserDetails actor) {
        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia de práctica no encontrada."));

        switch (tipoFirma.toUpperCase()) {
            case "TUTOR" -> {
                if (actor.getRol() != Rol.TUTOR_EMPRESARIAL && actor.getRol() != Rol.COORDINADOR_PRACTICAS)
                    throw new AccesoNoAutorizadoException("No tiene permiso para registrar la firma del tutor.");
                instancia.setFirmaTutor(true);
            }
            case "DOCENTE" -> {
                if (actor.getRol() != Rol.DOCENTE_ASESOR && actor.getRol() != Rol.COORDINADOR_PRACTICAS)
                    throw new AccesoNoAutorizadoException("No tiene permiso para registrar la firma del docente.");
                instancia.setFirmaDocente(true);
            }
            case "ESTUDIANTE" -> {
                if (actor.getRol() != Rol.ESTUDIANTE && actor.getRol() != Rol.COORDINADOR_PRACTICAS)
                    throw new AccesoNoAutorizadoException("No tiene permiso para registrar la firma del estudiante.");
                instancia.setFirmaEstudiante(true);
            }
            default -> throw new OperacionNoPermitidaException("Tipo de firma no válido. Use: TUTOR, DOCENTE o ESTUDIANTE.");
        }

        instanciaPracticaRepository.save(instancia);
        return mapper.toInstanciaPracticaResponse(instancia);
    }

    /** GPE-167 — Tablero: todas las prácticas EN_CURSO del sistema. */
    @Transactional
    public List<InstanciaPracticaResponse> listarPracticasEnCurso(CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS && actor.getRol() != Rol.DOCENTE_ASESOR
                && actor.getRol() != Rol.ADMIN_DTI && actor.getRol() != Rol.DIRECCION)
            throw new AccesoNoAutorizadoException("No tiene acceso al tablero de seguimiento.");

        return instanciaPracticaRepository.findAllByEstado(EstadoPractica.EN_CURSO)
                .stream().map(mapper::toInstanciaPracticaResponse).toList();
    }

    /** GPE-168 — Prácticas activas asignadas a un docente asesor. */
    @Transactional
    public List<InstanciaPracticaResponse> listarPracticasDeDocente(CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR)
            throw new AccesoNoAutorizadoException("Solo el docente asesor puede consultar sus practicantes.");

        return instanciaPracticaRepository
                .findByDocenteAsesor_IdAndEstadoNotIn(actor.getId(),
                        List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA))
                .stream().map(mapper::toInstanciaPracticaResponse).toList();
    }

    /** GPE-132 — Práctica activa del estudiante autenticado. */
    @Transactional
    public InstanciaPracticaResponse obtenerMiPractica(CustomUserDetails actor) {
        if (actor.getRol() != Rol.ESTUDIANTE)
            throw new AccesoNoAutorizadoException("Solo el estudiante puede consultar su práctica activa.");

        return instanciaPracticaRepository
                .findTopByExpediente_Estudiante_IdAndEstadoOrderByCreadoEnDesc(actor.getId(), EstadoPractica.EN_CURSO)
                .or(() -> instanciaPracticaRepository
                        .findTopByExpediente_Estudiante_IdOrderByCreadoEnDesc(actor.getId()))
                .map(mapper::toInstanciaPracticaResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No tienes prácticas registradas aún."));
    }

    /** GPE-157 — Detalle de una instancia específica (coordinador o docente con acceso). */
    @Transactional
    public InstanciaPracticaResponse obtenerInstancia(Long instanciaId, CustomUserDetails actor) {
        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia de práctica no encontrada."));
        verificarAccesoInstancia(instancia, actor);
        return mapper.toInstanciaPracticaResponse(instancia);
    }

    private void verificarAccesoInstancia(InstanciaPractica instancia, CustomUserDetails actor) {
        Rol rol = actor.getRol();
        if (rol == Rol.COORDINADOR_PRACTICAS || rol == Rol.ADMIN_DTI || rol == Rol.DIRECCION) return;
        if (rol == Rol.DOCENTE_ASESOR) {
            if (instancia.getDocenteAsesor() == null || !instancia.getDocenteAsesor().getId().equals(actor.getId()))
                throw new AccesoNoAutorizadoException("No tiene acceso a esta instancia de práctica.");
            return;
        }
        if (rol == Rol.ESTUDIANTE) {
            if (instancia.getExpediente() == null || !instancia.getExpediente().getEstudiante().getId().equals(actor.getId()))
                throw new AccesoNoAutorizadoException("No tiene acceso a esta instancia de práctica.");
            return;
        }
        throw new AccesoNoAutorizadoException("No tiene permiso para consultar esta práctica.");
    }
}

