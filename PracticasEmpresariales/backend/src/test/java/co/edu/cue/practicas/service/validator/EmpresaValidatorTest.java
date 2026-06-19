package co.edu.cue.practicas.service.validator;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoEmpresa;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import co.edu.cue.practicas.repository.empresa.EmpresaRepository;
import co.edu.cue.practicas.repository.vacante.VacanteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaValidator — Pruebas unitarias (GPE-150)")
class EmpresaValidatorTest {

    @Mock private EmpresaRepository empresaRepository;
    @Mock private VacanteRepository vacanteRepository;

    @InjectMocks
    private EmpresaValidator validator;

    @Test
    @DisplayName("validarNitUnico no lanza excepcion si el NIT no existe")
    void validarNitUnico_nitLibre_noLanzaExcepcion() {
        when(empresaRepository.existsByNit("900.1")).thenReturn(false);

        assertThatCode(() -> validator.validarNitUnico("900.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarNitUnico lanza excepcion si el NIT ya esta registrado")
    void validarNitUnico_nitDuplicado_lanzaExcepcion() {
        when(empresaRepository.existsByNit("900.1")).thenReturn(true);

        assertThatThrownBy(() -> validator.validarNitUnico("900.1"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("900.1");
    }

    @Test
    @DisplayName("validarSinVacantesActivas no lanza excepcion si no hay vacantes PENDIENTE/DISPONIBLE")
    void validarSinVacantesActivas_sinVacantesActivas_noLanzaExcepcion() {
        when(vacanteRepository.existsByEmpresaIdAndEstadoIn(
                eq(1L), eq(List.of(EstadoVacante.PENDIENTE, EstadoVacante.DISPONIBLE))))
                .thenReturn(false);

        assertThatCode(() -> validator.validarSinVacantesActivas(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarSinVacantesActivas lanza excepcion si hay vacantes PENDIENTE/DISPONIBLE — OCL vinculacionRestringida")
    void validarSinVacantesActivas_conVacantesActivas_lanzaExcepcion() {
        when(vacanteRepository.existsByEmpresaIdAndEstadoIn(
                eq(1L), eq(List.of(EstadoVacante.PENDIENTE, EstadoVacante.DISPONIBLE))))
                .thenReturn(true);

        assertThatThrownBy(() -> validator.validarSinVacantesActivas(1L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("vacantes activas");
    }

    @Test
    @DisplayName("validarEmpresaAprobadaParaVacantes no lanza excepcion si la empresa esta ACTIVA")
    void validarEmpresaAprobadaParaVacantes_empresaActiva_noLanzaExcepcion() {
        Empresa empresa = Empresa.builder().id(1L).estado(EstadoEmpresa.ACTIVA).build();

        assertThatCode(() -> validator.validarEmpresaAprobadaParaVacantes(empresa)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarEmpresaAprobadaParaVacantes lanza excepcion si la empresa esta INACTIVA — OCL vacantesRequierenAprobacion")
    void validarEmpresaAprobadaParaVacantes_empresaInactiva_lanzaExcepcion() {
        Empresa empresa = Empresa.builder().id(1L).estado(EstadoEmpresa.INACTIVA).build();

        assertThatThrownBy(() -> validator.validarEmpresaAprobadaParaVacantes(empresa))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ACTIVAS");
    }

    @Test
    @DisplayName("validarEmpresaAprobadaParaTutores no lanza excepcion si la empresa esta ACTIVA")
    void validarEmpresaAprobadaParaTutores_empresaActiva_noLanzaExcepcion() {
        Empresa empresa = Empresa.builder().id(1L).estado(EstadoEmpresa.ACTIVA).build();

        assertThatCode(() -> validator.validarEmpresaAprobadaParaTutores(empresa)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarEmpresaAprobadaParaTutores lanza excepcion si la empresa esta INACTIVA")
    void validarEmpresaAprobadaParaTutores_empresaInactiva_lanzaExcepcion() {
        Empresa empresa = Empresa.builder().id(1L).estado(EstadoEmpresa.INACTIVA).build();

        assertThatThrownBy(() -> validator.validarEmpresaAprobadaParaTutores(empresa))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ACTIVAS");
    }
}
