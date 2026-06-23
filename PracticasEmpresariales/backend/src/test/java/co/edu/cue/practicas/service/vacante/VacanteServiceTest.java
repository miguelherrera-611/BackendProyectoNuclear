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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias — VacanteService (GPE-152 / GPE-153)
 * Cubre el PATRÓN STATE (Vacante.activar/desactivar) y el PATRÓN BUILDER (via VacanteDirector).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VacanteService — Pruebas unitarias (GPE-152 / GPE-153)")
class VacanteServiceTest {

    @Mock private VacanteRepository vacanteRepository;
    @Mock private EmpresaService empresaService;
    @Mock private EmpresaValidator empresaValidator;
    @Mock private VacanteDirector vacanteDirector;
    @Mock private Dev3Mapper mapper;

    @InjectMocks
    private VacanteService vacanteService;

    private Empresa empresaActiva;
    private Vacante vacanteDisponible;
    private VacanteResponse vacanteResponseMock;

    @BeforeEach
    void setUp() {
        empresaActiva = Empresa.builder()
                .id(1L).razonSocial("TechCo S.A.").nit("900.123.456-7")
                .estado(EstadoEmpresa.ACTIVA).build();

        vacanteDisponible = Vacante.builder()
                .id(1L).empresa(empresaActiva).area("Desarrollo de software")
                .cuposTotales(2).cuposOcupados(0).estado(EstadoVacante.DISPONIBLE)
                .fechaPublicacion(LocalDate.now()).build();

        vacanteResponseMock = new VacanteResponse(
                1L, 1L, "TechCo S.A.", "Desarrollo de software",
                2, 0, EstadoVacante.DISPONIBLE, LocalDate.now(), null, null
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // crearVacante — PATRÓN BUILDER vía VacanteDirector
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crearVacante para empresa ACTIVA debe usar el Director y persistir")
    void crearVacante_empresaActiva_debeCrearVacante() {
        CrearVacanteRequest request = new CrearVacanteRequest(1L, "Desarrollo de software", 2);
        when(empresaService.buscarOFallar(1L)).thenReturn(empresaActiva);
        when(vacanteDirector.construirVacanteEstandar(any(), any(), anyInt()))
                .thenReturn(vacanteDisponible);
        when(vacanteRepository.save(any())).thenReturn(vacanteDisponible);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        VacanteResponse resultado = vacanteService.crearVacante(request);

        assertThat(resultado.area()).isEqualTo("Desarrollo de software");
        assertThat(resultado.cuposTotales()).isEqualTo(2);
        verify(empresaValidator).validarEmpresaAprobadaParaVacantes(empresaActiva);
        verify(vacanteDirector).construirVacanteEstandar(empresaActiva, "Desarrollo de software", 2);
        verify(vacanteRepository).save(any());
    }

    @Test
    @DisplayName("crearVacante para empresa NO activa debe lanzar excepción — OCL vacantesRequierenAprobacion")
    void crearVacante_empresaNoActiva_debeLanzarExcepcion() {
        CrearVacanteRequest request = new CrearVacanteRequest(1L, "Desarrollo", 2);
        when(empresaService.buscarOFallar(1L)).thenReturn(empresaActiva);
        doThrow(new OperacionNoPermitidaException("Empresa no activa"))
                .when(empresaValidator).validarEmpresaAprobadaParaVacantes(any());

        assertThatThrownBy(() -> vacanteService.crearVacante(request))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(vacanteRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // editarVacante
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("editarVacante exitoso debe actualizar area y cuposTotales")
    void editarVacanteExitoso() {
        co.edu.cue.practicas.dto.request.EditarVacanteRequest req =
                new co.edu.cue.practicas.dto.request.EditarVacanteRequest("QA", 5);

        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));
        when(vacanteRepository.save(any())).thenReturn(vacanteDisponible);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        vacanteService.editarVacante(1L, req);

        assertThat(vacanteDisponible.getArea()).isEqualTo("QA");
        assertThat(vacanteDisponible.getCuposTotales()).isEqualTo(5);
        verify(vacanteRepository).save(vacanteDisponible);
    }

    @Test
    @DisplayName("editarVacante con cuposTotales menores a los ocupados debe lanzar excepción")
    void editarVacanteCuposMenoresAOcupadosLanzaExcepcion() {
        vacanteDisponible.ocuparCupo();
        vacanteDisponible.ocuparCupo(); // 2 cupos ocupados de 2 totales

        co.edu.cue.practicas.dto.request.EditarVacanteRequest req =
                new co.edu.cue.practicas.dto.request.EditarVacanteRequest("QA", 1);

        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));

        assertThatThrownBy(() -> vacanteService.editarVacante(1L, req))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ocupados");

        verify(vacanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("editarVacante con id inexistente debe lanzar 404")
    void editarVacanteNoExisteLanza404() {
        co.edu.cue.practicas.dto.request.EditarVacanteRequest req =
                new co.edu.cue.practicas.dto.request.EditarVacanteRequest("QA", 1);

        when(vacanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacanteService.editarVacante(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(vacanteRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Lecturas
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("listarDisponibles debe filtrar por estado DISPONIBLE")
    void listarDisponiblesExitoso() {
        when(vacanteRepository.findByEstado(EstadoVacante.DISPONIBLE)).thenReturn(java.util.List.of(vacanteDisponible));
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        var resultado = vacanteService.listarDisponibles();

        assertThat(resultado).hasSize(1);
        verify(vacanteRepository).findByEstado(EstadoVacante.DISPONIBLE);
    }

    @Test
    @DisplayName("listarPorEmpresa debe delegar en el repositorio por empresaId")
    void listarPorEmpresaExitoso() {
        when(vacanteRepository.findByEmpresaId(1L)).thenReturn(java.util.List.of(vacanteDisponible));
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        var resultado = vacanteService.listarPorEmpresa(1L);

        assertThat(resultado).hasSize(1);
    }

    // ═══════════════════════════════════════════════════════════════════
    // activarVacante / desactivarVacante — PATRÓN STATE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("activarVacante debe reactivar una vacante CERRADA y volverla DISPONIBLE")
    void activarVacanteExitoso() {
        vacanteDisponible.setEstado(EstadoVacante.CERRADA);
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));
        when(vacanteRepository.save(any())).thenReturn(vacanteDisponible);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        vacanteService.activarVacante(1L);

        assertThat(vacanteDisponible.getEstado()).isEqualTo(EstadoVacante.DISPONIBLE);
        verify(vacanteRepository).save(vacanteDisponible);
    }

    @Test
    @DisplayName("activarVacante ya DISPONIBLE debe lanzar excepción")
    void activarVacanteYaDisponibleLanzaExcepcion() {
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));

        assertThatThrownBy(() -> vacanteService.activarVacante(1L))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(vacanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivarVacante debe cambiar el estado a CERRADA")
    void desactivarVacanteExitoso() {
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));
        when(vacanteRepository.save(any())).thenReturn(vacanteDisponible);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        vacanteService.desactivarVacante(1L);

        assertThat(vacanteDisponible.getEstado()).isEqualTo(EstadoVacante.CERRADA);
    }

    @Test
    @DisplayName("desactivarVacante ya CERRADA debe lanzar excepción (estado terminal)")
    void desactivarVacanteYaCerradaLanzaExcepcion() {
        vacanteDisponible.setEstado(EstadoVacante.CERRADA);
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));

        assertThatThrownBy(() -> vacanteService.desactivarVacante(1L))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(vacanteRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Alias legacy — aprobarVacante / rechazarVacante / cerrarVacante
    // Mantenidos por compatibilidad interna: delegan en activar/desactivar.
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("aprobarVacante (legacy) debe delegar en activarVacante")
    void aprobarVacanteLegacyDelegaEnActivar() {
        vacanteDisponible.setEstado(EstadoVacante.CERRADA);
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));
        when(vacanteRepository.save(any())).thenReturn(vacanteDisponible);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        vacanteService.aprobarVacante(1L);

        assertThat(vacanteDisponible.getEstado()).isEqualTo(EstadoVacante.DISPONIBLE);
    }

