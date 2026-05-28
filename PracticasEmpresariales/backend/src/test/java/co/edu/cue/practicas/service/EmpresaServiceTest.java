package co.edu.cue.practicas.service;

import co.edu.cue.practicas.dto.request.CrearEmpresaRequest;
import co.edu.cue.practicas.dto.request.RechazarRequest;
import co.edu.cue.practicas.dto.response.EmpresaResponse;
import co.edu.cue.practicas.event.EmpresaObserver;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.repository.empresa.EmpresaRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias — EmpresaService (GPE-150)
 *
 * Usa JUnit 5 + Mockito.
 * @Mock simula las dependencias sin tocar la BD real.
 * @InjectMocks crea EmpresaService con los mocks inyectados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GPE-150 — Pruebas EmpresaService")
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private VacanteRepository vacanteRepository;

    @Mock
    private EmpresaValidator validator;

    @Mock
    private Dev3Mapper mapper;

    @Mock
    private EmpresaObserver observer;

    @InjectMocks
    private EmpresaService empresaService;

    private Empresa empresaPendiente;
    private EmpresaResponse empresaResponseMock;

    @BeforeEach
    void setUp() {
        // Empresa de prueba en estado PENDIENTE
        empresaPendiente = Empresa.builder()
                .id(1L)
                .razonSocial("TechCo S.A.")
                .nit("900.123.456-7")
                .sector("Tecnología")
                .direccion("Calle 10 # 5-20")
                .municipio("Armenia")
                .telefono("3001234567")
                .nombreContacto("Juan Pérez")
                .correo("contacto@techco.com")
                .estado(EstadoEmpresa.PENDIENTE)
                .build();

        empresaResponseMock = new EmpresaResponse(
                1L, "TechCo S.A.", "900.123.456-7", "Tecnología",
                "Calle 10 # 5-20", "Armenia", "3001234567",
                "Juan Pérez", "contacto@techco.com",
                EstadoEmpresa.PENDIENTE, List.of(), null
        );

        // Inyectar observers manualmente (Spring no está disponible en pruebas unitarias)
        empresaService = new EmpresaService(
                empresaRepository, validator, mapper, List.of(observer)
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — crearEmpresa (GPE-150)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Crear empresa exitosamente con datos válidos")
    void crearEmpresa_conDatosValidos_debeRetornarEmpresaCreada() {
        // ARRANGE
        CrearEmpresaRequest request = new CrearEmpresaRequest(
                "TechCo S.A.", "900.123.456-7", "Tecnología",
                "Calle 10", "Armenia", "3001234567",
                "Juan Pérez", "contacto@techco.com", List.of()
        );
        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresaPendiente);
        when(mapper.toEmpresaResponse(any())).thenReturn(empresaResponseMock);

        // ACT
        EmpresaResponse resultado = empresaService.crearEmpresa(request);

        // ASSERT
        assertNotNull(resultado);
        assertEquals("TechCo S.A.", resultado.razonSocial());
        assertEquals(EstadoEmpresa.PENDIENTE, resultado.estado());
        verify(validator).validarNitUnico("900.123.456-7");
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("❌ Crear empresa con NIT duplicado debe lanzar excepción")
    void crearEmpresa_conNitDuplicado_debeLanzarExcepcion() {
        // ARRANGE
        CrearEmpresaRequest request = new CrearEmpresaRequest(
                "Otra Empresa", "900.123.456-7", null,
                null, null, null, "Contacto", null, null
        );
        doThrow(new OperacionNoPermitidaException("Ya existe una empresa con NIT: 900.123.456-7"))
                .when(validator).validarNitUnico("900.123.456-7");

        // ACT & ASSERT
        OperacionNoPermitidaException ex = assertThrows(
                OperacionNoPermitidaException.class,
                () -> empresaService.crearEmpresa(request)
        );
        assertTrue(ex.getMessage().contains("900.123.456-7"));
        verify(empresaRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — aprobarEmpresa (GPE-150)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Aprobar empresa PENDIENTE debe cambiar estado a APROBADA")
    void aprobarEmpresa_estaPendiente_debeQuedarAprobada() {
        // ARRANGE
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaPendiente));
        when(empresaRepository.save(any())).thenReturn(empresaPendiente);
        EmpresaResponse responseAprobada = new EmpresaResponse(
                1L, "TechCo S.A.", "900.123.456-7", "Tecnología",
                null, null, null, null, null,
                EstadoEmpresa.APROBADA, List.of(), null
        );
        when(mapper.toEmpresaResponse(any())).thenReturn(responseAprobada);

        // ACT
        EmpresaResponse resultado = empresaService.aprobarEmpresa(1L);

        // ASSERT
        assertEquals(EstadoEmpresa.APROBADA, resultado.estado());
        verify(empresaRepository).save(any());
    }

    @Test
    @DisplayName("❌ Aprobar empresa que no existe debe lanzar RecursoNoEncontradoException")
    void aprobarEmpresa_noExiste_debeLanzarExcepcion() {
        // ARRANGE
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> empresaService.aprobarEmpresa(99L)
        );
    }

    @Test
    @DisplayName("❌ Aprobar empresa ya APROBADA debe lanzar OperacionNoPermitidaException")
    void aprobarEmpresa_yaAprobada_debeLanzarExcepcion() {
        // ARRANGE
        empresaPendiente.aprobar(); // la pasamos a APROBADA primero
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaPendiente));

        // ACT & ASSERT
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> empresaService.aprobarEmpresa(1L)
        );
        verify(empresaRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — rechazarEmpresa (GPE-150)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Rechazar empresa con motivo válido debe cambiar estado a RECHAZADA")
    void rechazarEmpresa_conMotivo_debeQuedarRechazada() {
        // ARRANGE
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaPendiente));
        when(empresaRepository.save(any())).thenReturn(empresaPendiente);
        EmpresaResponse responseRechazada = new EmpresaResponse(
                1L, "TechCo S.A.", "900.123.456-7", null,
                null, null, null, null, null,
                EstadoEmpresa.RECHAZADA, List.of(), null
        );
        when(mapper.toEmpresaResponse(any())).thenReturn(responseRechazada);

        // ACT
        EmpresaResponse resultado = empresaService.rechazarEmpresa(
                1L, new RechazarRequest("No cumple requisitos legales")
        );

        // ASSERT
        assertEquals(EstadoEmpresa.RECHAZADA, resultado.estado());
        verify(empresaRepository).save(any());
    }

    @Test
    @DisplayName("❌ Rechazar empresa sin motivo debe lanzar excepción")
    void rechazarEmpresa_sinMotivo_debeLanzarExcepcion() {
        // ARRANGE
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaPendiente));

        // ACT & ASSERT
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> empresaService.rechazarEmpresa(1L, new RechazarRequest(""))
        );
        verify(empresaRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — inactivarEmpresa (GPE-150)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Inactivar empresa sin vacantes activas debe funcionar")
    void inactivarEmpresa_sinVacantesActivas_debeQuedarInactiva() {
        // ARRANGE
        empresaPendiente.aprobar();
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaPendiente));
        when(empresaRepository.save(any())).thenReturn(empresaPendiente);
        when(mapper.toEmpresaResponse(any())).thenReturn(empresaResponseMock);

        // ACT
        empresaService.inactivarEmpresa(1L);

        // ASSERT
        verify(validator).validarSinVacantesActivas(1L);
        verify(empresaRepository).save(any());
    }

    @Test
    @DisplayName("❌ Inactivar empresa con vacantes activas debe lanzar excepción")
    void inactivarEmpresa_conVacantesActivas_debeLanzarExcepcion() {
        // ARRANGE
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaPendiente));
        doThrow(new OperacionNoPermitidaException("Tiene vacantes activas"))
                .when(validator).validarSinVacantesActivas(1L);

        // ACT & ASSERT
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> empresaService.inactivarEmpresa(1L)
        );
        verify(empresaRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — PATRÓN OBSERVER
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Al inactivar empresa el Observer debe ser notificado")
    void inactivarEmpresa_debeNotificarObservers() {
        // ARRANGE
        empresaPendiente.aprobar();
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaPendiente));
        when(empresaRepository.save(any())).thenReturn(empresaPendiente);
        when(mapper.toEmpresaResponse(any())).thenReturn(empresaResponseMock);

        // ACT
        empresaService.inactivarEmpresa(1L);

        // ASSERT — el observer fue notificado con el evento correcto
        verify(observer).onEmpresaEvento(1L, "EMPRESA_INACTIVADA");
    }
}
