package co.edu.cue.practicas.service.tutor;

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
import co.edu.cue.practicas.service.validator.EmpresaValidator;
import co.edu.cue.practicas.service.validator.TutorEmpresarialValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TutorEmpresarialService — Pruebas unitarias (GPE-151)")
class TutorEmpresarialServiceTest {

    @Mock private TutorEmpresarialRepository tutorRepository;
    @Mock private EmpresaService empresaService;
    @Mock private EmpresaValidator empresaValidator;
    @Mock private TutorEmpresarialValidator tutorValidator;
    @Mock private Dev3Mapper mapper;

    @InjectMocks
    private TutorEmpresarialService service;

    private Empresa empresaAprobada;
    private TutorEmpresarial tutorEjemplo;
    private TutorEmpresarialResponse responseEjemplo;

    @BeforeEach
    void setUp() {
        empresaAprobada = Empresa.builder()
                .id(1L).razonSocial("TechCo").nit("900.1").nombreContacto("Juan")
                .estado(EstadoEmpresa.APROBADA).build();

        tutorEjemplo = TutorEmpresarial.builder()
                .id(1L).nombre("Laura Gómez").cargo("Líder Dev")
                .correo("laura@techco.com").telefono("300").empresa(empresaAprobada)
                .disponible(true).activo(true).build();

        responseEjemplo = new TutorEmpresarialResponse(
                1L, "Laura Gómez", "Líder Dev", "laura@techco.com",
                "300", 1L, "TechCo", true, true, null);
    }

    // =================================================================
    // crearTutor
    // =================================================================

    @Test
    @DisplayName("crearTutor exitoso debe persistir el tutor y retornar DTO")
    void crearTutorExitoso() {
        CrearTutorRequest req = new CrearTutorRequest("Laura Gómez", "Líder Dev",
                "laura@techco.com", "300", 1L);

        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        when(tutorRepository.save(any(TutorEmpresarial.class))).thenReturn(tutorEjemplo);
        when(mapper.toTutorResponse(any())).thenReturn(responseEjemplo);

        TutorEmpresarialResponse resultado = service.crearTutor(req);

        assertThat(resultado.getNombre()).isEqualTo("Laura Gómez");
        // OCL empresaActiva: debe validarse que la empresa esté APROBADA
        verify(empresaValidator).validarEmpresaAprobadaParaTutores(empresaAprobada);
        // OCL correoUnico: debe validarse que el correo no exista
        verify(tutorValidator).validarCorreoUnico("laura@techco.com");
        verify(tutorRepository).save(any(TutorEmpresarial.class));
    }

    @Test
    @DisplayName("crearTutor debe lanzar excepción si la empresa no está APROBADA")
    void crearTutorEmpresaNoAprobadaLanzaExcepcion() {
        CrearTutorRequest req = new CrearTutorRequest("Laura", "Dev", "l@t.com", "300", 1L);

        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        doThrow(new OperacionNoPermitidaException("Empresa no APROBADA"))
                .when(empresaValidator).validarEmpresaAprobadaParaTutores(empresaAprobada);

        assertThatThrownBy(() -> service.crearTutor(req))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(tutorRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearTutor debe lanzar excepción si el correo ya está registrado")
    void crearTutorCorreoDuplicadoLanzaExcepcion() {
        CrearTutorRequest req = new CrearTutorRequest("Laura", "Dev", "duplicado@t.com", "300", 1L);

        when(empresaService.buscarOFallar(1L)).thenReturn(empresaAprobada);
        doThrow(new OperacionNoPermitidaException("Correo duplicado"))
                .when(tutorValidator).validarCorreoUnico("duplicado@t.com");

        assertThatThrownBy(() -> service.crearTutor(req))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(tutorRepository, never()).save(any());
    }

    // =================================================================
    // desactivarTutor — OCL: PROHIBIDO eliminar, solo desactivar
    // =================================================================

    @Test
    @DisplayName("desactivarTutor debe marcar activo=false y disponible=false")
    void desactivarTutorExitoso() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutorEjemplo));
        when(tutorRepository.save(any())).thenReturn(tutorEjemplo);
        when(mapper.toTutorResponse(any())).thenReturn(responseEjemplo);

        service.desactivarTutor(1L);

        assertThat(tutorEjemplo.isActivo()).isFalse();
        assertThat(tutorEjemplo.isDisponible()).isFalse();
        verify(tutorRepository).save(tutorEjemplo);
    }

    @Test
    @DisplayName("desactivarTutor debe lanzar 404 si el tutor no existe")
    void desactivarNoEncontradoLanza404() {
        when(tutorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivarTutor(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // =================================================================
    // listarPorEmpresa
    // =================================================================

    @Test
    @DisplayName("listarPorEmpresa debe retornar la lista de la empresa filtrada")
    void listarPorEmpresaRetornaLista() {
        when(tutorRepository.findByEmpresaId(1L)).thenReturn(List.of(tutorEjemplo));
        when(mapper.toTutorResponse(tutorEjemplo)).thenReturn(responseEjemplo);

        List<TutorEmpresarialResponse> resultado = service.listarPorEmpresa(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Laura Gómez");
    }
}
