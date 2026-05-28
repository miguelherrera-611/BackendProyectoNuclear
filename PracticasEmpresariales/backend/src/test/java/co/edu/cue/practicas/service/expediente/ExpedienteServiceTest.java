package co.edu.cue.practicas.service.expediente;

import co.edu.cue.practicas.dto.request.MantenerNoAptoRequest;
import co.edu.cue.practicas.dto.request.SubirHojaDeVidaRequest;
import co.edu.cue.practicas.dto.response.ExpedienteResponse;
import co.edu.cue.practicas.dto.response.HojaDeVidaResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.EstadoHojaDeVida;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.pattern.builder.ExpedienteBuilder;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import co.edu.cue.practicas.repository.expediente.HojaDeVidaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpedienteService — Pruebas unitarias (GPE-146)")
class ExpedienteServiceTest {

    @Mock private ExpedienteEstudianteRepository expedienteRepository;
    @Mock private HojaDeVidaRepository hvRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ExpedienteBuilder expedienteBuilder;
    @Mock private EstudianteMapper mapper;

    @InjectMocks
    private ExpedienteService service;

    private Usuario estudiante;
    private ExpedienteEstudiante expediente;
    private CustomUserDetails coordinacion;

    @BeforeEach
    void setUp() {
        estudiante = Usuario.builder()
                .id(5L).nombre("Est. Test").correo("est@test.com")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        expediente = ExpedienteEstudiante.builder()
                .id(1L).estudiante(estudiante).build();

        Usuario coordUser = Usuario.builder().id(20L).nombre("Coord").correo("c@test.com")
                .passwordHash("h").rol(Rol.COORDINACION_ACADEMICA).activo(true).build();
        coordinacion = new CustomUserDetails(coordUser);
    }

    // =================================================================
    // obtenerExpediente
    // =================================================================

    @Test
    @DisplayName("obtenerExpediente debe retornar el DTO ensamblado por ExpedienteBuilder")
    void obtenerExpedienteExitoso() {
        ExpedienteResponse responseEsperado = ExpedienteResponse.builder()
                .expedienteId(1L).estudianteId(5L).build();

        when(expedienteRepository.findByEstudiante_Id(5L)).thenReturn(Optional.of(expediente));
        when(expedienteBuilder.construir(expediente)).thenReturn(responseEsperado);

        ExpedienteResponse resultado = service.obtenerExpediente(5L);

        assertThat(resultado.getExpedienteId()).isEqualTo(1L);
        // PATRÓN BUILDER: el builder debe haberse invocado para ensamblar la respuesta
        verify(expedienteBuilder).construir(expediente);
    }

