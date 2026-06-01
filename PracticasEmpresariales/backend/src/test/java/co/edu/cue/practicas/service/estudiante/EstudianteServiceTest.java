package co.edu.cue.practicas.service.estudiante;

import co.edu.cue.practicas.dto.request.EnviarAlProcesoRequest;
import co.edu.cue.practicas.dto.request.MarcarAptoRequest;
import co.edu.cue.practicas.dto.request.MantenerNoAptoRequest;
import co.edu.cue.practicas.dto.response.UsuarioResponse;
import co.edu.cue.practicas.event.AptitudCambiadaEvent;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.pattern.chain.ContextoValidacion;
import co.edu.cue.practicas.pattern.chain.ValidadorAptitud;
import co.edu.cue.practicas.pattern.strategy.EstrategiaValidacion;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import co.edu.cue.practicas.repository.expediente.HojaDeVidaRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.catalogo.CatalogoPracticaService;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import co.edu.cue.practicas.service.validator.EstudianteValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de EstudianteService.
 *
 * EstudianteService extiende PlantillaValidacionAptitud (abstract) y recibe
 * EstrategiaValidacion en el constructor, por lo que no puede usarse @InjectMocks.
 * Se construye manualmente en @BeforeEach inyectando mocks explícitamente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EstudianteService — Pruebas unitarias (GPE-143, GPE-145, GPE-147)")
class EstudianteServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ExpedienteEstudianteRepository expedienteRepository;
    @Mock private InstanciaPracticaRepository instanciaRepository;
    @Mock private HojaDeVidaRepository hvRepository;
    @Mock private CatalogoPracticaService catalogoService;
    @Mock private EstudianteValidator validator;
    @Mock private EstudianteMapper mapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EstudianteService service;

    // Estrategia que siempre pasa (no lanza excepciones)
    private final EstrategiaValidacion estrategiaOk = () -> new ValidadorAptitud() {
        @Override protected void ejecutarValidacion(ContextoValidacion ctx) { }
    };

    // Estrategia que siempre falla
    private final EstrategiaValidacion estrategiaFalla = () -> new ValidadorAptitud() {
        @Override protected void ejecutarValidacion(ContextoValidacion ctx) {
            throw new OperacionNoPermitidaException("Falla intencional en test");
        }
    };

    private Usuario estudianteNoApto;
    private Usuario estudianteApto;
    private CatalogoPractica catalogo;
    private ExpedienteEstudiante expediente;
    private CustomUserDetails coordinacionAcademica;

    @BeforeEach
    void setUp() {
        // Construimos el servicio manualmente porque extiende clase abstracta
        service = new EstudianteService(estrategiaOk, usuarioRepository, expedienteRepository,
                instanciaRepository, hvRepository, catalogoService, validator, mapper, eventPublisher);

        Programa programa = Programa.builder().id(1L).nombre("Sistemas").build();

        estudianteNoApto = Usuario.builder()
                .id(10L).nombre("Ana García").correo("ana@cue.edu.co")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true)
                .estadoEstudiante(EstadoEstudiante.NO_APTO).programa(programa).build();

        estudianteApto = Usuario.builder()
                .id(11L).nombre("Juan López").correo("juan@cue.edu.co")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true)
                .estadoEstudiante(EstadoEstudiante.APTO).programa(programa).build();

        catalogo = CatalogoPractica.builder()
                .id(1L).programa(programa).numeroPractica(1).nombre("Práctica I")
                .materiaNucleo("PE").codigoMateria("PE-101").numCortes(3).duracionSemanas(16)
                .activo(true).build();

        expediente = ExpedienteEstudiante.builder()
                .id(1L).estudiante(estudianteNoApto).build();

        Usuario coordUser = Usuario.builder().id(20L).nombre("Coordinadora")
                .correo("coord@cue.edu.co").passwordHash("h")
                .rol(Rol.COORDINACION_ACADEMICA).activo(true).build();
        coordinacionAcademica = new CustomUserDetails(coordUser);
    }

    // =================================================================
    // marcarApto
    // =================================================================

    @Test
    @DisplayName("marcarApto exitoso debe cambiar estado a APTO, crear instancia de práctica y publicar evento")
    void marcarAptoExitoso() {
        MarcarAptoRequest req = new MarcarAptoRequest(1L);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(estudianteNoApto));
        when(catalogoService.buscarOFallar(1L)).thenReturn(catalogo);
        when(hvRepository.findTopByEstudiante_IdAndEstadoOrderByVersionDesc(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(expedienteRepository.findByEstudiante_Id(10L)).thenReturn(Optional.of(expediente));
        when(instanciaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(expedienteRepository.save(any())).thenReturn(expediente);
        when(usuarioRepository.save(any())).thenReturn(estudianteNoApto);

        service.marcarApto(10L, req, coordinacionAcademica);

        // El estado debe haber cambiado a APTO
        assertThat(estudianteNoApto.getEstadoEstudiante()).isEqualTo(EstadoEstudiante.APTO);
        // El motivo debe limpiarse
        assertThat(estudianteNoApto.getMotivoNoApto()).isNull();
        // El evento debe publicarse (PATRÓN OBSERVER)
        verify(eventPublisher).publishEvent(any(AptitudCambiadaEvent.class));
        // La instancia de práctica debe persistirse (PATRÓN PROTOTYPE)
        verify(instanciaRepository).save(any(InstanciaPractica.class));
    }

    @Test
    @DisplayName("marcarApto debe lanzar excepción si el expediente no existe")
    void marcarAptoSinExpedienteLanzaExcepcion() {
        MarcarAptoRequest req = new MarcarAptoRequest(1L);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(estudianteNoApto));
        when(catalogoService.buscarOFallar(1L)).thenReturn(catalogo);
        when(hvRepository.findTopByEstudiante_IdAndEstadoOrderByVersionDesc(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(expedienteRepository.findByEstudiante_Id(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarApto(10L, req, coordinacionAcademica))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Expediente");
    }

    @Test
    @DisplayName("marcarApto cuando la validación falla NO debe cambiar el estado del estudiante")
    void marcarAptoConValidacionFallidaNoDebeModificarEstado() {
        // Recreamos el servicio con estrategia que siempre falla
        service = new EstudianteService(estrategiaFalla, usuarioRepository, expedienteRepository,
                instanciaRepository, hvRepository, catalogoService, validator, mapper, eventPublisher);

        MarcarAptoRequest req = new MarcarAptoRequest(1L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(estudianteNoApto));
        when(catalogoService.buscarOFallar(1L)).thenReturn(catalogo);
        when(hvRepository.findTopByEstudiante_IdAndEstadoOrderByVersionDesc(anyLong(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarApto(10L, req, coordinacionAcademica))
                .isInstanceOf(OperacionNoPermitidaException.class);

        // El estado NO debe haberse modificado
        assertThat(estudianteNoApto.getEstadoEstudiante()).isEqualTo(EstadoEstudiante.NO_APTO);
        verify(usuarioRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("marcarApto debe lanzar 404 si el estudiante no existe")
    void marcarAptoEstudianteNoEncontradoLanza404() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarApto(999L, new MarcarAptoRequest(1L), coordinacionAcademica))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =================================================================
    // mantenerNoApto
    // =================================================================

    @Test
    @DisplayName("mantenerNoApto debe persistir el motivo en el estudiante")
    void mantenerNoAptoGuardaMotivo() {
        MantenerNoAptoRequest req = new MantenerNoAptoRequest("No cumple créditos mínimos");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(estudianteNoApto));
        when(usuarioRepository.save(any())).thenReturn(estudianteNoApto);

        service.mantenerNoApto(10L, req, coordinacionAcademica);

        assertThat(estudianteNoApto.getMotivoNoApto()).isEqualTo("No cumple créditos mínimos");
        verify(usuarioRepository).save(estudianteNoApto);
    }

    // =================================================================
    // enviarAlProceso
    // =================================================================

    @Test
    @DisplayName("enviarAlProceso debe marcar enviadoAlProceso=true y publicar evento para cada estudiante")
    void enviarAlProcesoExitoso() {
        EnviarAlProcesoRequest req = new EnviarAlProcesoRequest(List.of(11L));

        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(estudianteApto));
        when(usuarioRepository.saveAll(any())).thenReturn(List.of(estudianteApto));

        List<UsuarioResponse> resultado = service.enviarAlProceso(req, coordinacionAcademica);

        assertThat(estudianteApto.isEnviadoAlProceso()).isTrue();
        // PATRÓN OBSERVER: se debe publicar un evento por cada estudiante enviado
        verify(eventPublisher).publishEvent(any(AptitudCambiadaEvent.class));
    }

    @Test
    @DisplayName("enviarAlProceso debe llamar al validator para verificar que cada estudiante sea APTO")
    void enviarAlProcesoLlamaValidatorParaCadaEstudiante() {
        EnviarAlProcesoRequest req = new EnviarAlProcesoRequest(List.of(11L));

        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(estudianteApto));
        when(usuarioRepository.saveAll(any())).thenReturn(List.of(estudianteApto));

        service.enviarAlProceso(req, coordinacionAcademica);

        // Debe verificarse que el estudiante es APTO antes de enviarlo
        verify(validator).validarEsApto(estudianteApto);
    }
}
