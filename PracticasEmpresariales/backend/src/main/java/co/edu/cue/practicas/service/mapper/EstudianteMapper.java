package co.edu.cue.practicas.service.mapper;

import co.edu.cue.practicas.dto.response.CatalogoPracticaResponse;
import co.edu.cue.practicas.dto.response.HojaDeVidaResponse;
import co.edu.cue.practicas.dto.response.InstanciaPracticaResponse;
import co.edu.cue.practicas.dto.response.UsuarioResponse;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.HojaDeVida;
import co.edu.cue.practicas.model.entity.InstanciaPractica;
import co.edu.cue.practicas.model.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * SOLID — SRP: convierte entidades del módulo de estudiantes a DTOs de respuesta.
 *
 * Separado de Dev3Mapper para que el módulo de estudiantes tenga su propio mapper:
 *   Dev3Mapper cambia si cambian las entidades Empresa/Vacante/Tutor.
 *   EstudianteMapper cambia si cambian CatalogoPractica/InstanciaPractica/HojaDeVida.
 */
@Component
public class EstudianteMapper {

    public CatalogoPracticaResponse toCatalogoPracticaResponse(CatalogoPractica c) {
        return CatalogoPracticaResponse.builder()
                .id(c.getId())
                .programaId(c.getPrograma().getId())
                .programaNombre(c.getPrograma().getNombre())
                .numeroPractica(c.getNumeroPractica())
                .nombre(c.getNombre())
                .materiaNucleo(c.getMateriaNucleo())
                .codigoMateria(c.getCodigoMateria())
                .numCortes(c.getNumCortes())
                .duracionSemanas(c.getDuracionSemanas())
                .documentosRequeridos(c.getDocumentosRequeridos())
                .activo(c.isActivo())
                .creadoEn(c.getCreadoEn())
                .build();
    }

    public InstanciaPracticaResponse toInstanciaPracticaResponse(InstanciaPractica i) {
        return InstanciaPracticaResponse.builder()
                .id(i.getId())
                .numeroPractica(i.getNumeroPractica())
                .nombre(i.getNombre())
                .materiaNucleo(i.getMateriaNucleo())
                .codigoMateria(i.getCodigoMateria())
                .numCortes(i.getNumCortes())
                .duracionSemanas(i.getDuracionSemanas())
                .documentosRequeridos(i.getDocumentosRequeridos())
                .estado(i.getEstado())
                .vacanteId(i.getVacanteId())
                .estudianteId(i.getExpediente() != null && i.getExpediente().getEstudiante() != null
                        ? i.getExpediente().getEstudiante().getId() : null)
                .nombreEstudiante(i.getExpediente() != null && i.getExpediente().getEstudiante() != null
                        ? i.getExpediente().getEstudiante().getNombre() : null)
                .empresaId(i.getEmpresa() != null ? i.getEmpresa().getId() : null)
                .razonSocialEmpresa(i.getEmpresa() != null ? i.getEmpresa().getRazonSocial() : null)
                .docenteAsesorId(i.getDocenteAsesor() != null ? i.getDocenteAsesor().getId() : null)
                .nombreDocenteAsesor(i.getDocenteAsesor() != null ? i.getDocenteAsesor().getNombre() : null)
                .tutorEmpresarialId(i.getTutorEmpresarial() != null ? i.getTutorEmpresarial().getId() : null)
                .nombreTutorEmpresarial(i.getTutorEmpresarial() != null ? i.getTutorEmpresarial().getNombre() : null)
                .fechaInicio(i.getFechaInicio())
                .fechaFin(i.getFechaFin())
                .firmaTutor(i.isFirmaTutor())
                .firmaDocente(i.isFirmaDocente())
                .firmaEstudiante(i.isFirmaEstudiante())
                .vinculacionConfirmadaEn(i.getVinculacionConfirmadaEn())
                .creadoEn(i.getCreadoEn())
                .actualizadoEn(i.getActualizadoEn())
                .build();
    }

    public HojaDeVidaResponse toHojaDeVidaResponse(HojaDeVida hv) {
        return HojaDeVidaResponse.builder()
                .id(hv.getId())
                .estudianteId(hv.getEstudiante().getId())
                .version(hv.getVersion())
                .fechaCarga(hv.getFechaCarga())
                .urlArchivo(hv.getUrlArchivo())
                .estado(hv.getEstado())
                .validadoPor(hv.getValidadoPor())
                .fechaValidacion(hv.getFechaValidacion())
                .motivoRechazo(hv.getMotivoRechazo())
                .creadoEn(hv.getCreadoEn())
                .build();
    }

    public UsuarioResponse toEstudianteResponse(Usuario u) {
        return UsuarioResponse.desde(u);
    }
}
