package co.edu.cue.practicas.service.facultad;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearFacultadRequest;
import co.edu.cue.practicas.dto.response.FacultadResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.BitacoraAuditoria;
import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.facultad.FacultadRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de FacultadService.
 *
 * @Mock crea versiones falsas de cada dependencia (repositorios, AuditoriaLogger).
 * @InjectMocks crea el FacultadService real con los mocks inyectados.
 *
 * Los métodos tienen @RequiereRol pero el Aspecto AOP NO se activa sin Spring,
 * así que probamos la lógica de negocio directamente sin interferencia del Proxy.
 *
 * Cómo ejecutar en IntelliJ:
 *   Clic derecho sobre la clase → Run 'FacultadServiceTest'
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FacultadService — Pruebas unitarias del CRUD de facultades")
class FacultadServiceTest {

    @Mock private FacultadRepository facultadRepository;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private FacultadService facultadService;

    // Usuario DTI que firma las operaciones en la bitácora
    private CustomUserDetails creador;

    @BeforeEach
    void setUp() {
        Usuario dti = Usuario.builder()
                .id(1L)
                .nombre("Admin DTI")
                .correo("dti@test.com")
                .passwordHash("hash")
                .rol(Rol.ADMIN_DTI)
                .activo(true)
                .build();
        creador = new CustomUserDetails(dti);
    }

    // =================================================================
    // crearFacultad
    // =================================================================

    @Test
    @DisplayName("crearFacultad exitoso debe persistir la entidad y registrar en bitácora")
    void crearFacultadExitoso() {
        // ARRANGE
        CrearFacultadRequest request = new CrearFacultadRequest();
        request.setNombre("Facultad de Ingeniería");
        request.setDescripcion("Descripción de prueba");

        Facultad facultadGuardada = Facultad.builder()
                .id(1L)
                .nombre("Facultad de Ingeniería")
                .descripcion("Descripción de prueba")
                .activa(true)
                .build();

        when(facultadRepository.existsByNombreIgnoreCase("Facultad de Ingeniería")).thenReturn(false);
        when(facultadRepository.save(any(Facultad.class))).thenReturn(facultadGuardada);

        // ACT
        FacultadResponse response = facultadService.crearFacultad(request, creador);

        // ASSERT
        assertThat(response.getNombre()).isEqualTo("Facultad de Ingeniería");
        assertThat(response.isActiva()).isTrue();
        verify(facultadRepository, times(1)).save(any(Facultad.class));
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    @Test
    @DisplayName("crearFacultad debe lanzar excepción si ya existe una facultad con ese nombre")
    void crearFacultadNombreDuplicadoLanzaExcepcion() {
        // ARRANGE
        CrearFacultadRequest request = new CrearFacultadRequest();
        request.setNombre("Facultad Existente");

        when(facultadRepository.existsByNombreIgnoreCase("Facultad Existente")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> facultadService.crearFacultad(request, creador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("nombre");

        // No debe haber llegado a persistir nada
        verify(facultadRepository, never()).save(any());
    }

    // =================================================================
    // editarFacultad
    // =================================================================

    @Test
    @DisplayName("editarFacultad exitoso debe persistir los nuevos datos")
    void editarFacultadExitoso() {
        // ARRANGE
        Facultad facultadExistente = Facultad.builder()
                .id(1L)
                .nombre("Nombre Antiguo")
                .activa(true)
                .build();

        CrearFacultadRequest request = new CrearFacultadRequest();
        request.setNombre("Nombre Actualizado");
        request.setDescripcion("Nueva descripción");

        when(facultadRepository.findById(1L)).thenReturn(Optional.of(facultadExistente));
        when(facultadRepository.save(any())).thenReturn(facultadExistente);

        // ACT
        FacultadResponse response = facultadService.editarFacultad(1L, request, creador);

        // ASSERT
        assertThat(response.getNombre()).isEqualTo("Nombre Actualizado");
        verify(facultadRepository, times(1)).save(facultadExistente);
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    // =================================================================
    // desactivarFacultad
    // =================================================================

    @Test
    @DisplayName("desactivarFacultad debe lanzar excepción si la facultad tiene programas activos")
    void desactivarFacultadConProgramasActivosLanzaExcepcion() {
        // ARRANGE — Programa usa @Builder.Default activo=true, así que basta con construirlo sin parámetros extra
        Programa programaActivo = Programa.builder()
                .id(1L)
                .nombre("Ingeniería de Sistemas")
                .build(); // activo=true por @Builder.Default

        List<Programa> programas = new ArrayList<>();
        programas.add(programaActivo);

        Facultad facultadConProgramas = Facultad.builder()
                .id(1L)
                .nombre("Facultad Test")
                .activa(true)
                .programas(programas) // lista explícita — override del @Builder.Default
                .build();

        when(facultadRepository.findById(1L)).thenReturn(Optional.of(facultadConProgramas));

        // ACT + ASSERT
        assertThatThrownBy(() -> facultadService.desactivarFacultad(1L, creador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("programas activos");

        verify(facultadRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivarFacultad exitoso cuando la facultad no tiene programas activos")
    void desactivarFacultadSinProgramasActivosExitoso() {
        // ARRANGE — lista de programas vacía (valor por defecto del builder)
        Facultad facultadSinProgramas = Facultad.builder()
                .id(1L)
                .nombre("Facultad Sin Programas")
                .activa(true)
                .build(); // programas = [] por @Builder.Default

        when(facultadRepository.findById(1L)).thenReturn(Optional.of(facultadSinProgramas));
        when(facultadRepository.save(any())).thenReturn(facultadSinProgramas);

        // ACT — no debe lanzar excepción
        assertThatCode(() -> facultadService.desactivarFacultad(1L, creador))
                .doesNotThrowAnyException();

        // La entidad debe haber quedado inactiva
        assertThat(facultadSinProgramas.isActiva()).isFalse();
        verify(facultadRepository, times(1)).save(facultadSinProgramas);
        verify(auditoriaLogger, times(1)).registrar(any(BitacoraAuditoria.BitacoraAuditoriaBuilder.class));
    }

    // =================================================================
    // obtenerPorId
    // =================================================================

    @Test
    @DisplayName("obtenerPorId debe lanzar RecursoNoEncontradoException si la facultad no existe")
    void obtenerPorIdNoEncontradoLanza404() {
        when(facultadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultadService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
