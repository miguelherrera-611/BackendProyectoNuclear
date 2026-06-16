package co.edu.cue.practicas.model.enums;

import java.util.Set;

public enum TipoDocumento {
    CARTA_PRESENTACION("Carta de presentación",    Set.of(MimeType.PDF, MimeType.JPEG, MimeType.PNG)),
    CONVENIO("Convenio",                           Set.of(MimeType.PDF, MimeType.JPEG, MimeType.PNG)),
    FIRMA_TUTOR("Firma del Tutor Empresarial",     Set.of(MimeType.PDF, MimeType.JPEG, MimeType.PNG)),
    FIRMA_DOCENTE("Firma del Docente Asesor",      Set.of(MimeType.PDF, MimeType.JPEG, MimeType.PNG)),
    FIRMA_ESTUDIANTE("Firma del Estudiante",       Set.of(MimeType.PDF, MimeType.JPEG, MimeType.PNG)),
    ACTA_SUSTENTACION("Acta de sustentación firmada", Set.of(MimeType.PDF, MimeType.JPEG, MimeType.PNG)),
    OTRO("Otro",                                   Set.of(MimeType.PDF, MimeType.JPEG, MimeType.PNG));

    private static final class MimeType {
        private static final String PDF  = "application/pdf";
        private static final String JPEG = "image/jpeg";
        private static final String PNG  = "image/png";

        private MimeType() {}
    }

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