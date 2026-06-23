package co.edu.cue.practicas.service.empresa;

import co.edu.cue.practicas.dto.request.CrearEmpresaRequest;
import co.edu.cue.practicas.dto.response.EmpresaResponse;
import co.edu.cue.practicas.event.EmpresaObserver;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.repository.empresa.EmpresaRepository;
import co.edu.cue.practicas.service.mapper.Dev3Mapper;
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias — EmpresaService (GPE-150)
 *
 * Usa JUnit 5 + Mockito. @Mock simula las dependencias sin tocar la BD real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaService — Pruebas unitarias (GPE-150)")
class EmpresaServiceTest {

    @Mock private EmpresaRepository empresaRepository;
    @Mock private EmpresaValidator validator;
    @Mock private Dev3Mapper mapper;
    @Mock private EmpresaObserver observerMock;

    private EmpresaService service;

    private Empresa empresaEjemplo;
    private EmpresaResponse responseEjemplo;

    @BeforeEach
    void setUp() {
        // EmpresaService necesita List<EmpresaObserver> — inyectamos uno para probar notificación
        service = new EmpresaService(empresaRepository, validator, mapper, List.of(observerMock));

        empresaEjemplo = Empresa.builder()
                .id(1L).razonSocial("TechCo S.A.").nit("900.123.456-7")
                .sector("Tecnología").direccion("Calle 10").municipio("Armenia")
                .telefono("3001234567").nombreContacto("Juan Díaz")
                .estado(EstadoEmpresa.INACTIVA).build();

        responseEjemplo = new EmpresaResponse(1L, "TechCo S.A.", "900.123.456-7",
                "Tecnología", "Calle 10", "Armenia", "3001234567", "Juan Díaz",
                null, EstadoEmpresa.INACTIVA, List.of(), null);
    }

    // =================================================================
    // crearEmpresa — PATRÓN BUILDER
    // =================================================================

    @Test
    @DisplayName("crearEmpresa debe usar el Builder, validar NIT único y persistir la entidad")
    void crearEmpresaExitoso() {
        CrearEmpresaRequest req = new CrearEmpresaRequest(
                "TechCo S.A.", "900.123.456-7", "Tech", "Calle 1", "Armenia",
                "300", "Contacto", "c@tech.com", List.of("Software"));

        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        EmpresaResponse resultado = service.crearEmpresa(req);

        assertThat(resultado.razonSocial()).isEqualTo("TechCo S.A.");
        assertThat(resultado.estado()).isEqualTo(EstadoEmpresa.INACTIVA);
        verify(validator).validarNitUnico("900.123.456-7");
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("crearEmpresa con NIT duplicado debe lanzar excepción antes de persistir")
    void crearEmpresaConNitDuplicadoLanzaExcepcion() {
        CrearEmpresaRequest req = new CrearEmpresaRequest(
                "Otra Empresa", "900.123.456-7", null, null, null, null,
                "Contacto", null, null);
        doThrow(new OperacionNoPermitidaException("Ya existe una empresa registrada con NIT: 900.123.456-7"))
                .when(validator).validarNitUnico("900.123.456-7");

        assertThatThrownBy(() -> service.crearEmpresa(req))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("900.123.456-7");

        verify(empresaRepository, never()).save(any());
    }

    // =================================================================
    // crearDesdeEmpresaExistente — PATRÓN PROTOTYPE
    // =================================================================

    @Test
    @DisplayName("crearDesdeEmpresaExistente debe clonar la empresa origen con nuevo NIT y razón social")
    void crearDesdeEmpresaExistenteExitoso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(empresaRepository.save(any())).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        service.crearDesdeEmpresaExistente(1L, "TechCo Sucursal", "900.999.111-2");

        // Debe haber validado NIT único del clon
        verify(validator).validarNitUnico("900.999.111-2");
        // Debe haber persistido el clon (siempre INACTIVA, sin razón social/NIT heredados)
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("crearDesdeEmpresaExistente con empresa origen inexistente debe lanzar 404")
    void crearDesdeEmpresaExistenteOrigenNoExisteLanzaExcepcion() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearDesdeEmpresaExistente(99L, "Clon", "900.1"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(empresaRepository, never()).save(any());
    }

    // =================================================================
    // editarEmpresa
    // =================================================================

    @Test
    @DisplayName("editarEmpresa exitoso debe persistir los nuevos datos sin validar NIT si no cambió")
    void editarEmpresaExitoso() {
        co.edu.cue.practicas.dto.request.EditarEmpresaRequest req = new co.edu.cue.practicas.dto.request.EditarEmpresaRequest(
                "TechCo Actualizada", "900.123.456-7", "Software", "Calle 20", "Calarcá",
                "3009999999", "Nuevo Contacto", "nuevo@tech.com", List.of("Backend"));

        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(empresaRepository.save(any())).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        EmpresaResponse resultado = service.editarEmpresa(1L, req);

        assertThat(empresaEjemplo.getRazonSocial()).isEqualTo("TechCo Actualizada");
        assertThat(empresaEjemplo.getDireccion()).isEqualTo("Calle 20");
        assertThat(resultado).isEqualTo(responseEjemplo);
        verify(validator, never()).validarNitUnicoParaEdicion(any(), any());
        verify(empresaRepository).save(empresaEjemplo);
    }

    @Test
    @DisplayName("editarEmpresa con NIT nuevo duplicado debe lanzar excepción antes de persistir")
    void editarEmpresaConNitDuplicadoLanzaExcepcion() {
        co.edu.cue.practicas.dto.request.EditarEmpresaRequest req = new co.edu.cue.practicas.dto.request.EditarEmpresaRequest(
                "TechCo S.A.", "900.999.999-9", "Tecnología", "Calle 10", "Armenia",
                "3001234567", "Juan Díaz", "c@tech.com", List.of());

        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        doThrow(new OperacionNoPermitidaException("Ya existe otra empresa registrada con NIT: 900.999.999-9"))
                .when(validator).validarNitUnicoParaEdicion("900.999.999-9", 1L);

        assertThatThrownBy(() -> service.editarEmpresa(1L, req))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("900.999.999-9");

        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("editarEmpresa con id inexistente debe lanzar 404")
    void editarEmpresaNoExisteLanza404() {
        co.edu.cue.practicas.dto.request.EditarEmpresaRequest req = new co.edu.cue.practicas.dto.request.EditarEmpresaRequest(
                "Cualquiera", "900.000.000-0", null, null, null, null, "Contacto", null, null);

        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editarEmpresa(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(empresaRepository, never()).save(any());
    }

    // =================================================================
    // Lecturas
    // =================================================================

    @Test
    @DisplayName("obtenerPorId debe retornar el DTO de la empresa encontrada")
    void obtenerPorIdExitoso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(mapper.toEmpresaResponse(empresaEjemplo)).thenReturn(responseEjemplo);

        EmpresaResponse resultado = service.obtenerPorId(1L);

        assertThat(resultado).isEqualTo(responseEjemplo);
    }

    @Test
    @DisplayName("listarActivas debe filtrar por estado ACTIVA en el repositorio")
    void listarActivasExitoso() {
        when(empresaRepository.findByEstado(EstadoEmpresa.ACTIVA)).thenReturn(List.of(empresaEjemplo));
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        List<EmpresaResponse> resultado = service.listarActivas();

        assertThat(resultado).hasSize(1);
        verify(empresaRepository).findByEstado(EstadoEmpresa.ACTIVA);
    }

    @Test
    @DisplayName("listarTodas debe mapear todas las empresas del repositorio")
    void listarTodasExitoso() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaEjemplo));
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        List<EmpresaResponse> resultado = service.listarTodas();

