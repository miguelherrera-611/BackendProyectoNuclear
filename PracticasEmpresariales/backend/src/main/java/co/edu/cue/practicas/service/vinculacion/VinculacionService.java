package co.edu.cue.practicas.service.vinculacion;

import co.edu.cue.practicas.dto.request.ConfirmarVinculacionRequest;
import co.edu.cue.practicas.event.VinculacionConfirmadaEvent;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VinculacionService {

    private final InstanciaPracticaRepository instanciaPracticaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaLogger auditoriaLogger;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InstanciaPractica confirmarVinculacion(Long instanciaId, ConfirmarVinculacionRequest req, CustomUserDetails actor) {
        validarRol(actor);

        InstanciaPractica instancia = instanciaPracticaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia de práctica no encontrada."));

        if (req.docenteAsesorId() != null) {
            Usuario docente = usuarioRepository.findById(req.docenteAsesorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Docente asesor no encontrado."));
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
                .valoresNuevos("{\"estado\":\"EN_CURSO\"}")
                );

        eventPublisher.publishEvent(new VinculacionConfirmadaEvent(this, instancia));
        return instancia;
    }

    private void validarRol(CustomUserDetails actor) {
        if (actor == null || (actor.getRol() != Rol.COORDINADOR_PRACTICAS && actor.getRol() != Rol.COORDINACION_ACADEMICA)) {
            throw new AccesoNoAutorizadoException("No tiene permiso para confirmar vinculaciones.");
        }
    }
}

