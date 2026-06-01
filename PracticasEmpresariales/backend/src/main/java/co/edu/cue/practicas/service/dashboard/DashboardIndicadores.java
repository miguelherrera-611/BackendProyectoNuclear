package co.edu.cue.practicas.service.dashboard;

import lombok.Builder;

import java.util.List;

/**
 * Contiene los indicadores calculados para el panel de inicio en Sprint 3.
 *
 * Se mantiene separado del DTO de salida para conservar SRP:
 * - DashboardIndicadorService calcula métricas
 * - DashboardMediator arma la estructura visual
 *
 * PATRON BUILDER: Lombok genera el builder para construir instancias con solo
 * los campos relevantes para cada rol, dejando el resto en 0 por defecto.
 */
@Builder
public record DashboardIndicadores(
        long usuariosActivosAdminDti,
        long usuariosActivosCoordinacionAcademica,
        long usuariosActivosCoordinadorPracticas,
        long usuariosActivosDocenteAsesor,
        long usuariosActivosTutorEmpresarial,
        long estudiantesNoApto,
        long estudiantesApto,
        long estudiantesNoAptoPendientesValidacion,
        long estudiantesAptoPendientesEnvioProceso,
        long estudiantesAptoDisponibles,
        long vacantesDisponibles,
        long practicasEnCurso,
        long planesPendientesAprobacion,
        long estudiantesAsignadosDocente,
        long seguimientosPendientesRevision,
        long sustentacionesProgramadas,
        long practicantesACargo,
        long encuestasPendientes,
        long practicasEnCursoDireccion,
        long tasaAprobacionGlobal,
        List<ProgramaIndicador> practicasEnCursoPorPrograma,
        long documentosPendientes,
        long semanaSeguimientoActual
) {
    public static DashboardIndicadores vacio() {
        return new DashboardIndicadores(
                0L, 0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L, 0L,
                null, 0L, 0L
        );
    }
}

