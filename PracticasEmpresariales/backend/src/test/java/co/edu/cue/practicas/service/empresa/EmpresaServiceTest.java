package co.edu.cue.practicas.service.empresa;

import co.edu.cue.practicas.dto.request.CrearEmpresaRequest;
import co.edu.cue.practicas.dto.request.RechazarRequest;
import co.edu.cue.practicas.dto.response.EmpresaResponse;
import co.edu.cue.practicas.event.EmpresaObserver;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.pattern.builder.EmpresaBuilder;
import co.edu.cue.practicas.pattern.prototype.EmpresaPlantilla;
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
                .estado(EstadoEmpresa.PENDIENTE).build();

        responseEjemplo = new EmpresaResponse(1L, "TechCo S.A.", "900.123.456-7",
                "Tecnología", "Calle 10", "Armenia", "3001234567", "Juan Díaz",
                null, EstadoEmpresa.PENDIENTE, List.of(), null);
    }

    // =================================================================
    // crearEmpresa — PATRÓN BUILDER
    // =================================================================

    @Test
    @DisplayName("crearEmpresa debe usar el Builder y persistir la entidad")
    void crearEmpresaExitoso() {
        CrearEmpresaRequest req = new CrearEmpresaRequest(
                "TechCo", "900.123.456-7", "Tech", "Calle 1", "Armenia",
                "300", "Contacto", "c@tech.com", List.of("Software"));

        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        EmpresaResponse resultado = service.crearEmpresa(req);

        assertThat(resultado.razonSocial()).isEqualTo("TechCo S.A.");
        verify(validator).validarNitUnico("900.123.456-7");
        verify(empresaRepository).save(any(Empresa.class));
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

        EmpresaResponse resultado = service.crearDesdeEmpresaExistente(1L, "TechCo Sucursal", "900.999.111-2");

        // Debe haber validado NIT único del clon
        verify(validator).validarNitUnico("900.999.111-2");
        // Debe haber persistido el clon
        verify(empresaRepository).save(any(Empresa.class));
    }

    // =================================================================
    // aprobarEmpresa — PATRÓN OBSERVER + STATE
    // =================================================================

    @Test
    @DisplayName("aprobarEmpresa debe cambiar estado a APROBADA y notificar observers")
    void aprobarEmpresaExitoso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(empresaRepository.save(any())).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        service.aprobarEmpresa(1L);

        assertThat(empresaEjemplo.getEstado()).isEqualTo(EstadoEmpresa.APROBADA);
        // PATRÓN OBSERVER: el observer debe recibir la notificación
        verify(observerMock).onEmpresaEvento(1L, "EMPRESA_APROBADA");
    }

    // =================================================================
    // rechazarEmpresa — STATE
    // =================================================================

    @Test
    @DisplayName("rechazarEmpresa debe cambiar estado a RECHAZADA")
    void rechazarEmpresaExitoso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(empresaRepository.save(any())).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        service.rechazarEmpresa(1L, new RechazarRequest("No cumple requisitos legales"));

        assertThat(empresaEjemplo.getEstado()).isEqualTo(EstadoEmpresa.RECHAZADA);
        verify(observerMock).onEmpresaEvento(1L, "EMPRESA_RECHAZADA");
    }

    @Test
    @DisplayName("rechazarEmpresa con motivo vacío debe lanzar excepción antes de persistir")
    void rechazarEmpresaSinMotivoLanzaExcepcion() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));

        assertThatThrownBy(() -> service.rechazarEmpresa(1L, new RechazarRequest("")))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("motivo");

        verify(empresaRepository, never()).save(any());
    }

    // =================================================================
    // inactivarEmpresa — OBSERVER notifica tutores
    // =================================================================

    @Test
    @DisplayName("inactivarEmpresa exitoso debe notificar a observers para inactivar tutores")
    void inactivarEmpresaNotificaObservers() {
        empresaEjemplo.aprobar(); // primero aprobamos
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaEjemplo));
        when(empresaRepository.save(any())).thenReturn(empresaEjemplo);
        when(mapper.toEmpresaResponse(any())).thenReturn(responseEjemplo);

        service.inactivarEmpresa(1L);

        assertThat(empresaEjemplo.getEstado()).isEqualTo(EstadoEmpresa.INACTIVA);
        // OBSERVER: TutorInactivacionObserver escucha "EMPRESA_INACTIVADA"
        verify(observerMock).onEmpresaEvento(1L, "EMPRESA_INACTIVADA");
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
