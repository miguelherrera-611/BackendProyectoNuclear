package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.enums.EstadoHojaDeVida;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GPE-146 — Entidad HojaDeVida (subobjeto versionado)
 *
 * Cada actualización genera una nueva versión; la anterior persiste en el historial.
 * La versión con número más alto es la vigente.
 *
 * OCL:
 *   hvInmutableEnPractica: si la práctica activa está EN_CURSO, no puede reemplazarse.
 *   versionPositiva: version siempre > 0.
 *   perteneceAEstudiante: cada HV pertenece a exactamente un estudiante.
 *   eliminarProhibido: NUNCA se elimina una HV.
 *
 * SOLID — SRP: solo encapsula los datos y reglas de una versión de HV.
 */
@Entity
@Table(name = "hojas_de_vida",
        indexes = {
                @Index(name = "idx_hv_estudiante", columnList = "estudiante_id"),
                @Index(name = "idx_hv_estado", columnList = "estado")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HojaDeVida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OCL: perteneceAEstudiante */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    /** Expediente al que pertenece esta HV — usado por la relación bidireccional con ExpedienteEstudiante */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id")
    private ExpedienteEstudiante expediente;

    /** OCL: versionPositiva — auto-gestionado por HojaDeVidaService */
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "fecha_carga", nullable = false)
    @Builder.Default
    private LocalDate fechaCarga = LocalDate.now();

    @NotBlank
    @Column(name = "url_archivo", nullable = false, length = 500)
    private String urlArchivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoHojaDeVida estado = EstadoHojaDeVida.PENDIENTE;

    /** ID del usuario (Coordinación de Prácticas / Secretaría) que validó o rechazó esta HV */
    @Column(name = "validado_por")
    private Long validadoPor;

    @Column(name = "fecha_validacion")
    private LocalDate fechaValidacion;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    // ── Transiciones de estado ────────────────────────────────────────────────

    public void validar(Long validadorId) {
        if (this.estado != EstadoHojaDeVida.PENDIENTE)
            throw new OperacionNoPermitidaException("Solo se puede validar una HV en estado PENDIENTE.");
        this.estado = EstadoHojaDeVida.VALIDA;
        this.validadoPor = validadorId;
        this.fechaValidacion = LocalDate.now();
    }

    public void rechazar(Long validadorId, String motivo) {
        if (motivo == null || motivo.isBlank())
            throw new OperacionNoPermitidaException("El motivo de rechazo es obligatorio.");
        if (this.estado != EstadoHojaDeVida.PENDIENTE)
            throw new OperacionNoPermitidaException("Solo se puede rechazar una HV en estado PENDIENTE.");
        this.estado = EstadoHojaDeVida.RECHAZADA;
        this.validadoPor = validadorId;
        this.fechaValidacion = LocalDate.now();
        this.motivoRechazo = motivo;
    }

    public boolean esValida() {
        return this.estado == EstadoHojaDeVida.VALIDA;
    }
}
