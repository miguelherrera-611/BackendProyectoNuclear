package co.edu.cue.practicas.service;

import co.edu.cue.practicas.dto.request.CrearTutorRequest;
import co.edu.cue.practicas.dto.response.TutorEmpresarialResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.entity.TutorEmpresarial;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.repository.tutor.TutorEmpresarialRepository;
import co.edu.cue.practicas.service.empresa.EmpresaService;
import co.edu.cue.practicas.service.mapper.Dev3Mapper;
import co.edu.cue.practicas.service.tutor.TutorEmpresarialService;
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import co.edu.cue.practicas.service.validator.TutorEmpresarialValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias — TutorEmpresarialService (GPE-151)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GPE-151 — Pruebas TutorEmpresarialService")
class TutorEmpresarialServiceTest {

    @Mock private TutorEmpresarialRepository tutorRepository;
    @Mock private EmpresaService empresaService;
    @Mock private EmpresaValidator empresaValidator;
    @Mock private TutorEmpresarialValidator tutorValidator;
    @Mock private Dev3Mapper mapper;

    @InjectMocks
    private TutorEmpresarialService tutorService;

    private Empresa empresaAprobada;
    private TutorEmpresarial tutorActivo;
    private TutorEmpresarialResponse tutorResponseMock;

    @BeforeEach
    void setUp() {
        empresaAprobada = Empresa.builder()
                .id(1L)
                .razonSocial("TechCo S.A.")
                .nit("900.123.456-7")
                .estado(EstadoEmpresa.APROBADA)
                .build();

        tutorActivo = TutorEmpresarial.builder()
                .id(1L)
                .nombre("Laura Gómez")
                .cargo("Líder de Desarrollo")
                .correo("laura@techco.com")
                .telefono("3007654321")
                .empresa(empresaAprobada)
                .disponible(true)
                .activo(true)
                .build();

        tutorResponseMock = new TutorEmpresarialResponse(
                1L, "Laura Gómez", "Líder de Desarrollo",
                "laura@techco.com", "3007654321",
                1L, "TechCo S.A.", true, true, null
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — crearTutor (GPE-151)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Crear tutor en empresa APROBADA debe funcionar")
    void crearTutor_empresaAprobada_debeCrearTutor() {
        // ARRANGE
        CrearTutorRequest request = new CrearTutorRequest(
                "Laura Gómez", "Líder de Desarrollo",
                "laura@techco.com", "3007654321", 1L
        );
        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        when(tutorRepository.save(any())).thenReturn(tutorActivo);
        when(mapper.toTutorResponse(any())).thenReturn(tutorResponseMock);

        // ACT
        TutorEmpresarialResponse resultado = tutorService.crearTutor(request);

        // ASSERT
        assertNotNull(resultado);
        assertEquals("Laura Gómez", resultado.nombre());
        assertTrue(resultado.activo());
        verify(empresaValidator).validarEmpresaAprobadaParaTutores(empresaAprobada);
        verify(tutorValidator).validarCorreoUnico("laura@techco.com");
        verify(tutorRepository).save(any());
    }

    @Test
    @DisplayName("❌ Crear tutor en empresa NO APROBADA debe lanzar excepción")
    void crearTutor_empresaNoAprobada_debeLanzarExcepcion() {
        // ARRANGE
        CrearTutorRequest request = new CrearTutorRequest(
                "Laura", "Dev", "laura@techco.com", null, 1L
        );
        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        doThrow(new OperacionNoPermitidaException("Empresa no aprobada"))
                .when(empresaValidator).validarEmpresaAprobadaParaTutores(any());

        // ACT & ASSERT
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> tutorService.crearTutor(request)
        );
        verify(tutorRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Crear tutor con correo duplicado debe lanzar excepción")
    void crearTutor_correoExistente_debeLanzarExcepcion() {
        // ARRANGE
        CrearTutorRequest request = new CrearTutorRequest(
                "Otro Tutor", "Dev", "laura@techco.com", null, 1L
        );
        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        doThrow(new OperacionNoPermitidaException("Correo ya registrado"))
                .when(tutorValidator).validarCorreoUnico("laura@techco.com");

        // ACT & ASSERT
        assertThrows(
                OperacionNoPermitidaException.class,
                () -> tutorService.crearTutor(request)
        );
        verify(tutorRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — desactivarTutor (GPE-151)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Desactivar tutor activo debe quedar inactivo y no disponible")
    void desactivarTutor_estaActivo_debeQuedarInactivo() {
        // ARRANGE
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutorActivo));
        when(tutorRepository.save(any())).thenReturn(tutorActivo);
        TutorEmpresarialResponse responseInactivo = new TutorEmpresarialResponse(
                1L, "Laura Gómez", "Líder", "laura@techco.com",
                null, 1L, "TechCo", false, false, null
        );
        when(mapper.toTutorResponse(any())).thenReturn(responseInactivo);

        // ACT
        TutorEmpresarialResponse resultado = tutorService.desactivarTutor(1L);

        // ASSERT — OCL: PROHIBIDO eliminar, solo desactivar
        assertFalse(resultado.activo());
        assertFalse(resultado.disponible());
        assertFalse(tutorActivo.isActivo());
        assertFalse(tutorActivo.isDisponible());
        verify(tutorRepository).save(tutorActivo);
    }

    @Test
    @DisplayName("❌ Desactivar tutor que no existe debe lanzar excepción")
    void desactivarTutor_noExiste_debeLanzarExcepcion() {
        // ARRANGE
        when(tutorRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> tutorService.desactivarTutor(99L)
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRUEBAS — PATRÓN OBSERVER (inactivación en cascada)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Al desactivar el tutor activo y disponible quedan en false")
    void tutorDesactivar_debePonerActivoYDisponibleEnFalse() {
        // ARRANGE & ACT
        tutorActivo.desactivar();

        // ASSERT — verifica que el método desactivar() funciona correctamente
        // Este es el método que llama TutorInactivacionObserver automáticamente
        assertFalse(tutorActivo.isActivo());
        assertFalse(tutorActivo.isDisponible());
    }
}
