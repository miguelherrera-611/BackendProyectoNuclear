package co.edu.cue.practicas.pattern.builder;

import co.edu.cue.practicas.dto.response.ExpedienteResponse;
import co.edu.cue.practicas.dto.response.HojaDeVidaResponse;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PATRÓN BUILDER — ExpedienteBuilder
 *
 * Construye el DTO de respuesta del expediente completo, agregando
 * paso a paso los datos del estudiante, la HV actual, el historial
 * de HVs y el listado de prácticas.
 *
 * La separación entre el Builder (armado del DTO) y el Proxy (control de acceso)
 * cumple SOLID SRP: el Builder solo ensambla, el Proxy solo protege.
 *
 * SOLID — SRP: solo ensambla el DTO de respuesta.
 * SOLID — OCP: agregar un nuevo campo al DTO → solo un nuevo método conX().
 */
@Component
@RequiredArgsConstructor
public class ExpedienteBuilder {

    private final EstudianteMapper mapper;

    public ExpedienteResponse construir(ExpedienteEstudiante expediente) {
        List<InstanciaPracticaResponse> practicas = expediente.getPracticas().stream()
                .map(mapper::toInstanciaPracticaResponse)
                .toList();

        List<HojaDeVidaResponse> historialHv = expediente.getHistorialHv().stream()
                .map(mapper::toHojaDeVidaResponse)
                .toList();

        return ExpedienteResponse.builder()
                .expedienteId(expediente.getId())
                .estudianteId(expediente.getEstudiante().getId())
                .nombreEstudiante(expediente.getEstudiante().getNombre())
                .identificacion(expediente.getEstudiante().getIdentificacion())
                .programa(expediente.getEstudiante().getPrograma() != null
                        ? expediente.getEstudiante().getPrograma().getNombre() : null)
                .semestre(expediente.getEstudiante().getSemestre())
                .estadoEstudiante(expediente.getEstudiante().getEstadoEstudiante())
                .hvActual(historialHv.isEmpty() ? null : historialHv.get(0))
                .historialHv(historialHv)
                .practicas(practicas)
                .creadoEn(expediente.getCreadoEn())
                .build();
    }
}