        assertThat(resultado).hasSize(1);
    }

    // =================================================================
    // activarEmpresa — PATRÓN OBSERVER + STATE
    // =================================================================

    @Test
    @DisplayName("activarEmpresa debe cambiar estado a ACTIVA y notificar observers")
    void activarEmpresaExitoso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(empresaRepository.save(any())).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        service.activarEmpresa(1L);

        assertThat(empresaEjemplo.getEstado()).isEqualTo(EstadoEmpresa.ACTIVA);
        // PATRÓN OBSERVER: el observer debe recibir la notificación
        verify(observerMock).onEmpresaEvento(1L, "EMPRESA_ACTIVADA");
        verify(empresaRepository).save(empresaEjemplo);
    }

    @Test
    @DisplayName("activarEmpresa ya ACTIVA debe lanzar excepción y no notificar de nuevo")
    void activarEmpresaYaActivaLanzaExcepcion() {
        empresaEjemplo.setEstado(EstadoEmpresa.ACTIVA);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));

        assertThatThrownBy(() -> service.activarEmpresa(1L))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(empresaRepository, never()).save(any());
    }

    // =================================================================
    // inactivarEmpresa — OBSERVER notifica tutores
    // =================================================================

    @Test
    @DisplayName("inactivarEmpresa exitoso debe notificar a observers para inactivar tutores")
    void inactivarEmpresaNotificaObservers() {
        empresaEjemplo.setEstado(EstadoEmpresa.ACTIVA);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(empresaRepository.save(any())).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        service.inactivarEmpresa(1L);

        assertThat(empresaEjemplo.getEstado()).isEqualTo(EstadoEmpresa.INACTIVA);
        // OBSERVER: TutorInactivacionObserver escucha "EMPRESA_INACTIVADA"
        verify(observerMock).onEmpresaEvento(1L, "EMPRESA_INACTIVADA");
        verify(validator).validarSinVacantesActivas(1L);
    }

    @Test
    @DisplayName("inactivarEmpresa debe lanzar excepción si tiene vacantes activas — OCL vinculacionRestringida")
    void inactivarConVacantesActivasLanzaExcepcion() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        doThrow(new OperacionNoPermitidaException("Tiene vacantes activas"))
                .when(validator).validarSinVacantesActivas(1L);

        assertThatThrownBy(() -> service.inactivarEmpresa(1L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("activas");

        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("inactivarEmpresa ya INACTIVA debe lanzar excepción")
    void inactivarEmpresaYaInactivaLanzaExcepcion() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));

        assertThatThrownBy(() -> service.inactivarEmpresa(1L))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(empresaRepository, never()).save(any());
    }

    // =================================================================
    // buscarOFallar
    // =================================================================

    @Test
    @DisplayName("buscarOFallar debe lanzar 404 si la empresa no existe")
    void buscarNoEncontradoLanza404() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarOFallar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
