package co.edu.cue.practicas.service;

import co.edu.cue.practicas.dto.request.CrearVacanteRequest;
import co.edu.cue.practicas.dto.request.RechazarRequest;
import co.edu.cue.practicas.dto.response.VacanteResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.Vacante;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import co.edu.cue.practicas.pattern.builder.VacanteDirector;
import co.edu.cue.practicas.repository.vacante.VacanteRepository;
import co.edu.cue.practicas.service.empresa.EmpresaService;
import co.edu.cue.practicas.service.mapper.Dev3Mapper;
import co.edu.cue.practicas.service.vacante.VacanteService;
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias — VacanteService (GPE-152 / GPE-153)
 * Cubre el PATRÓN STATE y el PATRÓN BUILDER (via Director).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GPE-152/153 — Pruebas VacanteService")
class VacanteServiceTest {

    @Mock private VacanteRepository vacanteRepository;
    @Mock private EmpresaService empresaService;
    @Mock private EmpresaValidator empresaValidator;
    @Mock private VacanteDirector vacanteDirector;
    @Mock private Dev3Mapper mapper;

    @InjectMocks
    private VacanteService vacanteService;

    private Empresa empresaAprobada;
    private Vacante vacantePendiente;
    private VacanteResponse vacanteResponseMock;

    @BeforeEach
    void setUp() {
        empresaAprobada = Empresa.builder()
                .id(1L)
                .razonSocial("TechCo S.A.")
                .nit("900.123.456-7")
                .estado(EstadoEmpresa.APROBADA)
                .build();

        vacantePendiente = Vacante.builder()
                .id(1L)
                .empresa(empresaAprobada)
                .area("Desarrollo de software")
                .cuposTotales(2)
                .cuposOcupados(0)
                .estado(EstadoVacante.PENDIENTE)
                .fechaPublicacion(LocalDate.now())
                .build();

        vacanteResponseMock = new VacanteResponse(
                1L, 1L, "TechCo S.A.", "Desarrollo de software",
                2, 0, EstadoVacante.PENDIENTE, LocalDate.now(), null, null
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — crearVacante (GPE-152) — PATRÓN BUILDER
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Crear vacante para empresa APROBADA debe funcionar")
    void crearVacante_empresaAprobada_debeCrearVacante() {
        // ARRANGE
        CrearVacanteRequest request = new CrearVacanteRequest(1L, "Desarrollo de software", 2);
        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        when(vacanteDirector.construirVacanteEstandar(any(), any(), anyInt()))
                .thenReturn(vacantePendiente);
        when(vacanteRepository.save(any())).thenReturn(vacantePendiente);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        // ACT
        VacanteResponse resultado = vacanteService.crearVacante(request);

        // ASSERT
        assertNotNull(resultado);
        assertEquals("Desarrollo de software", resultado.area());
        assertEquals(EstadoVacante.PENDIENTE, resultado.estado());
        assertEquals(2, resultado.cuposTotales());
        // Verificar que se usó el Director (PATRÓN BUILDER)
        verify(vacanteDirector).construirVacanteEstandar(empresaAprobada, "Desarrollo de software", 2);
        verify(vacanteRepository).save(any());
    }

    @Test
    @DisplayName("❌ Crear vacante para empresa NO APROBADA debe lanzar excepción")
    void crearVacante_empresaNoAprobada_debeLanzarExcepcion() {
        // ARRANGE
        CrearVacanteRequest request = new CrearVacanteRequest(1L, "Desarrollo", 2);
        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        doThrow(new OperacionNoPermitidaException("Empresa no aprobada"))
                .when(empresaValidator).validarEmpresaAprobadaParaVacantes(any());

        // ACT & ASSERT
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> vacanteService.crearVacante(request)
        );
        verify(vacanteRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — aprobarVacante (GPE-153) — PATRÓN STATE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Aprobar vacante PENDIENTE debe cambiar estado a DISPONIBLE")
    void aprobarVacante_estaPendiente_debeQuedarDisponible() {
        // ARRANGE
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacantePendiente));
        when(vacanteRepository.save(any())).thenReturn(vacantePendiente);
        VacanteResponse responseDisponible = new VacanteResponse(
                1L, 1L, "TechCo", "Desarrollo", 2, 0,
                EstadoVacante.DISPONIBLE, LocalDate.now(), null, null
        );
        when(mapper.toVacanteResponse(any())).thenReturn(responseDisponible);

        // ACT
        VacanteResponse resultado = vacanteService.aprobarVacante(1L);

        // ASSERT — PATRÓN STATE: la transición ocurrió en la entidad
        assertEquals(EstadoVacante.DISPONIBLE, resultado.estado());
        assertEquals(EstadoVacante.DISPONIBLE, vacantePendiente.getEstado());
        verify(vacanteRepository).save(vacantePendiente);
    }

    @Test
    @DisplayName("❌ Aprobar vacante que no está PENDIENTE debe lanzar excepción")
    void aprobarVacante_noEstaPendiente_debeLanzarExcepcion() {
        // ARRANGE — vacante ya DISPONIBLE
        vacantePendiente.aprobar(); // la pasamos a DISPONIBLE
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacantePendiente));