    @Test
    @DisplayName("obtenerExpediente debe lanzar 404 si no existe expediente para el estudiante")
    void obtenerExpedienteNoEncontradoLanza404() {
        when(expedienteRepository.findByEstudiante_Id(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerExpediente(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // =================================================================
    // subirHojaDeVida — PROXY PROTECCIÓN: hvInmutableEnPractica
    // =================================================================

    @Test
    @DisplayName("subirHojaDeVida exitoso cuando no hay práctica EN_CURSO")
    void subirHvExitosoSinPracticaEnCurso() {
        SubirHojaDeVidaRequest req = new SubirHojaDeVidaRequest("hvs/est005_v2.pdf");

        // Expediente sin prácticas → no hay restricción
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(expedienteRepository.findByEstudiante_Id(5L)).thenReturn(Optional.of(expediente));
        when(hvRepository.countByEstudiante_Id(5L)).thenReturn(1); // versión anterior existe
        when(hvRepository.save(any(HojaDeVida.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toHojaDeVidaResponse(any())).thenReturn(HojaDeVidaResponse.builder().version(2).build());

        HojaDeVidaResponse resultado = service.subirHojaDeVida(5L, req);

        assertThat(resultado.getVersion()).isEqualTo(2);
        verify(hvRepository).save(any(HojaDeVida.class));
    }

    @Test
    @DisplayName("subirHojaDeVida debe lanzar excepción si hay práctica EN_CURSO — OCL hvInmutableEnPractica")
    void subirHvBloqueadaCuandoPracticaEnCurso() {
        // Añadimos una instancia EN_CURSO al expediente
        InstanciaPractica practicaEnCurso = InstanciaPractica.builder()
                .estado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO).build();
        practicaEnCurso.iniciar(); // → EN_CURSO
        expediente.agregarPractica(practicaEnCurso);

        SubirHojaDeVidaRequest req = new SubirHojaDeVidaRequest("hvs/nueva.pdf");

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(expedienteRepository.findByEstudiante_Id(5L)).thenReturn(Optional.of(expediente));

        assertThatThrownBy(() -> service.subirHojaDeVida(5L, req))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("EN_CURSO");

        verify(hvRepository, never()).save(any());
    }

    @Test
    @DisplayName("subirHojaDeVida debe asignar versión = historial + 1")
    void subirHvDebeIncrementarVersion() {
        SubirHojaDeVidaRequest req = new SubirHojaDeVidaRequest("hvs/v3.pdf");
        // Hay 2 versiones anteriores → la nueva debe ser la 3
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(expedienteRepository.findByEstudiante_Id(5L)).thenReturn(Optional.of(expediente));
        when(hvRepository.countByEstudiante_Id(5L)).thenReturn(2);
        when(hvRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toHojaDeVidaResponse(any())).thenReturn(HojaDeVidaResponse.builder().version(3).build());

        HojaDeVidaResponse resultado = service.subirHojaDeVida(5L, req);

        assertThat(resultado.getVersion()).isEqualTo(3);
    }

    // =================================================================
    // validarHojaDeVida
    // =================================================================

    @Test
    @DisplayName("validarHojaDeVida debe cambiar estado a VALIDA y guardar")
    void validarHvExitoso() {
        HojaDeVida hv = HojaDeVida.builder()
                .id(1L).estudiante(estudiante).version(1)
                .urlArchivo("hv.pdf").estado(EstadoHojaDeVida.PENDIENTE).build();

        when(hvRepository.findById(1L)).thenReturn(Optional.of(hv));
        when(hvRepository.save(any())).thenReturn(hv);
        when(mapper.toHojaDeVidaResponse(any())).thenReturn(
                HojaDeVidaResponse.builder().estado(EstadoHojaDeVida.VALIDA).build());

        HojaDeVidaResponse resultado = service.validarHojaDeVida(5L, 1L, coordinacion);

        assertThat(resultado.getEstado()).isEqualTo(EstadoHojaDeVida.VALIDA);
        assertThat(hv.getEstado()).isEqualTo(EstadoHojaDeVida.VALIDA);
        verify(hvRepository).save(hv);
    }

    // =================================================================
    // rechazarHojaDeVida
    // =================================================================

    @Test
    @DisplayName("rechazarHojaDeVida debe cambiar estado a RECHAZADA con motivo")
    void rechazarHvExitoso() {
        HojaDeVida hv = HojaDeVida.builder()
                .id(2L).estudiante(estudiante).version(1)
                .urlArchivo("hv.pdf").estado(EstadoHojaDeVida.PENDIENTE).build();

        MantenerNoAptoRequest req = new MantenerNoAptoRequest("Foto de perfil faltante");

        when(hvRepository.findById(2L)).thenReturn(Optional.of(hv));
        when(hvRepository.save(any())).thenReturn(hv);
        when(mapper.toHojaDeVidaResponse(any())).thenReturn(
                HojaDeVidaResponse.builder().estado(EstadoHojaDeVida.RECHAZADA).build());

        HojaDeVidaResponse resultado = service.rechazarHojaDeVida(5L, 2L, req, coordinacion);

        assertThat(resultado.getEstado()).isEqualTo(EstadoHojaDeVida.RECHAZADA);
        assertThat(hv.getEstado()).isEqualTo(EstadoHojaDeVida.RECHAZADA);
        assertThat(hv.getMotivoRechazo()).isEqualTo("Foto de perfil faltante");
    }
}
