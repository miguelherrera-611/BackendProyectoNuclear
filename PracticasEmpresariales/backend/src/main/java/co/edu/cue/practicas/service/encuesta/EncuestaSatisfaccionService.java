package co.edu.cue.practicas.service.encuesta;

import co.edu.cue.practicas.dto.request.EnviarEncuestaRequest;
import co.edu.cue.practicas.dto.request.ResponderEncuestaRequest;
import co.edu.cue.practicas.dto.response.EncuestaCoordinadorResumen;
import co.edu.cue.practicas.dto.response.EncuestaResponse;
import co.edu.cue.practicas.event.Sprint4DomainEvent;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.EncuestaSatisfaccion;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoEncuesta;
import co.edu.cue.practicas.model.enums.EstadoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEncuesta;
import co.edu.cue.practicas.model.enums.TipoEvaluacionFinal;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.encuesta.EncuestaSatisfaccionRepository;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.notificacion.NotificacionConfigurableService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncuestaSatisfaccionService {

    private final EncuestaSatisfaccionRepository encuestaRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvaluacionFinalRepository evaluacionRepository;
    private final NotificacionConfigurableService notificacionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public EncuestaResponse enviarATutor(Long instanciaId, EnviarEncuestaRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo Coordinacion envia encuestas al tutor.");
        }
        InstanciaPractica instancia = buscarInstancia(instanciaId);
        validarInstanciaEnFacultadDelCoordinador(instancia, actor);
        validarEvaluacionDocenteCompleta(instanciaId);
        Usuario tutor = usuarioRepository.findById(req.getTutorEmpresarialId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor empresarial no encontrado."));
        if (instancia.getTutorEmpresarial() == null || !instancia.getTutorEmpresarial().getId().equals(tutor.getId())) {
            throw new OperacionNoPermitidaException("La encuesta solo puede asignarse al tutor de esta practica.");
        }
        EncuestaSatisfaccion encuesta = encuestaRepository.findByInstanciaPractica_IdAndTipo(instanciaId, TipoEncuesta.PARA_TUTOR)
                .orElseGet(() -> EncuestaSatisfaccion.builder().instanciaPractica(instancia).tipo(TipoEncuesta.PARA_TUTOR).build());
        encuesta.setTitulo(req.getTitulo());
        encuesta.setPreguntas(req.getPreguntas());
        encuesta.setActorAsignadoId(tutor.getId());
        encuesta.setActorAsignadoCorreo(tutor.getCorreo());
        encuesta.setTokenAcceso(UUID.randomUUID().toString());
        // SPRINT 4 - State: cambia la encuesta de PENDIENTE a enviada, habilitando borrador/completada.
        encuesta.enviar();
        // SPRINT 4 - Decorator: notificacion base enriquecida con enlace directo y recordatorios.
        notificacionService.enviar(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, tutor.getId(), tutor.getCorreo(), tutor.getNombre(),
                variables(instancia, "/api/v1/encuestas-satisfaccion/publica/" + encuesta.getTokenAcceso()));
        // SPRINT 4 - Observer: el envio actualiza checklist/indicadores por evento de dominio.
        eventPublisher.publishEvent(new Sprint4DomainEvent(instanciaId, TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA));
        return EncuestaResponse.desde(encuestaRepository.save(encuesta));
    }

    @Transactional
    public EncuestaResponse enviarAEstudiante(Long instanciaId, EnviarEncuestaRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo Coordinacion envia encuestas al estudiante.");
        }
        InstanciaPractica instancia = buscarInstancia(instanciaId);
        validarInstanciaEnFacultadDelCoordinador(instancia, actor);
        validarEvaluacionDocenteCompleta(instanciaId);
        Usuario estudiante = instancia.getExpediente().getEstudiante();
        EncuestaSatisfaccion encuesta = encuestaRepository.findByInstanciaPractica_IdAndTipo(instanciaId, TipoEncuesta.PARA_ESTUDIANTE)
                .orElseGet(() -> EncuestaSatisfaccion.builder().instanciaPractica(instancia).tipo(TipoEncuesta.PARA_ESTUDIANTE).build());
        encuesta.setTitulo(req.getTitulo());
        encuesta.setPreguntas(req.getPreguntas());
        encuesta.setActorAsignadoId(estudiante.getId());
        encuesta.setActorAsignadoCorreo(estudiante.getCorreo());
        encuesta.setTokenAcceso(UUID.randomUUID().toString());
        // SPRINT 4 - State: la encuesta del estudiante queda enviada y pendiente de respuesta definitiva.
        encuesta.enviar();
        // SPRINT 4 - Decorator: agrega variables dinamicas al correo configurado.
        notificacionService.enviar(TipoEventoNotificacion.ENCUESTA_ESTUDIANTE_ENVIADA, estudiante.getId(), estudiante.getCorreo(), estudiante.getNombre(),
                variables(instancia, "/api/v1/encuestas-satisfaccion/publica/" + encuesta.getTokenAcceso()));
        eventPublisher.publishEvent(new Sprint4DomainEvent(instanciaId, TipoEventoNotificacion.ENCUESTA_ESTUDIANTE_ENVIADA));
        return EncuestaResponse.desde(encuestaRepository.save(encuesta));
    }

    @Transactional
    public EncuestaResponse guardarBorrador(Long encuestaId, ResponderEncuestaRequest req, CustomUserDetails actor) {
        EncuestaSatisfaccion encuesta = buscarEncuesta(encuestaId);
        validarActorRespuesta(encuesta, actor);
        if (encuesta.getTipo() == TipoEncuesta.PARA_ESTUDIANTE) {
            throw new OperacionNoPermitidaException("La encuesta del estudiante solo permite envio definitivo.");
        }
        // SPRINT 4 - State: PENDIENTE -> EN_BORRADOR para encuesta de tutor.
        encuesta.guardarBorrador(req.getRespuestas());
        return EncuestaResponse.desde(encuestaRepository.save(encuesta));
    }

    @Transactional
    public EncuestaResponse completar(Long encuestaId, ResponderEncuestaRequest req, CustomUserDetails actor) {
        EncuestaSatisfaccion encuesta = buscarEncuesta(encuestaId);
        validarActorRespuesta(encuesta, actor);
        // SPRINT 4 - State + Proxy: al completar pasa a COMPLETADA y queda inmutable.
        encuesta.completar(req.getRespuestas());
        encuestaRepository.save(encuesta);
        // SPRINT 4 - Observer: al completarse detiene el flujo de pendientes y refresca checklist.
        eventPublisher.publishEvent(new Sprint4DomainEvent(encuesta.getInstanciaPractica().getId(), TipoEventoNotificacion.ENCUESTA_COMPLETADA));
        return EncuestaResponse.desde(encuesta);
    }

    @Transactional
    public EncuestaResponse completarPorToken(String token, ResponderEncuestaRequest req) {
        EncuestaSatisfaccion encuesta = encuestaRepository.findByTokenAcceso(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encuesta no encontrada o token invalido."));
        encuesta.completar(req.getRespuestas());
        encuestaRepository.save(encuesta);
        eventPublisher.publishEvent(new Sprint4DomainEvent(encuesta.getInstanciaPractica().getId(), TipoEventoNotificacion.ENCUESTA_COMPLETADA));
        return EncuestaResponse.desde(encuesta);
    }

    public EncuestaResponse consultarPorToken(String token) {
        return encuestaRepository.findByTokenAcceso(token)
                .map(EncuestaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encuesta no encontrada o token invalido."));
    }

    @Transactional
    public java.util.List<EncuestaResponse> misEncuestas(CustomUserDetails actor) {
        if (actor.getRol() == Rol.TUTOR_EMPRESARIAL) {
            return encuestaRepository.findByActorAsignadoCorreoIgnoreCaseOrderByFechaEnvioDesc(actor.getUsername())
                    .stream()
                    .map(EncuestaResponse::desde)
                    .toList();
        }
        if (actor.getRol() == Rol.ESTUDIANTE) {
            return encuestaRepository.findByActorAsignadoIdOrderByFechaEnvioDesc(actor.getId())
                    .stream()
                    .map(EncuestaResponse::desde)
                    .toList();
        }
        throw new AccesoNoAutorizadoException("Solo tutor o estudiante consultan sus encuestas asignadas.");
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void enviarRecordatorios() {
        encuestaRepository.findByEstadoNotAndEnviadaTrue(EstadoEncuesta.COMPLETADA).forEach(encuesta -> {
            // Usar el tipo de evento correcto para que el sistema use la plantilla configurada para esa encuesta
            TipoEventoNotificacion tipoEvento = encuesta.getTipo() == TipoEncuesta.PARA_TUTOR
                    ? TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA
                    : TipoEventoNotificacion.ENCUESTA_ESTUDIANTE_ENVIADA;
            // SPRINT 4 - Decorator: recordatorio automatico respetando la frecuenciaRecordatorioDias de la plantilla.
            if (notificacionService.puedeEnviarRecordatorio(encuesta.getActorAsignadoId(), tipoEvento, encuesta.getUltimoRecordatorio())) {
                notificacionService.enviar(tipoEvento, encuesta.getActorAsignadoId(),
                        encuesta.getActorAsignadoCorreo(), encuesta.getActorAsignadoCorreo(),
                        variables(encuesta.getInstanciaPractica(),
                                "/api/v1/encuestas-satisfaccion/publica/" + encuesta.getTokenAcceso()));
                encuesta.setUltimoRecordatorio(LocalDate.now());
            }
        });
    }

    private EncuestaSatisfaccion buscarEncuesta(Long id) {
        return encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encuesta no encontrada."));
    }

    private InstanciaPractica buscarInstancia(Long id) {
        InstanciaPractica instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));
        if (instancia.esInmutable()) {
            throw new OperacionNoPermitidaException("El expediente esta inmutable despues del cierre.");
        }
        return instancia;
    }

    private void validarActorRespuesta(EncuestaSatisfaccion encuesta, CustomUserDetails actor) {
        // SPRINT 4 - Proxy: solo el actor asignado atraviesa el acceso de escritura de la encuesta.
        boolean estudiante = encuesta.getTipo() == TipoEncuesta.PARA_ESTUDIANTE && actor.getId().equals(encuesta.getActorAsignadoId());
        boolean tutor = encuesta.getTipo() == TipoEncuesta.PARA_TUTOR && actor.getUsername().equalsIgnoreCase(encuesta.getActorAsignadoCorreo());
        if (!estudiante && !tutor) {
            throw new AccesoNoAutorizadoException("Solo el actor asignado puede responder esta encuesta.");
        }
    }

    private void validarEvaluacionDocenteCompleta(Long instanciaId) {
        boolean completa = evaluacionRepository.existsByInstanciaPractica_IdAndTipoAndEstado(
                instanciaId, TipoEvaluacionFinal.DOCENTE_ASESOR, EstadoEvaluacionFinal.COMPLETADA);
        if (!completa) {
            throw new OperacionNoPermitidaException(
                    "El docente asesor aun no ha registrado su evaluacion final. Las encuestas solo pueden enviarse luego de esa evaluacion.");
        }
    }

    @Transactional
    public java.util.List<EncuestaCoordinadorResumen> listarParaCoordinador(CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador puede ver este resumen.");
        }
        java.util.List<InstanciaPractica> instancias = instanciaRepository
                .findAllByEstadoAndExpediente_Estudiante_Programa_Facultad_Id(
                        EstadoPractica.EN_CURSO, actor.getFacultadId());

        return instancias.stream().map(this::resumenCoordinador).toList();
    }

    @Transactional
    public Page<EncuestaCoordinadorResumen> listarParaCoordinador(CustomUserDetails actor, Pageable pageable) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo el coordinador puede ver este resumen.");
        }

        return instanciaRepository
                .findAllByEstadoAndExpediente_Estudiante_Programa_Facultad_Id(
                        EstadoPractica.EN_CURSO, actor.getFacultadId(), pageable)
                .map(this::resumenCoordinador);
    }

    private void validarInstanciaEnFacultadDelCoordinador(InstanciaPractica instancia, CustomUserDetails actor) {
        Usuario estudiante = instancia.getExpediente() != null ? instancia.getExpediente().getEstudiante() : null;
        Long facultadEstudiante = estudiante != null
                && estudiante.getPrograma() != null
                && estudiante.getPrograma().getFacultad() != null
                ? estudiante.getPrograma().getFacultad().getId()
                : null;

        if (actor.getFacultadId() == null || !actor.getFacultadId().equals(facultadEstudiante)) {
            throw new AccesoNoAutorizadoException("No tiene acceso a practicas de otra facultad.");
        }
    }

    private EncuestaCoordinadorResumen resumenCoordinador(InstanciaPractica i) {
        boolean evalDocente = evaluacionRepository.existsByInstanciaPractica_IdAndTipoAndEstado(
                i.getId(), TipoEvaluacionFinal.DOCENTE_ASESOR, EstadoEvaluacionFinal.COMPLETADA);
        boolean tutorEnviada = encuestaRepository.findByInstanciaPractica_IdAndTipo(
                i.getId(), TipoEncuesta.PARA_TUTOR).isPresent();
        boolean tutorCompletada = encuestaRepository.existsByInstanciaPractica_IdAndTipoAndEstado(
                i.getId(), TipoEncuesta.PARA_TUTOR, EstadoEncuesta.COMPLETADA);
        boolean estudianteEnviada = encuestaRepository.findByInstanciaPractica_IdAndTipo(
                i.getId(), TipoEncuesta.PARA_ESTUDIANTE).isPresent();
        boolean estudianteCompletada = encuestaRepository.existsByInstanciaPractica_IdAndTipoAndEstado(
                i.getId(), TipoEncuesta.PARA_ESTUDIANTE, EstadoEncuesta.COMPLETADA);
        return EncuestaCoordinadorResumen.builder()
                .instanciaId(i.getId())
                .nombrePractica(i.getNombre())
                .nombreEstudiante(i.getExpediente().getEstudiante().getNombre())
                .programaNombre(i.getExpediente().getEstudiante().getPrograma() != null
                        ? i.getExpediente().getEstudiante().getPrograma().getNombre() : null)
                .nombreEmpresa(i.getEmpresa() != null ? i.getEmpresa().getRazonSocial() : null)
                .nombreDocenteAsesor(i.getDocenteAsesor() != null ? i.getDocenteAsesor().getNombre() : null)
                .tutorEmpresarialId(i.getTutorEmpresarial() != null ? i.getTutorEmpresarial().getId() : null)
                .nombreTutor(i.getTutorEmpresarial() != null ? i.getTutorEmpresarial().getNombre() : null)
                .evaluacionDocenteCompleta(evalDocente)
                .encuestaTutorEnviada(tutorEnviada)
                .encuestaTutorCompletada(tutorCompletada)
                .encuestaEstudianteEnviada(estudianteEnviada)
                .encuestaEstudianteCompletada(estudianteCompletada)
                .build();
    }

    private Map<String, String> variables(InstanciaPractica instancia, String enlace) {
        return Map.of(
                "nombre_estudiante", instancia.getExpediente().getEstudiante().getNombre(),
                "empresa", instancia.getEmpresa() != null ? instancia.getEmpresa().getRazonSocial() : "",
                "nombre_practica", instancia.getNombre(),
                "enlace_encuesta", enlace,
                "resultado", "",
                "nota_final", ""
        );
    }
}
