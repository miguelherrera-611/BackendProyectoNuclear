package co.edu.cue.practicas.pattern.prototype;

import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * PATRÓN PROTOTYPE — EmpresaPlantilla (ConcretePrototype)
 *
 * Del diagrama del documento:
 *   IPrototype → PracticeTemplate
 *                   ↓ clona para crear instancia
 *               PracticeInstance
 *
 * En el contexto del Dev 3:
 * Cuando el Coordinador registra una empresa con estructura similar
 * a una existente, clona la plantilla y solo ajusta NIT y razón social.
 *
 * SOLID — SRP: solo clona la configuración de una empresa.
 * SOLID — OCP: nuevos campos clonables → solo actualizar clonar().
 */
@RequiredArgsConstructor
public class EmpresaPlantilla implements IPrototype<Empresa> {

    private final Empresa original;

    /**
     * Clona la configuración base de la empresa original.
     * El clon inicia siempre en INACTIVA con NIT/razonSocial vacíos
     * para que el usuario los complete — no tiene sentido clonarlos.
     */
    @Override
    public Empresa clonar() {
        return Empresa.builder()
                .sector(original.getSector())
                .direccion(original.getDireccion())
                .municipio(original.getMunicipio())
                .telefono(original.getTelefono())
                .nombreContacto(original.getNombreContacto())
                .correo(original.getCorreo())
                .areasDisponibles(new ArrayList<>(original.getAreasDisponibles()))
                .estado(EstadoEmpresa.INACTIVA)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(LocalDateTime.now())
                .build();
    }
}
