package co.edu.cue.practicas.service.cierre;

import co.edu.cue.practicas.dto.request.RegistrarResultadoSustentacionRequest;
import co.edu.cue.practicas.dto.request.RegistrarSustentacionRequest;
import co.edu.cue.practicas.dto.response.SustentacionResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.SustentacionPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.cierre.SustentacionPracticaRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SustentacionService {

    private final SustentacionPracticaRepository sustentacionRepository;
    private final InstanciaPracticaRepository instanciaRepository;

    @Transactional
    public SustentacionResponse programar(Long instanciaId, RegistrarSustentacionRequest req, CustomUserDetails actor) {
        validarCoordinador(actor);
        InstanciaPractica instancia = buscarInstancia(instanciaId);
        validarScope(instancia, actor);
        if (instancia.getFechaInicio() != null && req.getFecha().isBefore(instancia.getFechaInicio())) {
            throw new OperacionNoPermitidaException("La fecha de sustentacion debe ser posterior al inicio de practica.");
        }
        SustentacionPractica sustentacion = sustentacionRepository.findByInstanciaPractica_Id(instanciaId)
                .orElseGet(() -> SustentacionPractica.builder().instanciaPractica(instancia).coordinador(actor.getUsuario()).build());
        sustentacion.setFecha(req.getFecha());
        sustentacion.setJurados(req.getJurados());
        return SustentacionResponse.desde(sustentacionRepository.save(sustentacion));
    }

    @Transactional
    public SustentacionResponse registrarResultado(Long instanciaId, RegistrarResultadoSustentacionRequest req, CustomUserDetails actor) {
        validarCoordinador(actor);
        SustentacionPractica sustentacion = sustentacionRepository.findByInstanciaPractica_Id(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sustentacion no programada."));
        validarScope(sustentacion.getInstanciaPractica(), actor);
        sustentacion.registrarResultado(req.getResultado(), req.getActaUrl(), req.isActaFirmada());
        return SustentacionResponse.desde(sustentacionRepository.save(sustentacion));
    }

    private InstanciaPractica buscarInstancia(Long instanciaId) {
        InstanciaPractica instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Practica no encontrada."));
        if (instancia.esInmutable()) {
            throw new OperacionNoPermitidaException("El expediente esta inmutable despues del cierre.");
        }
        return instancia;
    }

    private void validarCoordinador(CustomUserDetails actor) {
        if (actor.getRol() != Rol.COORDINADOR_PRACTICAS) {
            throw new AccesoNoAutorizadoException("Solo Coordinacion del programa gestiona sustentaciones.");
        }
    }

    private void validarScope(InstanciaPractica instancia, CustomUserDetails actor) {
        Long programaId = instancia.getExpediente().getEstudiante().getPrograma().getId();
        if (actor.getProgramaId() != null && !actor.getProgramaId().equals(programaId)) {
            throw new AccesoNoAutorizadoException("No puedes gestionar sustentaciones de otro programa.");
        }
    }
}
