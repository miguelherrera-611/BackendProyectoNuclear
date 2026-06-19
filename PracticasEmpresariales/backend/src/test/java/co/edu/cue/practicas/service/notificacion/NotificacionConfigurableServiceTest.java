package co.edu.cue.practicas.service.notificacion;

import co.edu.cue.practicas.dto.request.PlantillaNotificacionRequest;
import co.edu.cue.practicas.dto.response.PlantillaNotificacionResponse;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.model.entity.BitacoraCorreo;
import co.edu.cue.practicas.model.entity.PlantillaNotificacion;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoEnvioCorreo;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.model.enums.TipoEventoNotificacion;
import co.edu.cue.practicas.repository.notificacion.BitacoraCorreoRepository;
import co.edu.cue.practicas.repository.notificacion.PlantillaNotificacionRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionConfigurableService — Pruebas unitarias")
class NotificacionConfigurableServiceTest {

    @Mock private PlantillaNotificacionRepository plantillaRepository;
    @Mock private BitacoraCorreoRepository bitacoraRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private NotificacionConfigurableService service;

    private CustomUserDetails coordinador;
    private CustomUserDetails noCoordinador;

    @BeforeEach
    void setUp() {
        coordinador = new CustomUserDetails(Usuario.builder()
                .id(1L).nombre("Coord").correo("coord@cue.edu.co")
                .passwordHash("h").rol(Rol.COORDINADOR_PRACTICAS).activo(true).build());
        noCoordinador = new CustomUserDetails(Usuario.builder()
                .id(2L).nombre("Doc").correo("doc@cue.edu.co")
                .passwordHash("h").rol(Rol.DOCENTE_ASESOR).activo(true).build());
    }

    // ── guardarPlantilla / eliminarPlantilla ──────────────────────────────

    @Test
    @DisplayName("guardarPlantilla exitoso crea una nueva plantilla cuando no existe una para ese evento")
    void guardarPlantillaExitoso() {
        PlantillaNotificacionRequest req = new PlantillaNotificacionRequest();
        req.setTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA);
        req.setAsunto("Asunto");
        req.setCuerpo("Cuerpo {{nombre_estudiante}}");
        req.setFrecuenciaRecordatorioDias(2);

