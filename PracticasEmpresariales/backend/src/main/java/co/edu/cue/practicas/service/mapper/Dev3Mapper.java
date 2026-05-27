package co.edu.cue.practicas.service.mapper;

import co.edu.cue.practicas.dto.response.EmpresaResponse;
import co.edu.cue.practicas.dto.response.TutorEmpresarialResponse;
import co.edu.cue.practicas.dto.response.VacanteResponse;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.TutorEmpresarial;
import co.edu.cue.practicas.model.entity.Vacante;
import org.springframework.stereotype.Component;

/**
 * SOLID — SRP: responsabilidad única → convertir entidades a DTOs de respuesta.
 *
 * Separado de los servicios para que:
 *   - EmpresaService cambie solo si cambia el flujo de negocio.
 *   - EmpresaMapper cambie solo si cambia la estructura del DTO.
 *
 * SOLID — OCP: si se agrega un campo al DTO, solo se modifica aquí.
 */
@Component
public class Dev3Mapper {

    public EmpresaResponse toEmpresaResponse(Empresa e) {
        return new EmpresaResponse(
                e.getId(),
                e.getRazonSocial(),
                e.getNit(),
                e.getSector(),
                e.getDireccion(),
                e.getMunicipio(),
                e.getTelefono(),
                e.getNombreContacto(),
                e.getCorreo(),
                e.getEstado(),
                e.getAreasDisponibles(),
                e.getCreadoEn()
        );
    }

    public TutorEmpresarialResponse toTutorResponse(TutorEmpresarial t) {
        return new TutorEmpresarialResponse(
                t.getId(),
                t.getNombre(),
                t.getCargo(),
                t.getCorreo(),
                t.getTelefono(),
                t.getEmpresa().getId(),
                t.getEmpresa().getRazonSocial(),
                t.isDisponible(),
                t.isActivo(),
                t.getCreadoEn()
        );
    }

    public VacanteResponse toVacanteResponse(Vacante v) {
        return new VacanteResponse(
                v.getId(),
                v.getEmpresa().getId(),
                v.getEmpresa().getRazonSocial(),
                v.getArea(),
                v.getCuposTotales(),
                v.getCuposOcupados(),
                v.getEstado(),
                v.getFechaPublicacion(),
                v.getMotivoRechazo(),
                v.getCreadoEn()
        );
    }
}