    @Test
    @DisplayName("rechazarVacante (legacy) debe delegar en desactivarVacante, ignorando el motivo")
    void rechazarVacanteLegacyDelegaEnDesactivar() {
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));
        when(vacanteRepository.save(any())).thenReturn(vacanteDisponible);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        vacanteService.rechazarVacante(1L, new RechazarRequest("No aplica al programa"));

        assertThat(vacanteDisponible.getEstado()).isEqualTo(EstadoVacante.CERRADA);
    }

    @Test
    @DisplayName("cerrarVacante (legacy) debe delegar en desactivarVacante")
    void cerrarVacanteLegacyDelegaEnDesactivar() {
        when(vacanteRepository.findById(1L)).thenReturn(Optional.of(vacanteDisponible));
        when(vacanteRepository.save(any())).thenReturn(vacanteDisponible);
        when(mapper.toVacanteResponse(any())).thenReturn(vacanteResponseMock);

        vacanteService.cerrarVacante(1L);

        assertThat(vacanteDisponible.getEstado()).isEqualTo(EstadoVacante.CERRADA);
    }

    // ═══════════════════════════════════════════════════════════════════
    // OCL: cuposValidos / cerradaNoAcepta
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Vacante DISPONIBLE con cupos libres debe aceptar practicante")
    void vacante_conCuposDisponibles_debeAceptarPracticante() {
        assertThat(vacanteDisponible.puedeAceptarPracticante()).isTrue();
    }

    @Test
    @DisplayName("ocuparCupo debe cerrar automáticamente la vacante al llenar todos los cupos")
    void vacante_alLlenarCupos_seCierraAutomaticamente() {
        vacanteDisponible.ocuparCupo();
        vacanteDisponible.ocuparCupo(); // llena los 2 cupos → se cierra automáticamente

        assertThat(vacanteDisponible.puedeAceptarPracticante()).isFalse();
        assertThat(vacanteDisponible.getEstado()).isEqualTo(EstadoVacante.CERRADA);
    }

    @Test
    @DisplayName("liberarCupo sobre una vacante cerrada por cupos llenos debe reabrirla")
    void vacante_liberarCupo_reabreVacanteCerradaPorCuposLlenos() {
        vacanteDisponible.ocuparCupo();
        vacanteDisponible.ocuparCupo(); // CERRADA por cupos llenos

        vacanteDisponible.liberarCupo();

        assertThat(vacanteDisponible.getEstado()).isEqualTo(EstadoVacante.DISPONIBLE);
        assertThat(vacanteDisponible.getCuposOcupados()).isEqualTo(1);
    }

    @Test
    @DisplayName("buscarOFallar debe lanzar 404 si la vacante no existe")
    void buscarNoEncontradoLanza404() {
        when(vacanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacanteService.buscarOFallar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
