package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "practica_documentos", indexes = {
        @Index(name = "idx_doc_instancia", columnList = "instancia_practica_id"),
        @Index(name = "idx_doc_tipo", columnList = "tipo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_practica_id", nullable = false)
    private InstanciaPractica instanciaPractica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoDocumento tipo;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "ruta_archivo", nullable = false, length = 1000)
    private String rutaArchivo;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}