        // ACT & ASSERT — PATRÓN STATE: transición inválida
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> vacanteService.aprobarVacante(1L)
        );
        verify(vacanteRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — rechazarVacante (GPE-153) — PATRÓN STATE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Rechazar vacante con motivo válido debe cambiar estado a RECHAZADA")
    void rechazarVacante_conMotivo_debeQuedarRechazada() {
        // ARRANGE
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacantePendiente));
        when(vacanteRepository.save(any())).thenReturn(vacantePendiente);
        VacanteResponse responseRechazada = new VacanteResponse(
                1L, 1L, "TechCo", "Desarrollo", 2, 0,
                EstadoVacante.RECHAZADA, LocalDate.now(), "No aplica al programa", null
        );
        when(mapper.toVacanteResponse(any())).thenReturn(responseRechazada);

        // ACT
        VacanteResponse resultado = vacanteService.rechazarVacante(
                1L, new RechazarRequest("No aplica al programa")
        );

        // ASSERT
        assertEquals(EstadoVacante.RECHAZADA, resultado.estado());
        assertEquals("No aplica al programa", resultado.motivoRechazo());
        assertEquals(EstadoVacante.RECHAZADA, vacantePendiente.getEstado());
    }

    @Test
    @DisplayName("❌ Rechazar vacante sin motivo debe lanzar excepción")
    void rechazarVacante_sinMotivo_debeLanzarExcepcion() {
        // ARRANGE
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacantePendiente));

        // ACT & ASSERT
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> vacanteService.rechazarVacante(1L, new RechazarRequest(""))
        );
        verify(vacanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Rechazar vacante que no existe debe lanzar RecursoNoEncontradoException")
    void rechazarVacante_noExiste_debeLanzarExcepcion() {
        // ARRANGE
        when(vacanteRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> vacanteService.rechazarVacante(99L, new RechazarRequest("Motivo"))
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — cerrarVacante — PATRÓN STATE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Cerrar vacante DISPONIBLE debe cambiar estado a CERRADA")
    void cerrarVacante_estaDisponible_debeQuedarCerrada() {
        // ARRANGE
        vacantePendiente.aprobar(); // DISPONIBLE
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacantePendiente));
        when(vacanteRepository.save(any())).thenReturn(vacantePendiente);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        // ACT
        vacanteService.cerrarVacante(1L);

        // ASSERT — PATRÓN STATE: DISPONIBLE → CERRADA
        assertEquals(EstadoVacante.CERRADA, vacantePendiente.getEstado());
    }

    @Test
    @DisplayName("❌ Cerrar vacante CERRADA debe lanzar excepción (estado terminal)")
    void cerrarVacante_yaCerrada_debeLanzarExcepcion() {
        // ARRANGE
        vacantePendiente.aprobar();
        vacantePendiente.cerrar(); // ya está CERRADA
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacantePendiente));

        // ACT & ASSERT — PATRÓN STATE: transición inválida desde estado terminal
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> vacanteService.cerrarVacante(1L)
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — OCL: cuposValidos
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Vacante con cupos disponibles debe aceptar practicante")
    void vacante_conCuposDisponibles_debeAceptarPracticante() {
        vacantePendiente.aprobar();
        assertTrue(vacantePendiente.puedeAceptarPracticante());
    }

    @Test
    @DisplayName("❌ Vacante sin cupos no debe aceptar practicante")
    void vacante_sinCupos_noDebeAceptarPracticante() {
        vacantePendiente.aprobar();
        vacantePendiente.ocuparCupo();
        vacantePendiente.ocuparCupo(); // llena los 2 cupos → se cierra automáticamente
        assertFalse(vacantePendiente.puedeAceptarPracticante());
        assertEquals(EstadoVacante.CERRADA, vacantePendiente.getEstado());
    }
}
