package co.edu.cue.practicas.service.catalogo;

import co.edu.cue.practicas.dto.request.CrearCatalogoPracticaRequest;
import co.edu.cue.practicas.dto.response.CatalogoPracticaResponse;
import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.exception.RecursoNoEncontradoException;
import co.edu.cue.practicas.model.entity.CatalogoPractica;
import co.edu.cue.practicas.model.entity.Programa;
import co.edu.cue.practicas.model.enums.EstadoPractica;
import co.edu.cue.practicas.pattern.builder.CatalogoPracticaDirector;
import co.edu.cue.practicas.repository.catalogo.CatalogoPracticaRepository;
import co.edu.cue.practicas.repository.expediente.InstanciaPracticaRepository;
import co.edu.cue.practicas.repository.programa.ProgramaRepository;
import co.edu.cue.practicas.service.mapper.EstudianteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoPracticaService — Pruebas unitarias")
class CatalogoPracticaServiceTest {

    @Mock private CatalogoPracticaRepository catalogoRepository;
    @Mock private ProgramaRepository programaRepository;
    @Mock private InstanciaPracticaRepository instanciaRepository;
    @Mock private CatalogoPracticaDirector director;
    @Mock private EstudianteMapper mapper;

    @InjectMocks
    private CatalogoPracticaService service;

    private Programa programa;
    private CatalogoPractica catalogoEjemplo;
    private CatalogoPracticaResponse responseEjemplo;

    @BeforeEach
    void setUp() {
        programa = Programa.builder().id(1L).nombre("Ing. Sistemas").build();

        catalogoEjemplo = CatalogoPractica.builder()
                .id(1L).programa(programa).numeroPractica(1)
                .nombre("Práctica Empresarial I").materiaNucleo("PE")
                .codigoMateria("PE-101").numCortes(3).duracionSemanas(16)
                .activo(true).creadoEn(LocalDateTime.now()).build();

        responseEjemplo = CatalogoPracticaResponse.builder()
                .id(1L).programaId(1L).programaNombre("Ing. Sistemas")
                .numeroPractica(1).nombre("Práctica Empresarial I").activo(true).build();
    }

    // =================================================================
    // crearEntrada
    // =================================================================

    @Test
    @DisplayName("crearEntrada exitoso debe persistir el catálogo y retornar el DTO")
    void crearEntradaExitoso() {
        CrearCatalogoPracticaRequest req = new CrearCatalogoPracticaRequest(
                1L, 1, "Práctica I", "PE", "PE-101", 3, 16, null);

        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        when(catalogoRepository.existsByPrograma_IdAndNumeroPractica(1L, 1)).thenReturn(false);
        when(director.construirDesdeSolicitud(programa, req)).thenReturn(catalogoEjemplo);
        when(catalogoRepository.save(catalogoEjemplo)).thenReturn(catalogoEjemplo);
        when(mapper.toCatalogoPracticaResponse(catalogoEjemplo)).thenReturn(responseEjemplo);

        CatalogoPracticaResponse resultado = service.crearEntrada(req);

        assertThat(resultado.getNombre()).isEqualTo("Práctica Empresarial I");
        verify(catalogoRepository).save(catalogoEjemplo);
    }

    @Test
    @DisplayName("crearEntrada debe lanzar excepción si el programa no existe")
    void crearEntradaProgramaNoEncontradoLanzaExcepcion() {
        CrearCatalogoPracticaRequest req = new CrearCatalogoPracticaRequest(
                99L, 1, "P I", "PE", "PE-101", 3, 16, null);

        when(programaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearEntrada(req))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Programa");

        verify(catalogoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEntrada debe lanzar excepción si ya existe el número de práctica en el programa")
    void crearEntradaNumeroRepetidoLanzaExcepcion() {
        CrearCatalogoPracticaRequest req = new CrearCatalogoPracticaRequest(
                1L, 1, "Práctica I", "PE", "PE-101", 3, 16, null);

        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        when(catalogoRepository.existsByPrograma_IdAndNumeroPractica(1L, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.crearEntrada(req))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("catálogo");

        verify(catalogoRepository, never()).save(any());
    }

    // =================================================================
    // desactivar
    // =================================================================

    @Test
    @DisplayName("desactivar exitoso cuando no hay estudiantes activos en la práctica")
    void desactivarExitosoSinEstudiantesActivos() {
        when(catalogoRepository.findById(1L)).thenReturn(Optional.of(catalogoEjemplo));
        // No hay instancias activas (todos están en estados finales)
        when(instanciaRepository.existsByCatalogoPracticaIdAndEstadoNotIn(eq(1L), any()))
                .thenReturn(false);
        when(catalogoRepository.save(any())).thenReturn(catalogoEjemplo);
        when(mapper.toCatalogoPracticaResponse(any())).thenReturn(responseEjemplo);

        assertThatCode(() -> service.desactivar(1L)).doesNotThrowAnyException();
        assertThat(catalogoEjemplo.isActivo()).isFalse();
        verify(catalogoRepository).save(catalogoEjemplo);
    }

    @Test
    @DisplayName("desactivar debe lanzar excepción si hay estudiantes activos — OCL noDesactivarConEstudiantesActivos")
    void desactivarConEstudiantesActivosLanzaExcepcion() {
        when(catalogoRepository.findById(1L)).thenReturn(Optional.of(catalogoEjemplo));
        when(instanciaRepository.existsByCatalogoPracticaIdAndEstadoNotIn(eq(1L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.desactivar(1L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("estudiantes activos");

        verify(catalogoRepository, never()).save(any());
    }

    // =================================================================
    // obtenerPorId
    // =================================================================

    @Test
    @DisplayName("obtenerPorId debe lanzar 404 si el catálogo no existe")
    void obtenerPorIdNoEncontradoLanza404() {
        when(catalogoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("listarPorPrograma debe retornar lista del repositorio mapeada")
    void listarPorProgramaRetornaLista() {
        when(catalogoRepository.findByPrograma_Id(1L)).thenReturn(List.of(catalogoEjemplo));
        when(mapper.toCatalogoPracticaResponse(catalogoEjemplo)).thenReturn(responseEjemplo);

        List<CatalogoPracticaResponse> resultado = service.listarPorPrograma(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Práctica Empresarial I");
    }
}
