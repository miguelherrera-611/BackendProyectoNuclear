package co.edu.cue.practicas.service.configuracion;

import co.edu.cue.practicas.dto.request.ConfigurarProgramaRequest;
import co.edu.cue.practicas.dto.response.ProgramaConfiguracionResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.ProgramaConfiguracion;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.programa.ProgramaConfiguracionRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProgramaConfiguracionService {

    // SPRINT 4 - Singleton: Spring mantiene una unica instancia del servicio de configuracion.
    private static final double NOTA_MINIMA_INSTITUCIONAL = 3.0;
    private final ProgramaConfiguracionRepository configuracionRepository;
    private final ProgramaRepository programaRepository;

    @Transactional
    public ProgramaConfiguracionResponse configurar(Long programaId, ConfigurarProgramaRequest req, CustomUserDetails actor) {
        if (actor.getRol() != Rol.ADMIN_DTI) {
            throw new AccesoNoAutorizadoException("Solo DTI puede configurar parametros por programa.");
        }
        Programa programa = programaRepository.findById(programaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado."));
        configuracionRepository.findByPrograma_IdAndVigenteTrue(programaId)
                .forEach(c -> c.setVigente(false));

        // SPRINT 4 - Builder: crea la configuracion completa del programa de forma explicita e inmutable por version.
        ProgramaConfiguracion nueva = ProgramaConfiguracion.builder()
                .programa(programa)
                .numeroPracticas(req.getNumeroPracticas())
                .semanasSeguimiento(req.getSemanasSeguimiento())
                .notaMinimaAprobacion(req.getNotaMinimaAprobacion())
                .requisitosCierre(req.getRequisitosCierre())
                .umbralInactividadDias(req.getUmbralInactividadDias())
                .vigente(true)
                .build();
        return ProgramaConfiguracionResponse.desde(configuracionRepository.save(nueva));
    }

    public ProgramaConfiguracionResponse obtener(Long programaId) {
        return configuracionRepository.findTopByPrograma_IdAndVigenteTrueOrderByCreadoEnDesc(programaId)
                .map(ProgramaConfiguracionResponse::desde)
                .orElseGet(() -> ProgramaConfiguracionResponse.builder()
                        // SPRINT 4 - Builder: valores institucionales por defecto si el programa aun no fue configurado.
                        .programaId(programaId)
                        .numeroPracticas(1)
                        .semanasSeguimiento(12)
                        .notaMinimaAprobacion(NOTA_MINIMA_INSTITUCIONAL)
                        .requisitosCierre("evaluacion_docente,evaluacion_tutor,nota_final,encuesta_tutor,encuesta_estudiante,documentos,sustentacion")
                        .umbralInactividadDias(7)
                        .vigente(true)
                        .build());
    }

    public double notaMinima(Long programaId) {
        // SPRINT 4 - Strategy: otros servicios consultan aqui la politica de nota minima por programa.
        return obtener(programaId).getNotaMinimaAprobacion();
    }
}
