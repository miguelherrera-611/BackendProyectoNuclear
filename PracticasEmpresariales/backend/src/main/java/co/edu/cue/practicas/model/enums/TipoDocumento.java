package co.edu.cue.practicas.model.enums;

import java.util.Set;

public enum TipoDocumento {
    CARTA_PRESENTACION("Carta de presentación", Set.of("application/pdf", "image/jpeg", "image/png")),
    CONVENIO("Convenio", Set.of("application/pdf", "image/jpeg", "image/png")),
    OTRO("Otro", Set.of("application/pdf", "image/jpeg", "image/png"));

    private final String descripcion;
    private final Set<String> mimeTypesPermitidos;

    TipoDocumento(String descripcion, Set<String> mimeTypesPermitidos) {
        this.descripcion = descripcion;
        this.mimeTypesPermitidos = mimeTypesPermitidos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean admiteMimeType(String mimeType) {
        return mimeType != null && mimeTypesPermitidos.contains(mimeType.toLowerCase());
    }
}

