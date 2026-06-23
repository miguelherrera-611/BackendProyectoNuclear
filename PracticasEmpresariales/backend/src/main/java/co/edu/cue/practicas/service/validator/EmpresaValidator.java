package co.edu.cue.practicas.service.validator;

import co.edu.cue.practicas.exception.OperacionNoPermitidaException;
import co.edu.cue.practicas.model.entity.Empresa;
import co.edu.cue.practicas.model.enums.EstadoVacante;
import co.edu.cue.practicas.repository.empresa.EmpresaRepository;
import co.edu.cue.practicas.repository.vacante.VacanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SOLID — SRP: validaciones de precondiciones externas de Empresa.
 * Separado de EmpresaService para que cada clase tenga una sola razón de cambio.
 *
 * EmpresaService cambia si cambia el flujo de negocio.
 * EmpresaValidator cambia si cambian las reglas de validación.
 */
@Component
@RequiredArgsConstructor
public class EmpresaValidator {

    private final EmpresaRepository empresaRepository;
    private final VacanteRepository vacanteRepository;

    /** OCL: nitUnico */
    public void validarNitUnico(String nit) {
        if (empresaRepository.existsByNit(nit))
            throw new OperacionNoPermitidaException(
                    "Ya existe una empresa registrada con NIT: " + nit);
    }

    /** OCL: nitUnico — variante para edición, excluye la propia empresa de la verificación */
    public void validarNitUnicoParaEdicion(String nit, Long empresaId) {
        if (empresaRepository.existsByNitAndIdNot(nit, empresaId))
            throw new OperacionNoPermitidaException(
                    "Ya existe otra empresa registrada con NIT: " + nit);
    }

    /** OCL: vinculacionRestringida — no inactivar con vacantes activas */
    public void validarSinVacantesActivas(Long empresaId) {
        boolean tieneActivas = vacanteRepository.existsByEmpresaIdAndEstadoIn(
                empresaId, List.of(EstadoVacante.PENDIENTE, EstadoVacante.DISPONIBLE));
        if (tieneActivas)
            throw new OperacionNoPermitidaException(
                    "No se puede inactivar la empresa: tiene vacantes activas. " +
                    "Cierre o rechace todas las vacantes primero.");
    }

    /** OCL: vacantesRequierenAprobacion */
    public void validarEmpresaAprobadaParaVacantes(Empresa empresa) {
        if (!empresa.puedeCrearVacantes())
            throw new OperacionNoPermitidaException(
                    "Solo se pueden crear vacantes para empresas ACTIVAS. " +
                    "Estado actual: " + empresa.getEstado());
    }

    /** OCL: empresaActiva para tutores */
    public void validarEmpresaAprobadaParaTutores(Empresa empresa) {
        if (!empresa.puedeVincularPracticantes())
            throw new OperacionNoPermitidaException(
                    "Solo se pueden registrar tutores en empresas ACTIVAS. " +
                    "Estado actual: " + empresa.getEstado());
    }
}
