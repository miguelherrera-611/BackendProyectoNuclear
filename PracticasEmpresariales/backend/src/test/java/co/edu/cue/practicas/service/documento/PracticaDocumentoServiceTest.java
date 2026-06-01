package co.edu.cue.practicas.service.documento;

import co.edu.cue.practicas.audit.singleton.AuditoriaLogger;
import co.edu.cue.practicas.exception.AccesoNoAutorizadoException;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.*;
import co.edu.cue.practicas.model.enums.*;
import co.edu.cue.practicas.repository.documento.PracticaDocumentoRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GPE-162 / GPE-163 — Pruebas de carga de documentos de práctica.
 *
 * Nota: Los tests de la ruta feliz que escriben en disco se limitan
 * a verificar las reglas de negocio hasta el punto de persistencia.
 * La escritura real en disco pertenece a tests de integración.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PracticaDocumentoService — Pruebas unitarias (GPE-162 / GPE-163)")
class PracticaDocumentoServiceTest {

    @Mock private PracticaDocumentoRepository documentoRepository;
    @Mock private InstanciaPracticaRepository instanciaRepository;
    @Mock private AuditoriaLogger auditoriaLogger;

    @InjectMocks
    private PracticaDocumentoService service;

    private CustomUserDetails coordinador;
    private CustomUserDetails estudiante;
    private CustomUserDetails noAutorizado;
    private InstanciaPractica instanciaActiva;
    private InstanciaPractica instanciaFinalizada;
    private InstanciaPractica instanciaCancelada;

    private static final Long INSTANCIA_ID = 1L;

    @BeforeEach
    void setUp() {
        coordinador = udConRol(Rol.COORDINADOR_PRACTICAS, 1L);
        estudiante = udConRol(Rol.ESTUDIANTE, 10L);
        noAutorizado = udConRol(Rol.DIRECCION, 99L);

        instanciaActiva = instanciaConEstado(EstadoPractica.ASIGNADA_PENDIENTE_INICIO);
        instanciaFinalizada = instanciaConEstado(EstadoPractica.FINALIZADA);
        instanciaCancelada = instanciaConEstado(EstadoPractica.CANCELADA);
    }

    // =================================================================
    // Validaciones de acceso (roles)
    // =================================================================

