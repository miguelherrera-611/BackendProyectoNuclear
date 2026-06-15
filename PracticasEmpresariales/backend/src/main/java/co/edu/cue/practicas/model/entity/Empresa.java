package co.edu.cue.practicas.model.entity;

import co.edu.cue.practicas.event.EmpresaObserver;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.converter.EstadoEmpresaConverter;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GPE-150 — Entidad Empresa
 *
 * PATRÓN OBSERVER — Subject:
 *   Mantiene lista @Transient de EmpresaObserver (interfaz, no concreciones).
 *   Notifica a todos los observers al cambiar estado.
 *
 * SOLID — SRP: solo gestiona el estado y las reglas de negocio de Empresa.
 *              El mapping a DTO está en EmpresaMapper (clase separada).
 *              La validación de precondiciones externas está en EmpresaValidator.
 *
 * SOLID — DIP: depende de EmpresaObserver (interfaz), nunca de implementaciones.
 *
 * Reglas OCL embebidas:
 *   nitUnico, vacantesRequierenAprobacion, vinculacionRestringida, PROHIBIDO eliminar.
 */
@Entity
@Table(name = "empresas", indexes = {
        @Index(name = "idx_empresa_nit", columnList = "nit", unique = true),
        @Index(name = "idx_empresa_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @NotBlank
    @Column(name = "nit", nullable = false, unique = true, length = 20)
    private String nit;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "municipio", length = 100)
    private String municipio;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @NotBlank
    @Column(name = "nombre_contacto", nullable = false, length = 150)
    private String nombreContacto;

    @Email
    @Column(name = "correo", length = 150)
    private String correo;

    @Convert(converter = EstadoEmpresaConverter.class)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoEmpresa estado = EstadoEmpresa.INACTIVA;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "empresa_areas", joinColumns = @JoinColumn(name = "empresa_id"))
    @Column(name = "area")
    @Builder.Default
    private List<String> areasDisponibles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.actualizadoEn = LocalDateTime.now(); }

    // ── PATRÓN OBSERVER — Subject ─────────────────────────────────────────
    // @Transient: no persiste. Depende de la interfaz (DIP).
    @Transient
    @Builder.Default
    private List<EmpresaObserver> observers = new ArrayList<>();

    public void agregarObserver(EmpresaObserver o) {
        observers.add(o);
    }

    private void notificar(String evento) {
        observers.forEach(o -> o.onEmpresaEvento(this.id, evento));
    }

    // ── Transiciones de estado (OCL: estadoValido) ────────────────────────

    public void activar() {
        if (this.estado == EstadoEmpresa.ACTIVA)
            throw new OperacionNoPermitidaException("La empresa ya está activa.");
        this.estado = EstadoEmpresa.ACTIVA;
        notificar("EMPRESA_ACTIVADA");
    }

    public void inactivar() {
        if (this.estado == EstadoEmpresa.INACTIVA)
            throw new OperacionNoPermitidaException("La empresa ya está inactiva.");
        this.estado = EstadoEmpresa.INACTIVA;
        notificar("EMPRESA_INACTIVADA");
    }

    // ── OCL helpers ───────────────────────────────────────────────────────

    public boolean puedeCrearVacantes() {
        return this.estado == EstadoEmpresa.ACTIVA;
    }

    public boolean puedeVincularPracticantes() {
        return this.estado == EstadoEmpresa.ACTIVA;
    }
}