        when(plantillaRepository.findByTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA))
                .thenReturn(Optional.empty());
        when(plantillaRepository.save(any(PlantillaNotificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        PlantillaNotificacionResponse resultado = service.guardarPlantilla(req, coordinador);

        assertThat(resultado.getAsunto()).isEqualTo("Asunto");
        assertThat(resultado.getFrecuenciaRecordatorioDias()).isEqualTo(2);
    }

    @Test
    @DisplayName("guardarPlantilla bloquea a roles distintos de COORDINADOR_PRACTICAS")
    void guardarPlantillaRolNoAutorizadoLanzaExcepcion() {
        PlantillaNotificacionRequest req = new PlantillaNotificacionRequest();

        assertThatThrownBy(() -> service.guardarPlantilla(req, noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verify(plantillaRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminarPlantilla bloquea a roles distintos de COORDINADOR_PRACTICAS")
    void eliminarPlantillaRolNoAutorizadoLanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminarPlantilla(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verify(plantillaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("eliminarPlantilla exitoso borra la plantilla existente")
    void eliminarPlantillaExitoso() {
        PlantillaNotificacion plantilla = PlantillaNotificacion.builder()
                .id(1L).tipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA).build();
        when(plantillaRepository.findByTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA))
                .thenReturn(Optional.of(plantilla));

        service.eliminarPlantilla(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, coordinador);

        verify(plantillaRepository).delete(plantilla);
    }

    // ── previsualizar ─────────────────────────────────────────────────────

    @Test
    @DisplayName("previsualizar reemplaza las variables {{clave}} del cuerpo")
    void previsualizarReemplazaVariables() {
        PlantillaNotificacionRequest req = new PlantillaNotificacionRequest();
        req.setCuerpo("Hola {{nombre_estudiante}}, tu practica es {{nombre_practica}}.");

        String resultado = service.previsualizar(req,
                Map.of("nombre_estudiante", "Ana", "nombre_practica", "Practica I"), coordinador);

        assertThat(resultado).isEqualTo("Hola Ana, tu practica es Practica I.");
    }

    @Test
    @DisplayName("previsualizar bloquea a roles distintos de COORDINADOR_PRACTICAS")
    void previsualizarRolNoAutorizadoLanzaExcepcion() {
        PlantillaNotificacionRequest req = new PlantillaNotificacionRequest();
        req.setCuerpo("Hola");

        assertThatThrownBy(() -> service.previsualizar(req, Map.of(), noCoordinador))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // ── enviar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("enviar exitoso registra la bitacora con estado ENVIADO")
    void enviarExitosoRegistraBitacoraEnviado() {
        when(plantillaRepository.findByTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA))
                .thenReturn(Optional.empty()); // usa la plantilla por defecto

        service.enviar(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, 5L, "tutor@corp.com", "Tutor", Map.of());

        ArgumentCaptor<BitacoraCorreo> captor = ArgumentCaptor.forClass(BitacoraCorreo.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoEnvioCorreo.ENVIADO);
        assertThat(captor.getValue().getDestinatario()).isEqualTo("tutor@corp.com");
    }

    @Test
    @DisplayName("enviar registra la bitacora con estado FALLIDO si EmailService lanza excepcion")
    void enviarConFalloRegistraBitacoraFallido() {
        when(plantillaRepository.findByTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("SMTP no disponible"))
                .when(emailService).notificarAsignacion(any(), any(), any(), any());

        service.enviar(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, 5L, "tutor@corp.com", "Tutor", Map.of());

        ArgumentCaptor<BitacoraCorreo> captor = ArgumentCaptor.forClass(BitacoraCorreo.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoEnvioCorreo.FALLIDO);
    }

    // ── puedeEnviarRecordatorio ───────────────────────────────────────────

    @Test
    @DisplayName("puedeEnviarRecordatorio(actorId) es false si ya se envio un correo hoy")
    void puedeEnviarRecordatorioSimple_yaEnviadoHoy_retornaFalse() {
        when(bitacoraRepository.existsByActorIdAndFechaEnvioBetween(eq(5L), any(), any())).thenReturn(true);

        assertThat(service.puedeEnviarRecordatorio(5L)).isFalse();
    }

    @Test
    @DisplayName("puedeEnviarRecordatorio sin recordatorio previo solo verifica que no se haya enviado hoy")
    void puedeEnviarRecordatorioConFrecuencia_sinUltimoRecordatorio_consultaSoloHoy() {
        when(plantillaRepository.findByTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA))
                .thenReturn(Optional.empty());
        when(bitacoraRepository.existsByActorIdAndFechaEnvioBetween(eq(5L), any(), any())).thenReturn(false);

        boolean resultado = service.puedeEnviarRecordatorio(5L, TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, null);

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("puedeEnviarRecordatorio respeta la frecuencia configurada cuando ya hubo un recordatorio previo")
    void puedeEnviarRecordatorioConFrecuencia_dentroDeLaVentana_retornaFalse() {
        PlantillaNotificacion plantilla = PlantillaNotificacion.builder()
                .tipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA)
                .frecuenciaRecordatorioDias(3).build();
        when(plantillaRepository.findByTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA))
                .thenReturn(Optional.of(plantilla));

        boolean resultado = service.puedeEnviarRecordatorio(
                5L, TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, LocalDate.now().minusDays(1));

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("puedeEnviarRecordatorio permite el envio una vez transcurrida la frecuencia configurada")
    void puedeEnviarRecordatorioConFrecuencia_fueraDeLaVentana_retornaTrue() {
        PlantillaNotificacion plantilla = PlantillaNotificacion.builder()
                .tipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA)
                .frecuenciaRecordatorioDias(3).build();
        when(plantillaRepository.findByTipoEvento(TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA))
                .thenReturn(Optional.of(plantilla));

        boolean resultado = service.puedeEnviarRecordatorio(
                5L, TipoEventoNotificacion.ENCUESTA_TUTOR_ENVIADA, LocalDate.now().minusDays(4));

        assertThat(resultado).isTrue();
    }
}
