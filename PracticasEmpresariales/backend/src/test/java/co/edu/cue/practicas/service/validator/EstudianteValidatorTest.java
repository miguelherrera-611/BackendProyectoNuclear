package co.edu.cue.practicas.service.validator;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.Usuario;
import co.edu.cue.practicas.model.enums.EstadoEstudiante;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstudianteValidator — Pruebas unitarias")
class EstudianteValidatorTest {

    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EstudianteValidator validator;

    private Usuario estudianteBase(EstadoEstudiante estado, boolean activo) {
        return Usuario.builder()
                .id(1L).nombre("Ana").correo("ana@cue.edu.co").passwordHash("h")
                .rol(Rol.ESTUDIANTE).activo(activo).estadoEstudiante(estado).build();
    }

    // ── validarIdentificacionUnica ───────────────────────────────────────

    @Test
    @DisplayName("validarIdentificacionUnica no lanza excepcion si la identificacion es null")
    void validarIdentificacionUnica_null_noLanzaExcepcion() {
        assertThatCode(() -> validator.validarIdentificacionUnica(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarIdentificacionUnica no lanza excepcion si la identificacion no existe")
    void validarIdentificacionUnica_libre_noLanzaExcepcion() {
        when(usuarioRepository.existsByIdentificacion("123")).thenReturn(false);

        assertThatCode(() -> validator.validarIdentificacionUnica("123")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarIdentificacionUnica lanza excepcion si la identificacion ya existe — OCL identificacionUnica")
    void validarIdentificacionUnica_duplicada_lanzaExcepcion() {
        when(usuarioRepository.existsByIdentificacion("123")).thenReturn(true);

        assertThatThrownBy(() -> validator.validarIdentificacionUnica("123"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("123");
    }

    // ── validarEsEstudianteActivo ─────────────────────────────────────────

    @Test
    @DisplayName("validarEsEstudianteActivo no lanza excepcion para estudiante activo")
    void validarEsEstudianteActivo_estudianteActivo_noLanzaExcepcion() {
        Usuario estudiante = estudianteBase(EstadoEstudiante.APTO, true);

        assertThatCode(() -> validator.validarEsEstudianteActivo(estudiante)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarEsEstudianteActivo lanza excepcion si el rol no es ESTUDIANTE")
    void validarEsEstudianteActivo_rolDistinto_lanzaExcepcion() {
        Usuario docente = Usuario.builder().id(2L).nombre("Doc").correo("d@cue.edu.co")
                .passwordHash("h").rol(Rol.DOCENTE_ASESOR).activo(true).build();

        assertThatThrownBy(() -> validator.validarEsEstudianteActivo(docente))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ESTUDIANTE");
    }

    @Test
    @DisplayName("validarEsEstudianteActivo lanza excepcion si el estudiante esta inactivo")
    void validarEsEstudianteActivo_estudianteInactivo_lanzaExcepcion() {
        Usuario estudiante = estudianteBase(EstadoEstudiante.APTO, false);

        assertThatThrownBy(() -> validator.validarEsEstudianteActivo(estudiante))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inactivo");
    }

    // ── validarEsApto ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validarEsApto no lanza excepcion si el estudiante esta APTO")
    void validarEsApto_apto_noLanzaExcepcion() {
        Usuario estudiante = estudianteBase(EstadoEstudiante.APTO, true);

        assertThatCode(() -> validator.validarEsApto(estudiante)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarEsApto lanza excepcion si el estudiante es NO_APTO — OCL soloAptoPostulable")
    void validarEsApto_noApto_lanzaExcepcion() {
        Usuario estudiante = estudianteBase(EstadoEstudiante.NO_APTO, true);

        assertThatThrownBy(() -> validator.validarEsApto(estudiante))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("APTO");
    }

    // ── validarTransicionApto ─────────────────────────────────────────────

    @Test
    @DisplayName("validarTransicionApto no lanza excepcion si el estudiante esta NO_APTO")
    void validarTransicionApto_noApto_noLanzaExcepcion() {
        Usuario estudiante = estudianteBase(EstadoEstudiante.NO_APTO, true);

        assertThatCode(() -> validator.validarTransicionApto(estudiante)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarTransicionApto lanza excepcion si el estudiante ya esta APTO — OCL transicionEstadoValida")
    void validarTransicionApto_yaApto_lanzaExcepcion() {
        Usuario estudiante = estudianteBase(EstadoEstudiante.APTO, true);

        assertThatThrownBy(() -> validator.validarTransicionApto(estudiante))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("APTO");
    }
}
