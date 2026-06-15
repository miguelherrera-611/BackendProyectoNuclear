package co.edu.cue.practicas.model.converter;

import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EstadoEmpresaConverter implements AttributeConverter<EstadoEmpresa, String> {

    @Override
    public String convertToDatabaseColumn(EstadoEmpresa estado) {
        return estado == null ? null : estado.name();
    }

    @Override
    public EstadoEmpresa convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "ACTIVA"   -> EstadoEmpresa.ACTIVA;
            case "APROBADA" -> EstadoEmpresa.ACTIVA;
            default         -> EstadoEmpresa.INACTIVA;
        };
    }
}