    @Test
    @DisplayName("subirDocumento() debe bloquear si el rol no tiene acceso — OCL permisosDocumento")
    void subirDocumentoRolNoAutorizadoLanzaAccesoNoAutorizado() {
        MultipartFile archivo = archivoPdf("test.pdf", 100L);

        // validarAcceso() lanza la excepción antes de buscar la instancia
        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CARTA_PRESENTACION, archivo, noAutorizado))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verify(documentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("subirDocumento() debe bloquear si no hay sesión activa")
    void subirDocumentoSinAutenticacionLanzaAccesoNoAutorizado() {
        MultipartFile archivo = archivoPdf("test.pdf", 100L);

        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CARTA_PRESENTACION, archivo, null))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // =================================================================
    // Validaciones de inmutabilidad — GPE-162 / GPE-163
    // =================================================================

    @Test
    @DisplayName("subirDocumento() a práctica FINALIZADA debe lanzar excepción — documentos inmutables")
    void subirDocumentoPracticaFinalizadaLanzaExcepcion() {
        MultipartFile archivo = archivoPdf("carta.pdf", 500L);

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaFinalizada));

        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CARTA_PRESENTACION, archivo, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inmutables");

        verify(documentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("subirDocumento() a práctica CANCELADA debe lanzar excepción — documentos inmutables")
    void subirDocumentoPracticaCanceladaLanzaExcepcion() {
        MultipartFile archivo = archivoPdf("carta.pdf", 500L);

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaCancelada));

        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CONVENIO, archivo, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inmutables");
    }

    // =================================================================
    // Validaciones de archivo
    // =================================================================

    @Test
    @DisplayName("subirDocumento() archivo nulo debe lanzar excepción")
    void subirDocumentoArchivoNuloLanzaExcepcion() {
        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaActiva));

        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CARTA_PRESENTACION, null, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    @DisplayName("subirDocumento() archivo vacío debe lanzar excepción")
    void subirDocumentoArchivoVacioLanzaExcepcion() {
        MultipartFile archivoVacio = new MockMultipartFile("archivo", "vacio.pdf",
                "application/pdf", new byte[0]);

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaActiva));

        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CARTA_PRESENTACION, archivoVacio, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    @DisplayName("subirDocumento() archivo mayor a 10MB debe lanzar excepción")
    void subirDocumentoArchivoDemasiadoGrandeLanzaExcepcion() {
        long onzeMb = 11L * 1024 * 1024;
        MultipartFile archivoGrande = archivoPdf("grande.pdf", onzeMb);

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaActiva));

        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CARTA_PRESENTACION, archivoGrande, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("10MB");
    }

    @Test
    @DisplayName("subirDocumento() MIME no permitido debe lanzar excepción")
    void subirDocumentoMimeNoPermitidoLanzaExcepcion() {
        MultipartFile archivoExcel = archivoConMime("datos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 500L);

        when(instanciaRepository.findById(INSTANCIA_ID)).thenReturn(Optional.of(instanciaActiva));

        assertThatThrownBy(() -> service.subirDocumento(INSTANCIA_ID, TipoDocumento.CARTA_PRESENTACION, archivoExcel, coordinador))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("PDF");
    }

    // =================================================================
    // Validación de existencia de la instancia
    // =================================================================

    @Test
    @DisplayName("subirDocumento() instancia no encontrada debe lanzar 404")
    void subirDocumentoInstanciaNoEncontradaLanza404() {
        when(instanciaRepository.findById(99L)).thenReturn(Optional.empty());

        MultipartFile archivo = archivoPdf("test.pdf", 500L);

        assertThatThrownBy(() -> service.subirDocumento(99L, TipoDocumento.CARTA_PRESENTACION, archivo, coordinador))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =================================================================
    // listarDocumentos()
    // =================================================================

    @Test
    @DisplayName("listarDocumentos() retorna los documentos de una instancia existente")
    void listarDocumentosExitoso() {
        PracticaDocumento doc = PracticaDocumento.builder()
                .id(1L).instanciaPractica(instanciaActiva)
                .tipo(TipoDocumento.CARTA_PRESENTACION)
                .nombreOriginal("carta.pdf").rutaArchivo("uploads/1/carta.pdf")
                .mimeType("application/pdf").tamanoBytes(500L).build();

        when(instanciaRepository.existsById(INSTANCIA_ID)).thenReturn(true);
        when(documentoRepository.findByInstanciaPractica_IdOrderByCreadoEnDesc(INSTANCIA_ID))
                .thenReturn(List.of(doc));

        var resultado = service.listarDocumentos(INSTANCIA_ID, coordinador);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTipo()).isEqualTo(TipoDocumento.CARTA_PRESENTACION);
    }

    @Test
    @DisplayName("listarDocumentos() instancia inexistente debe lanzar 404")
    void listarDocumentosInstanciaNoEncontradaLanza404() {
        when(instanciaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarDocumentos(99L, coordinador))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // =================================================================
    // Helpers
    // =================================================================

    private MockMultipartFile archivoPdf(String nombre, long size) {
        return archivoConMime(nombre, "application/pdf", size);
    }

    private MockMultipartFile archivoConMime(String nombre, String contentType, long size) {
        byte[] contenido = new byte[(int) Math.min(size, 100)]; // byte array mínimo para test
        return new MockMultipartFile("archivo", nombre, contentType, contenido) {
            @Override public long getSize() { return size; }
        };
    }

    private InstanciaPractica instanciaConEstado(EstadoPractica estado) {
        return InstanciaPractica.builder()
                .id(INSTANCIA_ID).numeroPractica(1).nombre("P").materiaNucleo("M")
                .codigoMateria("C").numCortes(3).duracionSemanas(16).estado(estado).build();
    }

    private CustomUserDetails udConRol(Rol rol, Long id) {
        return new CustomUserDetails(Usuario.builder()
                .id(id).nombre("Test " + rol).correo("test@cue.edu.co")
                .passwordHash("hash").rol(rol).activo(true).build());
    }
}
