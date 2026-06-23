package co.edu.cue.practicas.service.programa;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.dto.request.CrearProgramaRequest;
import co.edu.cue.practicas.dto.response.ProgramaResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.Facultad;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.facultad.FacultadRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@DisplayName("ProgramaService — Pruebas unitarias")
class ProgramaServiceTest {

    @Mock private ProgramaRepository programaRepository;
    @Mock private FacultadRepository facultadRepository;
    @Mock private AuditoriaLogger auditoriaLogger;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ProgramaService service;

    private CustomUserDetails dti;
    private Facultad facultadActiva;

    @BeforeEach
    void setUp() {
        dti = new CustomUserDetails(Usuario.builder()
                .id(1L).nombre("DTI Test").correo("dti@cue.edu.co")
                .passwordHash("h").rol(Rol.ADMIN_DTI).activo(true).build());

        facultadActiva = Facultad.builder().id(10L).nombre("Ingenieria").activa(true).build();
    }

    // ── crearPrograma ─────────────────────────────────────────────────────

    @Test
    @DisplayName("crearPrograma exitoso debe usar el Builder y persistir el programa")
    void crearProgramaExitoso() {
        CrearProgramaRequest req = new CrearProgramaRequest();
        req.setNombre("Ing. Sistemas");
        req.setFacultadId(10L);
        req.setNumeroTotalPracticas(3);

        when(facultadRepository.findById(10L)).thenReturn(Optional.of(facultadActiva));
        when(programaRepository.existsByNombreIgnoreCaseAndFacultad_Id("Ing. Sistemas", 10L)).thenReturn(false);
        when(programaRepository.save(any(Programa.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgramaResponse resultado = service.crearPrograma(req, dti);

        assertThat(resultado.getNombre()).isEqualTo("Ing. Sistemas");
        assertThat(resultado.getFacultadId()).isEqualTo(10L);
        verify(programaRepository).save(any(Programa.class));
    }

    @Test
    @DisplayName("crearPrograma con facultad inexistente debe lanzar 404")
    void crearProgramaFacultadNoExisteLanza404() {
        CrearProgramaRequest req = new CrearProgramaRequest();
        req.setNombre("Ing. Sistemas");
        req.setFacultadId(99L);

        when(facultadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearPrograma(req, dti))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearPrograma con nombre duplicado en la misma facultad debe lanzar excepcion")
    void crearProgramaNombreDuplicadoLanzaExcepcion() {
        CrearProgramaRequest req = new CrearProgramaRequest();
        req.setNombre("Ing. Sistemas");
        req.setFacultadId(10L);

        when(facultadRepository.findById(10L)).thenReturn(Optional.of(facultadActiva));
        when(programaRepository.existsByNombreIgnoreCaseAndFacultad_Id("Ing. Sistemas", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.crearPrograma(req, dti))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Ya existe");

        verify(programaRepository, never()).save(any());
    }

    // ── editarPrograma ────────────────────────────────────────────────────

    @Test
    @DisplayName("editarPrograma exitoso debe persistir los nuevos datos")
    void editarProgramaExitoso() {
        Programa programaExistente = Programa.builder()
                .id(1L).nombre("Nombre Antiguo").facultad(facultadActiva)
                .numeroTotalPracticas(1).promedioMinimoGeneral(3.0).build();

        co.edu.cue.practicas.dto.request.EditarProgramaRequest req =
                new co.edu.cue.practicas.dto.request.EditarProgramaRequest();
        req.setNombre("Nombre Actualizado");
        req.setDescripcion("Nueva descripción");
        req.setNumeroTotalPracticas(2);
        req.setPromedioMinimoGeneral(3.5);

        when(programaRepository.findById(1L)).thenReturn(Optional.of(programaExistente));
        when(programaRepository.existsByNombreIgnoreCaseAndFacultad_IdAndIdNot("Nombre Actualizado", 10L, 1L))
                .thenReturn(false);
        when(programaRepository.save(any())).thenReturn(programaExistente);

        ProgramaResponse resultado = service.editarPrograma(1L, req, dti);

        assertThat(resultado.getNombre()).isEqualTo("Nombre Actualizado");
        assertThat(programaExistente.getNumeroTotalPracticas()).isEqualTo(2);
        verify(programaRepository).save(programaExistente);
    }

    @Test
    @DisplayName("editarPrograma con nombre duplicado en la misma facultad debe lanzar excepcion")
    void editarProgramaNombreDuplicadoLanzaExcepcion() {
        Programa programaExistente = Programa.builder()
                .id(1L).nombre("Nombre Antiguo").facultad(facultadActiva).build();

        co.edu.cue.practicas.dto.request.EditarProgramaRequest req =
                new co.edu.cue.practicas.dto.request.EditarProgramaRequest();
        req.setNombre("Ya Existe");

        when(programaRepository.findById(1L)).thenReturn(Optional.of(programaExistente));
        when(programaRepository.existsByNombreIgnoreCaseAndFacultad_IdAndIdNot("Ya Existe", 10L, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.editarPrograma(1L, req, dti))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Ya existe");

        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("editarPrograma con id inexistente debe lanzar 404")
    void editarProgramaNoExisteLanza404() {
        co.edu.cue.practicas.dto.request.EditarProgramaRequest req =
                new co.edu.cue.practicas.dto.request.EditarProgramaRequest();
        req.setNombre("Cualquiera");

        when(programaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editarPrograma(99L, req, dti))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(programaRepository, never()).save(any());
    }

    // ── activarPrograma / desactivarPrograma ─────────────────────────────

    @Test
    @DisplayName("activarPrograma exitoso cuando la facultad esta activa")
    void activarProgramaExitoso() {
        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas")
                .facultad(facultadActiva).activo(false).build();
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));

        service.activarPrograma(1L, dti);

        assertThat(programa.isActivo()).isTrue();
        verify(programaRepository).save(programa);
    }

    @Test
    @DisplayName("activarPrograma debe lanzar excepcion si la facultad del programa esta inactiva")
    void activarProgramaConFacultadInactivaLanzaExcepcion() {
        Facultad facultadInactiva = Facultad.builder().id(10L).nombre("Ingenieria").activa(false).build();
        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas")
                .facultad(facultadInactiva).activo(false).build();
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));

        assertThatThrownBy(() -> service.activarPrograma(1L, dti))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inactiva");

        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivarPrograma exitoso debe marcar activo=false sin eliminar el registro")
    void desactivarProgramaExitoso() {
        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas")
                .facultad(facultadActiva).activo(true).build();
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));

        service.desactivarPrograma(1L, dti);

        assertThat(programa.isActivo()).isFalse();
        verify(programaRepository).save(programa);
    }

    @Test
    @DisplayName("desactivarPrograma con id inexistente debe lanzar 404")
    void desactivarProgramaNoExisteLanza404() {
        when(programaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivarPrograma(99L, dti))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ── obtenerPorId / listarPorFacultad ──────────────────────────────────

    @Test
    @DisplayName("obtenerPorId retorna el DTO del programa encontrado")
    void obtenerPorIdExitoso() {
        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas").facultad(facultadActiva).build();
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));

        ProgramaResponse resultado = service.obtenerPorId(1L);

        assertThat(resultado.getNombre()).isEqualTo("Ing. Sistemas");
    }

    @Test
    @DisplayName("obtenerPorId con id inexistente debe lanzar 404")
    void obtenerPorIdNoExisteLanza404() {
        when(programaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("listarPorFacultad debe delegar en el repositorio filtrando por facultadId")
    void listarPorFacultadExitoso() {
        Programa programa = Programa.builder().id(1L).nombre("Ing. Sistemas").facultad(facultadActiva).build();
        when(programaRepository.findByFacultad_IdAndActivoTrue(10L)).thenReturn(java.util.List.of(programa));

        var resultado = service.listarPorFacultad(10L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Ing. Sistemas");
    }
}
