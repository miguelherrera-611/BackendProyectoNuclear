package co.edu.cue.practicas.service.estudiante;

import co.edu.cue.practicas.dto.request.EnviarAlProcesoRequest;
import co.edu.cue.practicas.dto.request.MarcarAptoRequest;
import co.edu.cue.practicas.dto.request.MantenerNoAptoRequest;
import co.edu.cue.practicas.dto.response.UsuarioResponse;
import co.edu.cue.practicas.event.AptitudCambiadaEvent;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.EstadoHojaDeVida;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.prototype.CatalogoPracticaPlantilla;
import co.edu.cue.practicas.pattern.strategy.EstrategiaValidacion;
import co.edu.cue.practicas.pattern.template.PlantillaValidacionAptitud;
import co.edu.cue.practicas.pattern.chain.ContextoValidacion;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import co.edu.cue.practicas.repository.expediente.HojaDeVidaRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.security.annotation.RequiereRol;
import co.edu.cue.practicas.security.annotation.SoloLectura;
import co.edu.cue.practicas.service.catalogo.CatalogoPracticaService;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import co.edu.cue.practicas.service.validator.EstudianteValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class EstudianteService extends PlantillaValidacionAptitud {

    private final UsuarioRepository usuarioRepository;
    private final ExpedienteEstudianteRepository expedienteRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final HojaDeVidaRepository hvRepository;
    private final CatalogoPracticaService catalogoService;
    private final EstudianteValidator validator;
    private final EstudianteMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public EstudianteService(EstrategiaValidacion estrategia,
                             UsuarioRepository usuarioRepository,
                             ExpedienteEstudianteRepository expedienteRepository,
                             InstanciaPracticaRepository instanciaRepository,
                             HojaDeVidaRepository hvRepository,
                             CatalogoPracticaService catalogoService,
                             EstudianteValidator validator,
                             EstudianteMapper mapper,
                             ApplicationEventPublisher eventPublisher) {
        super(estrategia);
        this.usuarioRepository = usuarioRepository;
        this.expedienteRepository = expedienteRepository;
        this.instanciaRepository = instanciaRepository;
        this.hvRepository = hvRepository;
        this.catalogoService = catalogoService;
        this.validator = validator;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    public UsuarioResponse marcarApto(Long estudianteId, MarcarAptoRequest req,
                                      CustomUserDetails ejecutor) {
        Usuario estudiante = buscarEstudianteOFallar(estudianteId);
        validator.validarEsEstudianteActivo(estudiante);
        validator.validarTransicionApto(estudiante);

        CatalogoPractica catalogo = catalogoService.buscarOFallar(req.catalogoPracticaId());

        Optional<HojaDeVida> hvActual = hvRepository
                .findTopByEstudiante_IdAndEstadoOrderByVersionDesc(
                        estudianteId, EstadoHojaDeVida.VALIDA);

        Optional<InstanciaPractica> practicaAnterior = catalogo.getNumeroPractica() > 1
                ? instanciaRepository.findPorEstudianteYNumero(
                estudianteId, catalogo.getNumeroPractica() - 1)
                : Optional.empty();

        super.ejecutar(estudiante, catalogo, hvActual, practicaAnterior);

        estudiante.setEstadoEstudiante(EstadoEstudiante.APTO);
        estudiante.setMotivoNoApto(null);
        usuarioRepository.save(estudiante);

        eventPublisher.publishEvent(
                new AptitudCambiadaEvent(this, estudiante, EstadoEstudiante.APTO, false));

        log.info("[GPE-145] Estudiante '{}' marcado como APTO por {}",
                estudiante.getNombre(), ejecutor.getNombre());
        return UsuarioResponse.desde(estudiante);
    }

    @Override
    protected void onAptoConfirmado(ContextoValidacion ctx) {
        ExpedienteEstudiante expediente = expedienteRepository
                .findByEstudiante_Id(ctx.estudiante().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Expediente no encontrado para el estudiante: "
                                + ctx.estudiante().getId()));

        InstanciaPractica instancia = new CatalogoPracticaPlantilla(
                ctx.catalogo(), expediente).clonar();

        instanciaRepository.save(instancia);
        expediente.agregarPractica(instancia);
        expedienteRepository.save(expediente);

        log.info("[Prototype][GPE-145] InstanciaPractica clonada desde catalogo '{}' -> expediente {}",
                ctx.catalogo().getNombre(), expediente.getId());
    }

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    public UsuarioResponse mantenerNoApto(Long estudianteId, MantenerNoAptoRequest req,
                                          CustomUserDetails ejecutor) {
        Usuario estudiante = buscarEstudianteOFallar(estudianteId);
        validator.validarEsEstudianteActivo(estudiante);
        estudiante.setMotivoNoApto(req.motivo());
        usuarioRepository.save(estudiante);

        log.info("[GPE-145] Estudiante '{}' mantenido como NO_APTO por {}: {}",
                estudiante.getNombre(), ejecutor.getNombre(), req.motivo());
        return UsuarioResponse.desde(estudiante);
    }

    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA})
    public List<UsuarioResponse> enviarAlProceso(EnviarAlProcesoRequest req,
                                                 CustomUserDetails ejecutor) {
        // Reemplazamos Stream.peek con un loop explícito para evitar el issue de Sonar
        List<Usuario> estudiantes = new ArrayList<>();
        for (Long id : req.estudianteIds()) {
            Usuario e = buscarEstudianteOFallar(id);
            validator.validarEsEstudianteActivo(e);
            validator.validarEsApto(e);
            e.setEnviadoAlProceso(true);
            eventPublisher.publishEvent(
                    new AptitudCambiadaEvent(this, e, EstadoEstudiante.APTO, true));
            estudiantes.add(e);
        }

        usuarioRepository.saveAll(estudiantes);
        log.info("[GPE-147] {} estudiante(s) enviados al proceso por {}",
                estudiantes.size(), ejecutor.getNombre());
        return estudiantes.stream().map(UsuarioResponse::desde).toList();
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS,
            Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarEstudiantes(CustomUserDetails usuario,
                                                   EstadoEstudiante estadoFiltro,
                                                   Pageable pageable) {
        Rol rol = usuario.getRol();
        Page<Usuario> resultados = switch (rol) {
            case COORDINACION_ACADEMICA -> {
                Long facultadId = usuario.getFacultadId();
                if (estadoFiltro != null) {
                    yield usuarioRepository.findEstudiantesPorEstadoYFacultad(
                            Rol.ESTUDIANTE, estadoFiltro, facultadId, pageable);
                }
                yield usuarioRepository.findEstudiantesPorFacultad(
                        Rol.ESTUDIANTE, facultadId, pageable);
            }
            case COORDINADOR_PRACTICAS -> {
                Long programaId = usuario.getProgramaId();
                yield usuarioRepository
                        .findByRolAndEstadoEstudianteAndEnviadoAlProcesoTrueAndPrograma_IdAndActivoTrue(
                                Rol.ESTUDIANTE, EstadoEstudiante.APTO, programaId, pageable);
            }
            default -> usuarioRepository.findByRol(Rol.ESTUDIANTE, pageable);
        };

        return resultados.map(UsuarioResponse::desde);
    }

    @SoloLectura
    @RequiereRol(roles = {Rol.COORDINACION_ACADEMICA, Rol.COORDINADOR_PRACTICAS,
            Rol.ADMIN_DTI, Rol.DIRECCION})
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.desde(buscarEstudianteOFallar(id));
    }

    private Usuario buscarEstudianteOFallar(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> Rol.ESTUDIANTE.equals(u.getRol()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + id));
    }
}