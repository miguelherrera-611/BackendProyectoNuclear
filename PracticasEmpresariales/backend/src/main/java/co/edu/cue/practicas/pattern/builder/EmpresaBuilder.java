package co.edu.cue.practicas.pattern.builder;

import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN BUILDER — EmpresaBuilder (ConcreteBuilder)
 *
 * Construye una Empresa paso a paso separando la construcción
 * de su representación final.
 *
 * SOLID — SRP: solo construye Empresa, no hace nada más.
 * SOLID — OCP: nuevos campos → nuevo método withX(), sin tocar Empresa.
 *
 * Uso:
 *   Empresa e = new EmpresaBuilder()
 *       .conRazonSocial("TechCo S.A.")
 *       .conNit("900.123.456-7")
 *       .conSector("Tecnología")
 *       .conContacto("Juan", "juan@techco.com")
 *       .build();
 */
public class EmpresaBuilder {

    private String razonSocial;
    private String nit;
    private String sector;
    private String direccion;
    private String municipio;
    private String telefono;
    private String nombreContacto;
    private String correo;
    private List<String> areasDisponibles = new ArrayList<>();

    public EmpresaBuilder conRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
        return this;
    }

    public EmpresaBuilder conNit(String nit) {
        this.nit = nit;
        return this;
    }

    public EmpresaBuilder conSector(String sector) {
        this.sector = sector;
        return this;
    }

    public EmpresaBuilder conDireccion(String direccion, String municipio) {
        this.direccion = direccion;
        this.municipio = municipio;
        return this;
    }

    public EmpresaBuilder conTelefono(String telefono) {
        this.telefono = telefono;
        return this;
    }

    public EmpresaBuilder conContacto(String nombre, String correo) {
        this.nombreContacto = nombre;
        this.correo = correo;
        return this;
    }

    public EmpresaBuilder conAreas(List<String> areas) {
        this.areasDisponibles = areas != null ? areas : new ArrayList<>();
        return this;
    }

    public Empresa build() {
        validar();
        return Empresa.builder()
                .razonSocial(razonSocial)
                .nit(nit)
                .sector(sector)
                .direccion(direccion)
                .municipio(municipio)
                .telefono(telefono)
                .nombreContacto(nombreContacto)
                .correo(correo)
                .areasDisponibles(areasDisponibles)
                .estado(EstadoEmpresa.INACTIVA)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(LocalDateTime.now())
                .build();
    }

    private void validar() {
        if (razonSocial == null || razonSocial.isBlank())
            throw new IllegalStateException("La razón social es obligatoria.");
        if (nit == null || nit.isBlank())
            throw new IllegalStateException("El NIT es obligatorio.");
        if (nombreContacto == null || nombreContacto.isBlank())
            throw new IllegalStateException("El nombre del contacto es obligatorio.");
    }
}
