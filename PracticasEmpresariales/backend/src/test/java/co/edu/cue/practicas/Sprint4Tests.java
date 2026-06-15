package co.edu.cue.practicas;

import co.edu.cue.practicas.dto.request.*;
import co.edu.cue.practicas.dto.response.*;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.repository.cierre.PazYSalvoRepository;
import co.edu.cue.practicas.repository.cierre.SustentacionPracticaRepository;
import co.edu.cue.practicas.repository.documento.PracticaDocumentoRepository;
import co.edu.cue.practicas.repository.encuesta.EncuestaSatisfaccionRepository;
import co.edu.cue.practicas.repository.evaluacion.EvaluacionFinalRepository;
import co.edu.cue.practicas.repository.evaluacion.NotaFinalCoordinadorRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.programa.ProgramaConfiguracionRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.repository.usuario.UsuarioRepository;
import co.edu.cue.practicas.repository.empresa.EmpresaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import co.edu.cue.practicas.service.cierre.*;
import co.edu.cue.practicas.service.configuracion.ProgramaConfiguracionService;
import co.edu.cue.practicas.service.encuesta.EncuestaSatisfaccionService;
import co.edu.cue.practicas.service.evaluacion.*;
import co.edu.cue.practicas.service.notificacion.NotificacionConfigurableService;
import co.edu.cue.practicas.service.reporte.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Sprint4Tests {

    // ─── Utilidades comunes ───────────────────────────────────────

    private CustomUserDetails actor(Rol rol, Long programaId) {
        CustomUserDetails actor = mock(CustomUserDetails.class);
        lenient().when(actor.getRol()).thenReturn(rol);
        lenient().when(actor.getProgramaId()).thenReturn(programaId);
        return actor;
    }

    private InstanciaPractica instanciaEnCurso(Long instanciaId, Long programaId) {
        Programa programa = Programa.builder().id(programaId).nombre("Ingenieria").build();
        Usuario estudiante = Usuario.builder().id(1L).nombre("Estudiante Test")
                .correo("est@cue.edu.co").programa(programa).build();
        ExpedienteEstudiante expediente = ExpedienteEstudiante.builder()
                .id(1L).estudiante(estudiante).build();
        return InstanciaPractica.builder()
                .id(instanciaId)
                .expediente(expediente)
                .estado(EstadoPractica.EN_CURSO)
                .nombre("Practica I")
                .numeroPractica(1)
                .build();
    }

    private List<CriterioEvaluacion> criteriosValidos() {
        return List.of(
                CriterioEvaluacion.builder().nombre("Tecnico").peso(0.4).puntaje(4.5).build(),
                CriterioEvaluacion.builder().nombre("Equipo").peso(0.3).puntaje(4.0).build(),
                CriterioEvaluacion.builder().nombre("Responsabilidad").peso(0.3).puntaje(4.2).build()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-173 | EvaluacionFinal — entidad de dominio
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-173 | EvaluacionFinal — entidad de dominio")
    class EvaluacionFinalEntityTests {

        @Test
        @DisplayName("Calcula promedio automaticamente al completar con criterios validos")
        void completar_criteriosValidos_calculaPromedioAutomatico() {
            EvaluacionFinal eval = EvaluacionFinal.builder()
                    .instanciaPractica(instanciaEnCurso(1L, 10L))
                    .tipo(TipoEvaluacionFinal.DOCENTE_ASESOR)
                    .evaluadorId(5L).evaluadorNombre("Docente").build();

            eval.completar(criteriosValidos(), "Buen desempeno");

            assertThat(eval.getPromedioFinal()).isEqualTo(4.26);
            assertThat(eval.getEstado()).isEqualTo(EstadoEvaluacionFinal.COMPLETADA);
            assertThat(eval.getFecha()).isNotNull();
        }

        @Test
        @DisplayName("Lanza excepcion cuando lista de criterios esta vacia")
        void completar_sinCriterios_lanzaExcepcion() {
            EvaluacionFinal eval = EvaluacionFinal.builder()
                    .instanciaPractica(instanciaEnCurso(1L, 10L))
                    .tipo(TipoEvaluacionFinal.DOCENTE_ASESOR)
                    .evaluadorId(5L).evaluadorNombre("Docente").build();
            List<CriterioEvaluacion> vacio = List.of();

            assertThatThrownBy(() -> eval.completar(vacio, null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Lanza excepcion cuando la suma de pesos no es 1.0")
        void completar_pesosInvalidos_lanzaExcepcion() {
            EvaluacionFinal eval = EvaluacionFinal.builder()
                    .instanciaPractica(instanciaEnCurso(1L, 10L))
                    .tipo(TipoEvaluacionFinal.DOCENTE_ASESOR)
                    .evaluadorId(5L).evaluadorNombre("Docente").build();
            List<CriterioEvaluacion> malos = List.of(
                    CriterioEvaluacion.builder().nombre("A").peso(0.5).puntaje(3.0).build()
            );

            assertThatThrownBy(() -> eval.completar(malos, null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Lanza excepcion cuando un puntaje esta fuera del rango 0.0-5.0")
        void completar_puntajeFueraDeRango_lanzaExcepcion() {
            EvaluacionFinal eval = EvaluacionFinal.builder()
                    .instanciaPractica(instanciaEnCurso(1L, 10L))
                    .tipo(TipoEvaluacionFinal.DOCENTE_ASESOR)
                    .evaluadorId(5L).evaluadorNombre("Docente").build();
            List<CriterioEvaluacion> malos = List.of(
                    CriterioEvaluacion.builder().nombre("A").peso(0.5).puntaje(6.0).build(),
                    CriterioEvaluacion.builder().nombre("B").peso(0.5).puntaje(3.0).build()
            );

            assertThatThrownBy(() -> eval.completar(malos, null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Proxy: lanza excepcion al modificar evaluacion de practica cerrada")
        void completar_practicaFinalizada_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);
            EvaluacionFinal eval = EvaluacionFinal.builder()
                    .instanciaPractica(instancia)
                    .tipo(TipoEvaluacionFinal.DOCENTE_ASESOR)
                    .evaluadorId(5L).evaluadorNombre("Docente").build();
            List<CriterioEvaluacion> criterios = criteriosValidos();

            assertThatThrownBy(() -> eval.completar(criterios, null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-173/174 | EvaluacionDocenteService / EvaluacionTutorService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-173/174 | EvaluacionDocenteService / EvaluacionTutorService")
    @ExtendWith(MockitoExtension.class)
    class EvaluacionServicesTests {

        @Mock private EvaluacionFinalRepository evaluacionRepo;
        @Mock private InstanciaPracticaRepository instanciaRepo;
        @Mock private ApplicationEventPublisher eventPublisher;
        @InjectMocks private EvaluacionDocenteService docenteService;
        @InjectMocks private EvaluacionTutorService tutorService;

        @Test
        @DisplayName("Docente asesor NO asignado a la practica lanza acceso denegado")
        void docente_noAsignado_lanzaAccesoNoAutorizado() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.setDocenteAsesor(Usuario.builder().id(99L).build());
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
            when(actor.getId()).thenReturn(5L);

            RegistrarEvaluacionFinalRequest req = new RegistrarEvaluacionFinalRequest();
            req.setCriterios(List.of());

            assertThatThrownBy(() -> docenteService.registrar(1L, req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Tutor empresarial NO asignado a la practica lanza acceso denegado")
        void tutor_noAsignado_lanzaAccesoNoAutorizado() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.setTutorEmpresarial(
                    Usuario.builder().id(1L).correo("tutor@empresa.com").passwordHash("h").rol(Rol.TUTOR_EMPRESARIAL).build());
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.TUTOR_EMPRESARIAL, null);
            when(actor.getUsername()).thenReturn("otro@empresa.com");

            RegistrarEvaluacionFinalRequest req = new RegistrarEvaluacionFinalRequest();
            req.setCriterios(List.of());

            assertThatThrownBy(() -> tutorService.registrar(1L, req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Practica no EN_CURSO no permite registrar evaluacion")
        void evaluacion_practicaNoEnCurso_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
            RegistrarEvaluacionFinalRequest req = new RegistrarEvaluacionFinalRequest();
            req.setCriterios(List.of());

            assertThatThrownBy(() -> docenteService.registrar(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Practica no encontrada lanza RecursoNoEncontrado")
        void evaluacion_practicaNoExiste_lanzaRecursoNoEncontrado() {
            when(instanciaRepo.findById(99L)).thenReturn(Optional.empty());
            CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
            RegistrarEvaluacionFinalRequest req = new RegistrarEvaluacionFinalRequest();

            assertThatThrownBy(() -> docenteService.registrar(99L, req, actor))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-175 | NotaFinalCoordinador
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-175 | NotaFinalCoordinador")
    class NotaFinalCoordinadorTests {

        @Test
        @DisplayName("Nota >= minima produce resultado APROBADO")
        void actualizar_notaSuficiente_resultadoAprobado() {
            NotaFinalCoordinador nota = NotaFinalCoordinador.builder()
                    .instanciaPractica(instanciaEnCurso(1L, 10L))
                    .notaFinal(0).notaMinimaAplicada(0)
                    .resultado(ResultadoPractica.NO_APROBADO).build();

            nota.actualizar(4.0, 3.0, "Aprobado");

            assertThat(nota.getResultado()).isEqualTo(ResultadoPractica.APROBADO);
        }

        @Test
        @DisplayName("Nota < minima produce resultado NO_APROBADO")
        void actualizar_notaInsuficiente_resultadoNoAprobado() {
            NotaFinalCoordinador nota = NotaFinalCoordinador.builder()
                    .instanciaPractica(instanciaEnCurso(1L, 10L))
                    .notaFinal(0).notaMinimaAplicada(0)
                    .resultado(ResultadoPractica.APROBADO).build();

            nota.actualizar(2.5, 3.0, null);

            assertThat(nota.getResultado()).isEqualTo(ResultadoPractica.NO_APROBADO);
        }

        @Test
        @DisplayName("Nota fuera de rango 0-5 lanza excepcion")
        void actualizar_notaFueraDeRango_lanzaExcepcion() {
            NotaFinalCoordinador nota = NotaFinalCoordinador.builder()
                    .instanciaPractica(instanciaEnCurso(1L, 10L))
                    .notaFinal(0).notaMinimaAplicada(0)
                    .resultado(ResultadoPractica.NO_APROBADO).build();

            assertThatThrownBy(() -> nota.actualizar(5.5, 3.0, null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Proxy: nota es inmutable despues del cierre")
        void actualizar_practicaCerrada_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);
            NotaFinalCoordinador nota = NotaFinalCoordinador.builder()
                    .instanciaPractica(instancia)
                    .notaFinal(4.0).notaMinimaAplicada(3.0)
                    .resultado(ResultadoPractica.APROBADO).build();

            assertThatThrownBy(() -> nota.actualizar(2.0, 3.0, null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Nested
        @DisplayName("NotaFinalCoordinadorService")
        @ExtendWith(MockitoExtension.class)
        class NotaFinalServiceTests {

            @Mock private NotaFinalCoordinadorRepository notaRepo;
            @Mock private EvaluacionFinalRepository evalRepo;
            @Mock private InstanciaPracticaRepository instanciaRepo;
            @Mock private ProgramaConfiguracionService configuracionService;
            @Mock private ApplicationEventPublisher eventPublisher;
            @InjectMocks private NotaFinalCoordinadorService service;

            @Test
            @DisplayName("Solo coordinador puede registrar la nota final")
            void registrar_rolNoCoordinador_lanzaAccesoDenegado() {
                CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
                RegistrarNotaFinalRequest req = new RegistrarNotaFinalRequest();

                assertThatThrownBy(() -> service.registrar(1L, req, actor))
                        .isInstanceOf(AccesoNoAutorizadoException.class);
            }

            @Test
            @DisplayName("Coordinador de otro programa lanza acceso denegado")
            void registrar_otroPrograma_lanzaAccesoDenegado() {
                InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
                when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

                CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 99L);
                RegistrarNotaFinalRequest req = new RegistrarNotaFinalRequest();

                assertThatThrownBy(() -> service.registrar(1L, req, actor))
                        .isInstanceOf(AccesoNoAutorizadoException.class);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-176/177 | EncuestaSatisfaccion — entidad de dominio
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-176/177 | EncuestaSatisfaccion — entidad de dominio")
    class EncuestaSatisfaccionEntityTests {

        @Test
        @DisplayName("State: enviar registra enviada=true y fechaEnvio")
        void enviar_registraEnvioCorrectamente() {
            EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder()
                    .tipo(TipoEncuesta.PARA_TUTOR).titulo("Test").build();

            encuesta.enviar();

            assertThat(encuesta.isEnviada()).isTrue();
            assertThat(encuesta.getFechaEnvio()).isNotNull();
        }

        @Test
        @DisplayName("State: guardar borrador transiciona a EN_BORRADOR")
        void guardarBorrador_encuestaEnviada_pasaAEnBorrador() {
            EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder()
                    .tipo(TipoEncuesta.PARA_TUTOR).titulo("Test").build();
            encuesta.enviar();
            List<String> respuestas = List.of("Respuesta 1");

            encuesta.guardarBorrador(respuestas);

            assertThat(encuesta.getEstado()).isEqualTo(EstadoEncuesta.EN_BORRADOR);
        }

        @Test
        @DisplayName("No se puede guardar borrador sin enviar primero")
        void guardarBorrador_sinEnviar_lanzaExcepcion() {
            EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder()
                    .tipo(TipoEncuesta.PARA_TUTOR).titulo("Test").build();
            List<String> respuestas = List.of("R1");

            assertThatThrownBy(() -> encuesta.guardarBorrador(respuestas))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("State: completar pasa a COMPLETADA e inmutable")
        void completar_encuestaEnviada_pasaACompletada() {
            EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder()
                    .tipo(TipoEncuesta.PARA_ESTUDIANTE).titulo("Test").build();
            encuesta.enviar();
            List<String> respuestas = List.of("R1", "R2");

            encuesta.completar(respuestas);

            assertThat(encuesta.isCompletada()).isTrue();
            assertThat(encuesta.getEstado()).isEqualTo(EstadoEncuesta.COMPLETADA);
            assertThat(encuesta.getFechaCompletada()).isNotNull();
        }

        @Test
        @DisplayName("Proxy: encuesta completada no admite nuevas respuestas")
        void completar_encuestaYaCompletada_lanzaExcepcion() {
            EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder()
                    .tipo(TipoEncuesta.PARA_ESTUDIANTE).titulo("Test").build();
            encuesta.enviar();
            encuesta.completar(List.of("R1"));
            List<String> nuevas = List.of("R2");

            assertThatThrownBy(() -> encuesta.completar(nuevas))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("No se puede completar sin haber enviado primero")
        void completar_sinEnviar_lanzaExcepcion() {
            EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder()
                    .tipo(TipoEncuesta.PARA_ESTUDIANTE).titulo("Test").build();
            List<String> respuestas = List.of("R1");

            assertThatThrownBy(() -> encuesta.completar(respuestas))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-176/177 | EncuestaSatisfaccionService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-176/177 | EncuestaSatisfaccionService")
    @ExtendWith(MockitoExtension.class)
    class EncuestaSatisfaccionServiceTests {

        @Mock private EncuestaSatisfaccionRepository encuestaRepo;
        @Mock private InstanciaPracticaRepository instanciaRepo;
        @Mock private UsuarioRepository usuarioRepo;
        @Mock private NotificacionConfigurableService notificacionService;
        @Mock private ApplicationEventPublisher eventPublisher;
        @InjectMocks private EncuestaSatisfaccionService service;

        @Test
        @DisplayName("Solo coordinador puede enviar encuesta al tutor")
        void enviarATutor_rolNoCoordinador_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
            EnviarEncuestaRequest req = new EnviarEncuestaRequest();

            assertThatThrownBy(() -> service.enviarATutor(1L, req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Solo coordinador puede enviar encuesta al estudiante")
        void enviarAEstudiante_rolNoCoordinador_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
            EnviarEncuestaRequest req = new EnviarEncuestaRequest();

            assertThatThrownBy(() -> service.enviarAEstudiante(1L, req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Token invalido lanza RecursoNoEncontrado")
        void completarPorToken_tokenInvalido_lanzaExcepcion() {
            when(encuestaRepo.findByTokenAcceso("TOKEN_INVALIDO")).thenReturn(Optional.empty());
            ResponderEncuestaRequest req = new ResponderEncuestaRequest();

            assertThatThrownBy(() -> service.completarPorToken("TOKEN_INVALIDO", req))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("Tutor no asignado a la practica no puede recibir encuesta")
        void enviarATutor_tutorNoAsignadoAInstancia_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.setTutorEmpresarial(
                    Usuario.builder().id(1L).correo("t@e.com").passwordHash("h").rol(Rol.TUTOR_EMPRESARIAL).build());
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));
            when(usuarioRepo.findById(99L)).thenReturn(
                    Optional.of(Usuario.builder().id(99L).correo("otro@e.com").passwordHash("h").rol(Rol.TUTOR_EMPRESARIAL).build()));

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            EnviarEncuestaRequest req = new EnviarEncuestaRequest();
            req.setTutorEmpresarialId(99L);

            assertThatThrownBy(() -> service.enviarATutor(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Practica cerrada no permite enviar encuesta")
        void enviarATutor_practicaCerrada_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            EnviarEncuestaRequest req = new EnviarEncuestaRequest();

            assertThatThrownBy(() -> service.enviarATutor(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-179 | SustentacionPractica — entidad de dominio
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-179 | SustentacionPractica — entidad de dominio")
    class SustentacionEntityTests {

        @Test
        @DisplayName("registrarResultado exitoso con jurado y acta firmada")
        void registrarResultado_condicionesValidas_registraCorrectamente() {
            SustentacionPractica sust = SustentacionPractica.builder()
                    .jurados(new ArrayList<>(List.of("Jurado A"))).build();

            sust.registrarResultado(ResultadoSustentacion.APROBADO, "http://acta.pdf", true);

            assertThat(sust.getResultado()).isEqualTo(ResultadoSustentacion.APROBADO);
            assertThat(sust.isActaFirmada()).isTrue();
            assertThat(sust.estaCompleta()).isTrue();
        }

        @Test
        @DisplayName("OCL minimoUnJurado: sin jurados lanza excepcion")
        void registrarResultado_sinJurados_lanzaExcepcion() {
            SustentacionPractica sust = SustentacionPractica.builder()
                    .jurados(new ArrayList<>()).build();

            assertThatThrownBy(() ->
                    sust.registrarResultado(ResultadoSustentacion.APROBADO, "http://acta.pdf", true))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("OCL actaObligatoria: sin acta firmada lanza excepcion")
        void registrarResultado_sinActaFirmada_lanzaExcepcion() {
            SustentacionPractica sust = SustentacionPractica.builder()
                    .jurados(new ArrayList<>(List.of("Jurado A"))).build();

            assertThatThrownBy(() ->
                    sust.registrarResultado(ResultadoSustentacion.APROBADO, null, false))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("estaCompleta retorna false cuando falta acta o jurados")
        void estaCompleta_incompleta_retornaFalse() {
            SustentacionPractica sust = SustentacionPractica.builder()
                    .jurados(new ArrayList<>()).build();

            assertThat(sust.estaCompleta()).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-179 | SustentacionService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-179 | SustentacionService")
    @ExtendWith(MockitoExtension.class)
    class SustentacionServiceTests {

        @Mock private SustentacionPracticaRepository sustRepo;
        @Mock private InstanciaPracticaRepository instanciaRepo;
        @InjectMocks private SustentacionService service;

        @Test
        @DisplayName("Solo coordinador puede programar sustentacion")
        void programar_rolNoCoordinador_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
            RegistrarSustentacionRequest req = new RegistrarSustentacionRequest();

            assertThatThrownBy(() -> service.programar(1L, req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Fecha de sustentacion anterior al inicio lanza excepcion")
        void programar_fechaAnteriorAlInicio_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.setFechaInicio(LocalDate.now());
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            lenient().when(actor.getUsuario()).thenReturn(mock(Usuario.class));

            RegistrarSustentacionRequest req = new RegistrarSustentacionRequest();
            req.setFecha(LocalDate.now().minusDays(1));
            req.setJurados(List.of("Jurado A"));

            assertThatThrownBy(() -> service.programar(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Expediente cerrado no permite programar sustentacion")
        void programar_practicaCerrada_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            RegistrarSustentacionRequest req = new RegistrarSustentacionRequest();

            assertThatThrownBy(() -> service.programar(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-179 | ChecklistCierreService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-179 | ChecklistCierreService")
    @ExtendWith(MockitoExtension.class)
    class ChecklistCierreServiceTests {

        @Mock private InstanciaPracticaRepository instanciaRepo;
        @Mock private EvaluacionFinalRepository evaluacionRepo;
        @Mock private NotaFinalCoordinadorRepository notaRepo;
        @Mock private EncuestaSatisfaccionRepository encuestaRepo;
        @Mock private PracticaDocumentoRepository documentoRepo;
        @Mock private SustentacionPracticaRepository sustRepo;
        @Mock private ProgramaConfiguracionService configuracionService;
        @InjectMocks private ChecklistCierreService service;

        private ProgramaConfiguracionResponse configCompleta() {
            return ProgramaConfiguracionResponse.builder()
                    .requisitosCierre("evaluacion_docente,evaluacion_tutor,nota_final," +
                            "encuesta_tutor,encuesta_estudiante,documentos,sustentacion")
                    .build();
        }

        @Test
        @DisplayName("Checklist incompleto deja puedeEjecutarCierre en false")
        void generar_checklistIncompleto_noPermiteCierre() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));
            when(configuracionService.obtener(10L)).thenReturn(configCompleta());
            when(evaluacionRepo.existsByInstanciaPractica_IdAndTipoAndEstado(
                    any(), any(), any())).thenReturn(false);
            when(notaRepo.existsByInstanciaPractica_Id(any())).thenReturn(false);
            when(encuestaRepo.existsByInstanciaPractica_IdAndTipoAndEstado(
                    any(), any(), any())).thenReturn(false);
            when(documentoRepo.countByInstanciaPractica_Id(any())).thenReturn(0L);
            when(sustRepo.findByInstanciaPractica_Id(any())).thenReturn(Optional.empty());

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            ChecklistCierreResponse resp = service.generar(1L, actor);

            assertThat(resp.isPuedeEjecutarCierre()).isFalse();
            assertThat(resp.getItems()).anyMatch(i -> !i.isCompleto());
        }

        @Test
        @DisplayName("Checklist completo habilita puedeEjecutarCierre")
        void generar_checklistCompleto_permiteEjecutarCierre() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));
            when(configuracionService.obtener(10L)).thenReturn(configCompleta());
            when(evaluacionRepo.existsByInstanciaPractica_IdAndTipoAndEstado(
                    any(), any(), any())).thenReturn(true);
            when(notaRepo.existsByInstanciaPractica_Id(any())).thenReturn(true);
            when(encuestaRepo.existsByInstanciaPractica_IdAndTipoAndEstado(
                    any(), any(), any())).thenReturn(true);
            when(documentoRepo.countByInstanciaPractica_Id(any())).thenReturn(3L);

            SustentacionPractica sustCompleta = SustentacionPractica.builder()
                    .jurados(new ArrayList<>(List.of("Jurado A")))
                    .actaUrl("http://acta.pdf").actaFirmada(true)
                    .resultado(ResultadoSustentacion.APROBADO).build();
            when(sustRepo.findByInstanciaPractica_Id(any()))
                    .thenReturn(Optional.of(sustCompleta));

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            ChecklistCierreResponse resp = service.generar(1L, actor);

            assertThat(resp.isPuedeEjecutarCierre()).isTrue();
        }

        @Test
        @DisplayName("Sustentacion siempre es obligatoria aunque no este en la config del programa")
        void generar_sustentacionSiempreObligatoria() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));
            when(configuracionService.obtener(10L)).thenReturn(
                    ProgramaConfiguracionResponse.builder()
                            .requisitosCierre("nota_final").build());
            when(notaRepo.existsByInstanciaPractica_Id(any())).thenReturn(true);
            when(sustRepo.findByInstanciaPractica_Id(any())).thenReturn(Optional.empty());

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            ChecklistCierreResponse resp = service.generar(1L, actor);

            assertThat(resp.isPuedeEjecutarCierre()).isFalse();
            assertThat(resp.getItems()).anyMatch(i -> "sustentacion".equals(i.getCodigo()));
        }

        @Test
        @DisplayName("Coordinador de otro programa no puede consultar checklist")
        void generar_otroPrograma_lanzaAccesoDenegado() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 99L);

            assertThatThrownBy(() -> service.generar(1L, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-180 | InstanciaPractica — State + cierre irreversible
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-180 | InstanciaPractica — State y cierre irreversible")
    class InstanciaPracticaStateTests {

        @Test
        @DisplayName("State: EN_CURSO transiciona a FINALIZADA con resultado")
        void finalizarConResultado_enCurso_transicionaCorrectamente() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);

            instancia.finalizarConResultado(ResultadoPractica.APROBADO);

            assertThat(instancia.getEstado()).isEqualTo(EstadoPractica.FINALIZADA);
            assertThat(instancia.getResultadoCierre()).isEqualTo(ResultadoPractica.APROBADO);
            assertThat(instancia.getFechaCierre()).isNotNull();
        }

        @Test
        @DisplayName("Estado no retrocede: FINALIZADA no puede finalizarse de nuevo")
        void finalizar_practicaFinalizada_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);

            assertThatThrownBy(() ->
                    instancia.finalizarConResultado(ResultadoPractica.NO_APROBADO))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Proxy: esInmutable es true cuando esta FINALIZADA")
        void esInmutable_finalizada_retornaTrue() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);

            assertThat(instancia.esInmutable()).isTrue();
        }

        @Test
        @DisplayName("Proxy: esInmutable es true cuando esta CANCELADA")
        void esInmutable_cancelada_retornaTrue() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.cancelar();

            assertThat(instancia.esInmutable()).isTrue();
        }

        @Test
        @DisplayName("Proxy: esInmutable es false cuando esta EN_CURSO")
        void esInmutable_enCurso_retornaFalse() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);

            assertThat(instancia.esInmutable()).isFalse();
        }

        @Test
        @DisplayName("FINALIZADA no puede cancelarse")
        void cancelar_practicaFinalizada_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.finalizarConResultado(ResultadoPractica.APROBADO);

            assertThatThrownBy(instancia::cancelar)
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Resultado null en cierre lanza excepcion")
        void finalizarConResultado_resultadoNull_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);

            assertThatThrownBy(() -> instancia.finalizarConResultado(null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-180 | CierreFormalFacade
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-180 | CierreFormalFacade")
    @ExtendWith(MockitoExtension.class)
    class CierreFormalFacadeTests {

        @Mock private InstanciaPracticaRepository instanciaRepo;
        @Mock private NotaFinalCoordinadorRepository notaRepo;
        @Mock private PazYSalvoRepository pazYSalvoRepo;
        @Mock private ChecklistCierreService checklistService;
        @Mock private NotificacionConfigurableService notificacionService;
        @Mock private ApplicationEventPublisher eventPublisher;
        @Mock private UsuarioRepository usuarioRepo;
        @InjectMocks private CierreFormalFacade facade;

        @Test
        @DisplayName("Solo coordinador puede ejecutar el cierre formal")
        void ejecutar_rolNoCoordinador_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.DOCENTE_ASESOR, null);
            EjecutarCierreRequest req = new EjecutarCierreRequest();
            req.setConfirmarCierreIrreversible(true);

            assertThatThrownBy(() -> facade.ejecutar(1L, req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Sin confirmacion explicita el cierre es rechazado")
        void ejecutar_sinConfirmacion_lanzaExcepcion() {
            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            EjecutarCierreRequest req = new EjecutarCierreRequest();
            req.setConfirmarCierreIrreversible(false);

            assertThatThrownBy(() -> facade.ejecutar(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Practica no EN_CURSO no puede cerrarse")
        void ejecutar_practicaNoEnCurso_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            instancia.cancelar();
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            EjecutarCierreRequest req = new EjecutarCierreRequest();
            req.setConfirmarCierreIrreversible(true);

            assertThatThrownBy(() -> facade.ejecutar(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("Checklist incompleto bloquea la ejecucion del cierre")
        void ejecutar_checklistIncompleto_lanzaExcepcion() {
            InstanciaPractica instancia = instanciaEnCurso(1L, 10L);
            when(instanciaRepo.findById(1L)).thenReturn(Optional.of(instancia));
            when(checklistService.generar(eq(1L), any())).thenReturn(
                    ChecklistCierreResponse.builder()
                            .puedeEjecutarCierre(false).items(List.of()).build());

            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            EjecutarCierreRequest req = new EjecutarCierreRequest();
            req.setConfirmarCierreIrreversible(true);

            assertThatThrownBy(() -> facade.ejecutar(1L, req, actor))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-191 | ProgramaConfiguracionService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-191 | ProgramaConfiguracionService")
    @ExtendWith(MockitoExtension.class)
    class ProgramaConfiguracionServiceTests {

        @Mock private ProgramaConfiguracionRepository configuracionRepo;
        @Mock private ProgramaRepository programaRepo;
        @InjectMocks private ProgramaConfiguracionService service;

        @Test
        @DisplayName("Solo DTI puede configurar parametros por programa")
        void configurar_rolNoDTI_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, null);
            ConfigurarProgramaRequest req = new ConfigurarProgramaRequest();

            assertThatThrownBy(() -> service.configurar(1L, req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Valores institucionales por defecto cuando el programa no tiene configuracion")
        void obtener_sinConfiguracion_retornaValoresPorDefecto() {
            when(configuracionRepo.findTopByPrograma_IdAndVigenteTrueOrderByCreadoEnDesc(1L))
                    .thenReturn(Optional.empty());

            ProgramaConfiguracionResponse config = service.obtener(1L);

            assertThat(config.getNotaMinimaAprobacion()).isEqualTo(3.0);
            assertThat(config.getNumeroPracticas()).isEqualTo(1);
            assertThat(config.getSemanasSeguimiento()).isEqualTo(12);
            assertThat(config.isVigente()).isTrue();
        }

        @Test
        @DisplayName("notaMinima retorna la nota configurada para el programa")
        void notaMinima_programaConConfig_retornaNotaConfigurada() {
            Programa programa = Programa.builder().id(1L).nombre("Ingenieria").build();
            ProgramaConfiguracion conf = ProgramaConfiguracion.builder()
                    .programa(programa).notaMinimaAprobacion(3.5).vigente(true).build();
            when(configuracionRepo.findTopByPrograma_IdAndVigenteTrueOrderByCreadoEnDesc(1L))
                    .thenReturn(Optional.of(conf));

            assertThat(service.notaMinima(1L)).isEqualTo(3.5);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-184 | ReporteEstadoProcesoService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-184 | ReporteEstadoProcesoService")
    @ExtendWith(MockitoExtension.class)
    class ReporteEstadoProcesoServiceTests {

        @Mock private UsuarioRepository usuarioRepo;
        @Mock private InstanciaPracticaRepository instanciaRepo;
        @InjectMocks private ReporteEstadoProcesoService service;

        @Test
        @DisplayName("Coordinador no puede ver reportes de otro programa")
        void construir_coordinadorOtroPrograma_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, 10L);
            ReporteEstadoProcesoRequest req = new ReporteEstadoProcesoRequest();
            req.setProgramaId(99L);

            assertThatThrownBy(() -> service.construir(req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Coordinador sin programa asignado lanza acceso denegado")
        void construir_coordinadorSinPrograma_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, null);
            ReporteEstadoProcesoRequest req = new ReporteEstadoProcesoRequest();

            assertThatThrownBy(() -> service.construir(req, actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GPE-187 | TableroGerencialDireccionService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GPE-187 | TableroGerencialDireccionService")
    @ExtendWith(MockitoExtension.class)
    class TableroGerencialTests {

        @Mock private ProgramaRepository programaRepo;
        @Mock private InstanciaPracticaRepository instanciaRepo;
        @Mock private NotaFinalCoordinadorRepository notaRepo;
        @Mock private EmpresaRepository empresaRepo;
        @InjectMocks private TableroGerencialDireccionService service;

        @Test
        @DisplayName("Rol distinto a DIRECCION lanza acceso denegado")
        void consultar_rolNoEsDireccion_lanzaAccesoDenegado() {
            CustomUserDetails actor = actor(Rol.COORDINADOR_PRACTICAS, null);

            assertThatThrownBy(() -> service.consultar(actor))
                    .isInstanceOf(AccesoNoAutorizadoException.class);
        }

        @Test
        @DisplayName("Direccion ve tasa de aprobacion calculada correctamente")
        void consultar_rolDireccion_retornaTasaAprobacion() {
            CustomUserDetails actor = actor(Rol.DIRECCION, null);
            when(programaRepo.findAll()).thenReturn(List.of());
            when(notaRepo.countByResultado(ResultadoPractica.APROBADO)).thenReturn(80L);
            when(notaRepo.countByResultado(ResultadoPractica.NO_APROBADO)).thenReturn(20L);
            when(empresaRepo.countByEstado(EstadoEmpresa.APROBADA)).thenReturn(5L);

            TableroGerencialResponse resp = service.consultar(actor);

            assertThat(resp.getTasaAprobacionGlobal()).isEqualTo(80.0);
            assertThat(resp.getEmpresasActivas()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Sin practicas evaluadas la tasa es 0.0")
        void consultar_sinPracticasEvaluadas_tasaCero() {
            CustomUserDetails actor = actor(Rol.DIRECCION, null);
            when(programaRepo.findAll()).thenReturn(List.of());
            when(notaRepo.countByResultado(ResultadoPractica.APROBADO)).thenReturn(0L);
            when(notaRepo.countByResultado(ResultadoPractica.NO_APROBADO)).thenReturn(0L);
            when(empresaRepo.countByEstado(any())).thenReturn(0L);

            TableroGerencialResponse resp = service.consultar(actor);

            assertThat(resp.getTasaAprobacionGlobal()).isEqualTo(0.0);
        }
    }
}