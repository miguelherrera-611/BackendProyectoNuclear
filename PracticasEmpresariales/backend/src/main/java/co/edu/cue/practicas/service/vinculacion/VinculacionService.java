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

    private static final String MSG_INSTANCIA_NO_ENCONTRADA = "Instancia de practica no encontrada.";

    private final InstanciaPracticaRepository instanciaPracticaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteMapper mapper;
    private final AuditoriaLogger auditoriaLogger;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InstanciaPracticaResponse confirmarVinculacion(Long instanciaId,
                                                          ConfirmarVinculacionRequest req,
                                                          CustomUserDetails actor) {
        if (actor == null || actor.getRol() != Rol.COORDINADOR_PRACTICAS)
            throw new AccesoNoAutorizadoException("Solo el Coordinador de Practicas puede confirmar la vinculacion.");

        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_INSTANCIA_NO_ENCONTRADA));

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

    @Transactional
    public InstanciaPracticaResponse registrarFirma(Long instanciaId, String tipoFirma, CustomUserDetails actor) {
        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_INSTANCIA_NO_ENCONTRADA));

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
            default -> throw new OperacionNoPermitidaException("Tipo de firma no valido. Use: TUTOR, DOCENTE o ESTUDIANTE.");
        }

        instanciaPracticaRepository.save(instancia);
        return mapper.toInstanciaPracticaResponse(instancia);
    }

    @Transactional
    public List<InstanciaPracticaResponse> listarPracticasEnCurso(CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS && actor.getRol() != Rol.DOCENTE_ASESOR
                && actor.getRol() != Rol.ADMIN_DTI && actor.getRol() != Rol.DIRECCION)
            throw new AccesoNoAutorizadoException("No tiene acceso al tablero de seguimiento.");

        return instanciaPracticaRepository.findAllByEstado(EstadoPractica.EN_CURSO)
                .stream().map(mapper::toInstanciaPracticaResponse).toList();
    }

    @Transactional
    public List<InstanciaPracticaResponse> listarMisPracticantes(CustomUserDetails actor) {
        if (actor.getRol() == Rol.DOCENTE_ASESOR) return listarPracticasDeDocente(actor);
        if (actor.getRol() == Rol.TUTOR_EMPRESARIAL) return listarPracticasDeTutor(actor);
        throw new AccesoNoAutorizadoException("Solo docente asesor o tutor empresarial pueden consultar sus practicantes.");
    }

    @Transactional
    public List<InstanciaPracticaResponse> listarPracticasDeDocente(CustomUserDetails actor) {
        if (actor.getRol() != Rol.DOCENTE_ASESOR)
            throw new AccesoNoAutorizadoException("Solo el docente asesor puede consultar sus practicantes.");

        return instanciaPracticaRepository
                .findByDocenteAsesor_IdAndEstadoNotIn(actor.getId(),
                        List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA))
                .stream().map(mapper::toInstanciaPracticaResponse).toList();
    }

    @Transactional
    public List<InstanciaPracticaResponse> listarPracticasDeTutor(CustomUserDetails actor) {
        if (actor.getRol() != Rol.TUTOR_EMPRESARIAL)
            throw new AccesoNoAutorizadoException("Solo el tutor empresarial puede consultar sus practicantes.");

        return instanciaPracticaRepository
                .findByTutorEmpresarial_CorreoIgnoreCaseAndEstadoNotIn(actor.getUsername(),
                        List.of(EstadoPractica.FINALIZADA, EstadoPractica.CANCELADA))
                .stream().map(mapper::toInstanciaPracticaResponse).toList();
    }

    @Transactional
    public InstanciaPracticaResponse obtenerMiPractica(CustomUserDetails actor) {
        if (actor.getRol() != Rol.ESTUDIANTE)
            throw new AccesoNoAutorizadoException("Solo el estudiante puede consultar su practica activa.");

        return instanciaPracticaRepository
                .findTopByExpediente_Estudiante_IdAndEstadoOrderByCreadoEnDesc(actor.getId(), EstadoPractica.EN_CURSO)
                .or(() -> instanciaPracticaRepository
                        .findTopByExpediente_Estudiante_IdOrderByCreadoEnDesc(actor.getId()))
                .map(mapper::toInstanciaPracticaResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No tienes practicas registradas aun."));
    }

    @Transactional
    public InstanciaPracticaResponse obtenerInstancia(Long instanciaId, CustomUserDetails actor) {
        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_INSTANCIA_NO_ENCONTRADA));
        verificarAccesoInstancia(instancia, actor);
        return mapper.toInstanciaPracticaResponse(instancia);
    }

    private void verificarAccesoInstancia(InstanciaPractica instancia, CustomUserDetails actor) {
        Rol rol = actor.getRol();
        if (rol == Rol.COORDINADOR_PRACTICAS || rol == Rol.ADMIN_DTI || rol == Rol.DIRECCION) return;
        if (rol == Rol.DOCENTE_ASESOR) {
            if (instancia.getDocenteAsesor() == null || !instancia.getDocenteAsesor().getId().equals(actor.getId()))
                throw new AccesoNoAutorizadoException("No tiene acceso a esta instancia de practica.");
            return;
        }
        if (rol == Rol.TUTOR_EMPRESARIAL) {
            if (instancia.getTutorEmpresarial() == null
                    || !instancia.getTutorEmpresarial().getCorreo().equalsIgnoreCase(actor.getUsername()))
                throw new AccesoNoAutorizadoException("No tiene acceso a esta instancia de practica.");
            return;
        }
        if (rol == Rol.ESTUDIANTE) {
            if (instancia.getExpediente() == null || !instancia.getExpediente().getEstudiante().getId().equals(actor.getId()))
                throw new AccesoNoAutorizadoException("No tiene acceso a esta instancia de practica.");
            return;
        }
        throw new AccesoNoAutorizadoException("No tiene permiso para consultar esta practica.");
    }
}