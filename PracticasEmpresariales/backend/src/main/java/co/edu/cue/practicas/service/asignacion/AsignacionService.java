package co.edu.cue.practicas.service.asignacion;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearAsignacionRequest;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.entity.Vacante;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoAccion;
import co.edu.cue.practicas.repository.catalogo.CatalogoPracticaRepository;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.repository.vacante.VacanteRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import co.edu.cue.practicas.service.notificacion.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    private final UsuarioRepository usuarioRepository;
    private final VacanteRepository vacanteRepository;
    private final ExpedienteEstudianteRepository expedienteRepository;
    private final CatalogoPracticaRepository catalogoRepository;
    private final InstanciaPracticaRepository instanciaRepository;
    private final EstudianteMapper estudianteMapper;
    private final AuditoriaLogger auditoriaLogger;
    private final EmailService emailService;

    /**
     * Asigna un estudiante a una vacante creando la InstanciaPractica correspondiente.
     * Reglas:
     *  - Solo rol COORDINADOR_PRACTICAS puede asignar
     *  - Estudiante debe ser APTO y enviadoAlProceso = true
     *  - Vacante debe poder aceptar practicante (cupos)
     */
    @Transactional
    public InstanciaPracticaResponse asignar(CrearAsignacionRequest req, CustomUserDetails actor) {
        if (actor == null || actor.getRol() != Rol.COORDINADOR_PRACTICAS)
            throw new AccesoNoAutorizadoException("Solo el Coordinador de Prácticas puede asignar estudiantes.");

        Usuario estudiante = usuarioRepository.findById(req.getEstudianteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado."));

        if (!estudiante.isEnviadoAlProceso() || estudiante.getEstadoEstudiante() == null)
            throw new IllegalArgumentException("El estudiante no ha sido enviado al proceso o no tiene estado definido.");

        if (!estudiante.getEstadoEstudiante().name().equals("APTO"))
            throw new IllegalArgumentException("Solo estudiantes en estado APTO pueden asignarse.");

        Vacante vacante = vacanteRepository.findById(req.getVacanteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vacante no encontrada."));

        if (!vacante.puedeAceptarPracticante())
            throw new IllegalArgumentException("La vacante no acepta más practicantes.");

        ExpedienteEstudiante expediente = expedienteRepository.findByEstudiante_Id(estudiante.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente del estudiante no encontrado."));

        CatalogoPractica catalogo = determinarCatalogo(req, estudiante, expediente);

        if (catalogo == null)
            throw new RecursoNoEncontradoException("No existe catálogo de práctica activo para el programa del estudiante.");

        // Validar que el estudiante no tenga otra práctica activa (OCL: maxUnaPracticaActiva)
        long practicasActivas = instanciaRepository.countByExpediente_Estudiante_IdAndEstado(
                estudiante.getId(), EstadoPractica.EN_CURSO);
        if (practicasActivas > 0)
            throw new OperacionNoPermitidaException("El estudiante ya tiene una práctica EN_CURSO activa.");

        // Resolver tutor empresarial si se especificó (debe ser usuario con rol TUTOR_EMPRESARIAL)
        Usuario tutor = null;
        if (req.getTutorEmpresarialId() != null) {
            tutor = usuarioRepository.findById(req.getTutorEmpresarialId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tutor empresarial no encontrado."));
            if (tutor.getRol() != Rol.TUTOR_EMPRESARIAL)
                throw new OperacionNoPermitidaException("El usuario indicado no tiene rol TUTOR_EMPRESARIAL.");
        }

        // Resolver docente asesor si se especificó
        Usuario docente = null;
        if (req.getDocenteAsesorId() != null) {
            docente = usuarioRepository.findById(req.getDocenteAsesorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Docente asesor no encontrado."));
            if (docente.getRol() != Rol.DOCENTE_ASESOR)
                throw new OperacionNoPermitidaException("El usuario indicado no tiene rol DOCENTE_ASESOR.");
        }

        // Clonar catálogo a InstanciaPractica (snapshot)
        InstanciaPractica instancia = InstanciaPractica.builder()
                .catalogoPracticaId(catalogo.getId())
                .numeroPractica(catalogo.getNumeroPractica())
                .nombre(catalogo.getNombre())
                .materiaNucleo(catalogo.getMateriaNucleo())
                .codigoMateria(catalogo.getCodigoMateria())
                .numCortes(catalogo.getNumCortes())
                .duracionSemanas(catalogo.getDuracionSemanas())
                .documentosRequeridos(catalogo.getDocumentosRequeridos())
                .empresa(vacante.getEmpresa())
                .vacanteId(vacante.getId())
                .tutorEmpresarial(tutor)
                .docenteAsesor(docente)
                .build();

        // Ocupar cupo en la vacante (lanza excepción si no hay cupos)
        vacante.ocuparCupo();
        vacanteRepository.save(vacante);

        // Persistir instancia dentro del expediente
        expediente.agregarPractica(instancia);
        instanciaRepository.save(instancia);

        // Registrar en la auditoría
        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .modulo("AsignacionService")
                .tipoAccion(TipoAccion.CREAR)
                .registroAfectadoId(instancia.getId())
                .registroAfectadoTipo(InstanciaPractica.class.getSimpleName())
                .valoresNuevos("{\"estudianteId\":" + estudiante.getId() + ",\"vacanteId\":" + vacante.getId() + "}")
                .build());

        notificarAsignacionCreada(instancia);

        return estudianteMapper.toInstanciaPracticaResponse(instancia);
    }

    @Transactional
    public void cancelarAsignacion(Long instanciaId, String motivo, CustomUserDetails actor) {
        if (actor == null || actor.getRol() != Rol.COORDINADOR_PRACTICAS)
            throw new AccesoNoAutorizadoException("Solo el Coordinador de Prácticas puede cancelar asignaciones.");

        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia de práctica no encontrada."));

        // No se puede cancelar una práctica EN_CURSO o FINALIZADA
        if (instancia.getEstado() == EstadoPractica.EN_CURSO
                || instancia.getEstado() == EstadoPractica.FINALIZADA)
            throw new OperacionNoPermitidaException("No se puede cancelar una práctica que ya está en curso o finalizada.");

        // Cambiamos el estado a CANCELADA
        instancia.cancelar();
        instanciaRepository.save(instancia);

        // Liberar cupo en la vacante origen si existe
        if (instancia.getVacanteId() != null) {
            vacanteRepository.findById(instancia.getVacanteId()).ifPresent(v -> {
                v.liberarCupo();
                vacanteRepository.save(v);
            });
        }

        // Auditoría
        auditoriaLogger.registrar(BitacoraAuditoria.builder()
                .usuario(actor.getUsuario())
                .nombreUsuario(actor.getNombre())
                .rolUsuario(actor.getRol())
                .modulo("AsignacionService")
                .tipoAccion(TipoAccion.CAMBIO_ESTADO)
                .registroAfectadoId(instancia.getId())
                .registroAfectadoTipo(InstanciaPractica.class.getSimpleName())
                .valoresNuevos("{\"estado\":\"CANCELADA\",\"motivo\":\"" + (motivo!=null?motivo:"") + "\"}"));

        notificarAsignacionCancelada(instancia, motivo);
    }

    private void notificarAsignacionCreada(InstanciaPractica instancia) {
        if (instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null) {
            var estudiante = instancia.getExpediente().getEstudiante();
            if (estudiante.getCorreo() != null) {
                String html = "<p>Estimado/a " + estudiante.getNombre() + ",</p>"
                        + "<p>Has sido asignado a la vacante en la empresa <strong>"
                        + (instancia.getEmpresa() != null ? instancia.getEmpresa().getRazonSocial() : "(empresa)")
                        + "</strong>.</p>";
                emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, "Asignación a vacante");
            }
        }

        if (instancia.getEmpresa() != null && instancia.getEmpresa().getCorreo() != null) {
            String nombreEstudiante = instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null
                    ? instancia.getExpediente().getEstudiante().getNombre()
                    : "(estudiante)";
            String htmlEmpresa = "<p>Se ha asignado al estudiante <strong>" + nombreEstudiante
                    + "</strong> a la vacante publicada en su empresa.</p>";
            emailService.notificarAsignacion(instancia.getEmpresa().getCorreo(), instancia.getEmpresa().getRazonSocial(), htmlEmpresa, "Nuevo practicante asignado");
        }
    }

    private void notificarAsignacionCancelada(InstanciaPractica instancia, String motivo) {
        if (instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null) {
            var estudiante = instancia.getExpediente().getEstudiante();
            if (estudiante.getCorreo() != null) {
                String html = "<p>Estimado/a " + estudiante.getNombre() + ",</p>"
                        + "<p>Tu asignación ha sido cancelada"
                        + (motivo != null && !motivo.isBlank() ? ": " + motivo : ".")
                        + "</p>";
                emailService.notificarAsignacion(estudiante.getCorreo(), estudiante.getNombre(), html, "Asignación cancelada");
            }
        }

        if (instancia.getEmpresa() != null && instancia.getEmpresa().getCorreo() != null) {
            String nombreEstudiante = instancia.getExpediente() != null && instancia.getExpediente().getEstudiante() != null
                    ? instancia.getExpediente().getEstudiante().getNombre()
                    : "(estudiante)";
            String htmlEmpresa = "<p>Se ha cancelado la asignación del estudiante <strong>" + nombreEstudiante
                    + "</strong>.</p>";
            emailService.notificarAsignacion(instancia.getEmpresa().getCorreo(), instancia.getEmpresa().getRazonSocial(), htmlEmpresa, "Asignación cancelada");
        }
    }

    /** GPE-158 — Lista asignaciones activas con filtro opcional por estado */
    @Transactional
    public List<InstanciaPracticaResponse> listarAsignaciones(String estadoStr, CustomUserDetails actor) {
        if (actor == null || actor.getRol() != Rol.COORDINADOR_PRACTICAS)
            throw new AccesoNoAutorizadoException("Solo el Coordinador de Prácticas puede ver las asignaciones.");

        List<InstanciaPractica> instancias;
        if (estadoStr != null && !estadoStr.isBlank()) {
            try {
                EstadoPractica estado = EstadoPractica.valueOf(estadoStr.toUpperCase());
                instancias = instanciaRepository.findAllByEstado(estado);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado no válido: " + estadoStr);
            }
        } else {
            instancias = instanciaRepository.findAll();
        }
        return instancias.stream().map(estudianteMapper::toInstanciaPracticaResponse).toList();
    }

    /** GPE-159 — Detalle de una asignación */
    @Transactional
    public InstanciaPracticaResponse obtenerAsignacion(Long id, CustomUserDetails actor) {
        if (actor == null || actor.getRol() != Rol.COORDINADOR_PRACTICAS)
            throw new AccesoNoAutorizadoException("Solo el Coordinador de Prácticas puede ver el detalle de asignaciones.");
        InstanciaPractica instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación no encontrada."));
        return estudianteMapper.toInstanciaPracticaResponse(instancia);
    }

    private CatalogoPractica determinarCatalogo(CrearAsignacionRequest req, Usuario estudiante, ExpedienteEstudiante expediente) {
        if (req.getCatalogoPracticaId() != null) {
            return catalogoRepository.findById(req.getCatalogoPracticaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Catálogo de práctica no encontrado."));
        }

        int siguienteNumero = expediente.getPracticas().size() + 1;
        var opt = catalogoRepository.findByPrograma_IdAndNumeroPracticaAndActivoTrue(
                estudiante.getPrograma() != null ? estudiante.getPrograma().getId() : -1L, siguienteNumero);
        if (opt.isPresent()) return opt.get();

        var lista = catalogoRepository.findByPrograma_IdAndActivoTrue(
                estudiante.getPrograma() != null ? estudiante.getPrograma().getId() : -1L);
        if (!lista.isEmpty()) return lista.get(0);

        return null;
    }
}

