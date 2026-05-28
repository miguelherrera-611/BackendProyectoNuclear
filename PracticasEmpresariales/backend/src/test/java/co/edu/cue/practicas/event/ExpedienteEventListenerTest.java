package co.edu.cue.practicas.event;

import co.edu.cue.practicas.model.entity.ExpedienteEstudiante;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.event.listener.ExpedienteEventListener;
import co.edu.cue.practicas.repository.expediente.ExpedienteEstudianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas del PATRÓN OBSERVER — ExpedienteEventListener.
 *
 * GPE-143: al crear un usuario ESTUDIANTE, el listener debe crear
 * automáticamente un expediente vacío sin intervención del UsuarioService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpedienteEventListener — Patrón Observer: creación automática de expediente")
class ExpedienteEventListenerTest {

    @Mock private ExpedienteEstudianteRepository expedienteRepository;

    @InjectMocks
    private ExpedienteEventListener listener;

    private Usuario estudianteNuevo;
    private Usuario otroRolNuevo;

    @BeforeEach
    void setUp() {
        estudianteNuevo = Usuario.builder()
                .id(10L).nombre("Ana Prueba").correo("ana@cue.edu.co")
                .passwordHash("h").rol(Rol.ESTUDIANTE).activo(true).build();

        otroRolNuevo = Usuario.builder()
                .id(11L).nombre("Admin DTI").correo("dti@cue.edu.co")
                .passwordHash("h").rol(Rol.ADMIN_DTI).activo(true).build();
    }

    @Test
    @DisplayName("debe crear expediente vacío automáticamente al registrar un ESTUDIANTE")
    void creaExpedienteCuandoEstudianteCreado() {
        // ARRANGE — simulamos que el expediente no existe aún
        when(expedienteRepository.existsByEstudiante_Id(10L)).thenReturn(false);

        UsuarioCreadoEvent evento = new UsuarioCreadoEvent(this, estudianteNuevo, "tempPass123");

        // ACT
        listener.onEstudianteCreado(evento);

        // ASSERT — debe haberse persistido un nuevo expediente
        ArgumentCaptor<ExpedienteEstudiante> captor =
                ArgumentCaptor.forClass(ExpedienteEstudiante.class);
        verify(expedienteRepository).save(captor.capture());

        ExpedienteEstudiante expedienteGuardado = captor.getValue();
        assertThat(expedienteGuardado.getEstudiante()).isEqualTo(estudianteNuevo);
        assertThat(expedienteGuardado.getPracticas()).isEmpty();
        assertThat(expedienteGuardado.getHistorialHv()).isEmpty();
        assertThat(expedienteGuardado.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("NO debe crear expediente si el usuario no es ESTUDIANTE")
    void noDebeCrearExpedienteParaRolNoEstudiante() {
        UsuarioCreadoEvent evento = new UsuarioCreadoEvent(this, otroRolNuevo, "pass");

        listener.onEstudianteCreado(evento);

        verify(expedienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("NO debe crear expediente duplicado si ya existe uno para el estudiante")
    void noDebeCrearExpedienteSiYaExiste() {
        // ARRANGE — el expediente ya fue creado antes
        when(expedienteRepository.existsByEstudiante_Id(10L)).thenReturn(true);

        UsuarioCreadoEvent evento = new UsuarioCreadoEvent(this, estudianteNuevo, "pass");

        // ACT
        listener.onEstudianteCreado(evento);

        // ASSERT — no debe crear otro
        verify(expedienteRepository, never()).save(any());
    }
}
