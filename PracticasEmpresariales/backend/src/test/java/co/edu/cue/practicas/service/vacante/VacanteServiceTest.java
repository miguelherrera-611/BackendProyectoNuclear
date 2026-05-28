package co.edu.cue.practicas.service.vacante;

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
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VacanteService — Pruebas unitarias (GPE-152 / GPE-153)")
class VacanteServiceTest {

    @Mock private VacanteRepository vacanteRepository;
    @Mock private EmpresaService empresaService;
    @Mock private EmpresaValidator empresaValidator;
    @Mock private VacanteDirector vacanteDirector;
    @Mock private Dev3Mapper mapper;

    @InjectMocks
    private VacanteService service;

    private Empresa empresaAprobada;
    private Vacante vacanteEnPendiente;
    private VacanteResponse responseEjemplo;

    @BeforeEach
    void setUp() {
        empresaAprobada = Empresa.builder()
                .id(1L).razonSocial("TechCo").nit("900.1").nombreContacto("Ana")
                .estado(EstadoEmpresa.APROBADA).build();

        vacanteEnPendiente = Vacante.builder()
                .id(1L).empresa(empresaAprobada).area("Desarrollo")
                .cuposTotales(2).cuposOcupados(0).estado(EstadoVacante.PENDIENTE).build();

        responseEjemplo = new VacanteResponse(1L, 1L, "TechCo", "Desarrollo",
                2, 0, EstadoVacante.PENDIENTE, null, null, null);
    }

    // =================================================================
    // crearVacante — PATRÓN BUILDER via Director
    // =================================================================

    @Test
    @DisplayName("crearVacante exitoso para empresa APROBADA debe persistir y retornar DTO")
    void crearVacanteExitoso() {
        CrearVacanteRequest req = new CrearVacanteRequest(1L, "Desarrollo", 2);

        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        when(vacanteDirector.construirVacanteEstandar(empresaAprobada, "Desarrollo", 2))
                .thenReturn(vacanteEnPendiente);
        when(vacanteRepository.save(vacanteEnPendiente)).thenReturn(vacanteEnPendiente);
        when(mapper.toVacanteResponse(vacanteEnPendiente)).thenReturn(responseEjemplo);

        VacanteResponse resultado = service.crearVacante(req);

        assertThat(resultado.area()).isEqualTo("Desarrollo");
        assertThat(resultado.estado()).isEqualTo(EstadoVacante.PENDIENTE);
        verify(empresaValidator).validarEmpresaAprobadaParaVacantes(empresaAprobada);
        verify(vacanteRepository).save(vacanteEnPendiente);
    }

    @Test
    @DisplayName("crearVacante debe bloquear si la empresa no está APROBADA — OCL vacantesRequierenAprobacion")
    void crearVacanteEmpresaNoAprobadaLanzaExcepcion() {
        CrearVacanteRequest req = new CrearVacanteRequest(1L, "Desarrollo", 2);

        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        doThrow(new OperacionNoPermitidaException("Empresa no APROBADA"))
                .when(empresaValidator).validarEmpresaAprobadaParaVacantes(empresaAprobada);

        assertThatThrownBy(() -> service.crearVacante(req))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(vacanteRepository, never()).save(any());
    }

    // =================================================================
    // aprobarVacante — PATRÓN STATE: PENDIENTE → DISPONIBLE
    // =================================================================

    @Test
    @DisplayName("aprobarVacante debe cambiar estado a DISPONIBLE")
    void aprobarVacanteExitoso() {
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteEnPendiente));
        when(vacanteRepository.save(any())).thenReturn(vacanteEnPendiente);
        when(mapper.toVacanteResponse(any())).thenReturn(responseEjemplo);

        service.aprobarVacante(1L);

        assertThat(vacanteEnPendiente.getEstado()).isEqualTo(EstadoVacante.DISPONIBLE);
        verify(vacanteRepository).save(vacanteEnPendiente);
    }

    @Test
    @DisplayName("aprobarVacante desde DISPONIBLE debe lanzar excepción — transición inválida")
    void aprobarVacanteYaDisponibleLanzaExcepcion() {
        vacanteEnPendiente.aprobar(); // ya está DISPONIBLE
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteEnPendiente));

        assertThatThrownBy(() -> service.aprobarVacante(1L))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    // =================================================================
    // rechazarVacante — STATE: PENDIENTE → RECHAZADA
    // =================================================================

    @Test
    @DisplayName("rechazarVacante debe cambiar estado a RECHAZADA con motivo")
    void rechazarVacanteExitoso() {
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteEnPendiente));
        when(vacanteRepository.save(any())).thenReturn(vacanteEnPendiente);
        when(mapper.toVacanteResponse(any())).thenReturn(responseEjemplo);

        service.rechazarVacante(1L, new RechazarRequest("No cumple perfil requerido"));

        assertThat(vacanteEnPendiente.getEstado()).isEqualTo(EstadoVacante.RECHAZADA);
        assertThat(vacanteEnPendiente.getMotivoRechazo()).isEqualTo("No cumple perfil requerido");
    }

    @Test
    @DisplayName("rechazarVacante sin motivo debe lanzar excepción — motivo obligatorio")
    void rechazarVacanteSinMotivoLanzaExcepcion() {
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteEnPendiente));

        assertThatThrownBy(() -> service.rechazarVacante(1L, new RechazarRequest("")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("motivo");

        verify(vacanteRepository, never()).save(any());
    }

    // =================================================================
    // cerrarVacante — STATE: DISPONIBLE → CERRADA (irreversible)
    // =================================================================

    @Test
    @DisplayName("cerrarVacante debe cambiar estado a CERRADA")
    void cerrarVacanteExitoso() {
        vacanteEnPendiente.aprobar(); // primero aprobamos → DISPONIBLE
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteEnPendiente));
        when(vacanteRepository.save(any())).thenReturn(vacanteEnPendiente);
        when(mapper.toVacanteResponse(any())).thenReturn(responseEjemplo);

        service.cerrarVacante(1L);

        assertThat(vacanteEnPendiente.getEstado()).isEqualTo(EstadoVacante.CERRADA);
    }

    @Test
    @DisplayName("buscarOFallar debe lanzar 404 si la vacante no existe")
    void buscarNoEncontradoLanza404() {
        when(vacanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarOFallar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
