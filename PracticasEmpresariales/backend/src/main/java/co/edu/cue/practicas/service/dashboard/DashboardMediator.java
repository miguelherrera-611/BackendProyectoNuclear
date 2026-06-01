package co.edu.cue.practicas.service.dashboard;

import co.edu.cue.practicas.dto.response.DashboardResponse;
import co.edu.cue.practicas.model.enums.Rol;
import co.edu.cue.practicas.security.CustomUserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PATRON MEDIATOR — GPE-131
 *
 * Coordina la comunicación entre el sistema de autenticación y el módulo
 * de dashboard para determinar qué panel renderizar según el rol y
 * etiqueta detectados al iniciar sesión.
 *
 * El frontend no necesita saber qué componente renderizar: solo llama
 * al mediador con el usuario autenticado y recibe la estructura correcta.
 *
 * Cada rol tiene su propio panel con secciones distintas:
 *   - ADMIN_DTI              → gestión de usuarios, facultades y auditoría
 *   - COORDINACION_ACADEMICA → validación de estudiantes y catálogo de prácticas
 *   - COORDINADOR_PRACTICAS  → asignaciones, vacantes y seguimiento
 *   - DOCENTE_ASESOR         → sus estudiantes y sustentaciones
 *   - TUTOR_EMPRESARIAL      → practicantes a cargo y encuestas
 *   - ESTUDIANTE             → su propia práctica y documentos
 *   - DIRECCION              → solo lectura: indicadores y reportes
 */
@Component
public class DashboardMediator {

    /**
     * Punto central de coordinación: recibe el usuario autenticado
     * y devuelve la estructura del panel correcta para su rol.
     * Usa un switch expression de Java 14+ para mayor legibilidad.
     *
     * En Sprint 1 los contadores están vacíos (sin datos reales aún).
     * En Sprint 3 estos métodos retornarán datos reales.
     *
     * @param userDetails  usuario autenticado con su rol y etiqueta de cargo
     * @return estructura del dashboard con título, secciones y permisos de escritura
     */
    public DashboardResponse resolverDashboard(CustomUserDetails userDetails) {
        return resolverDashboard(userDetails, DashboardIndicadores.vacio());
    }

    /**
     * Variante con indicadores reales calculados por una capa de servicio externa.
     * Mantiene este mediador como ensamblador puro de la UI.
     */
    public DashboardResponse resolverDashboard(CustomUserDetails userDetails, DashboardIndicadores indicadores) {
        Rol rol = userDetails.getRol();

        // Cada caso del switch delega a un método privado especializado para ese rol
        return switch (rol) {
            case ADMIN_DTI              -> construirDashboardDTI(userDetails, indicadores);
            case COORDINACION_ACADEMICA -> construirDashboardCoordinacionAcademica(userDetails, indicadores);
            case COORDINADOR_PRACTICAS  -> construirDashboardCoordinadorPracticas(userDetails, indicadores);
            case DOCENTE_ASESOR         -> construirDashboardDocenteAsesor(userDetails, indicadores);
            case TUTOR_EMPRESARIAL      -> construirDashboardTutor(userDetails, indicadores);
            case ESTUDIANTE             -> construirDashboardEstudiante(userDetails, indicadores);
            case DIRECCION              -> construirDashboardDireccion(userDetails, indicadores);
        };
    }

    /**
     * Panel del Administrador DTI.
     * Tiene acceso completo: puede gestionar usuarios, facultades,
     * programas y ver la bitácora de auditoría del sistema.
     */
    private DashboardResponse construirDashboardDTI(CustomUserDetails ud, DashboardIndicadores indicadores) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Administrador DTI")
                .soloLectura(false) // el DTI tiene permisos de escritura en todo el sistema
                .secciones(List.of(
                        crearSeccion("Usuarios activos DTI", "usuarios-dti", "/usuarios?rol=ADMIN_DTI", indicadores.usuariosActivosAdminDti()),
                        crearSeccion("Usuarios activos Coordinación Académica", "usuarios-coordinacion", "/usuarios?rol=COORDINACION_ACADEMICA", indicadores.usuariosActivosCoordinacionAcademica()),
                        crearSeccion("Usuarios activos Coordinador Prácticas", "usuarios-coordinador", "/usuarios?rol=COORDINADOR_PRACTICAS", indicadores.usuariosActivosCoordinadorPracticas()),
                        crearSeccion("Usuarios activos Docente Asesor", "usuarios-docente", "/usuarios?rol=DOCENTE_ASESOR", indicadores.usuariosActivosDocenteAsesor()),
                        crearSeccion("Usuarios activos Tutor Empresarial", "usuarios-tutor", "/usuarios?rol=TUTOR_EMPRESARIAL", indicadores.usuariosActivosTutorEmpresarial()),
                        crearSeccion("Estudiantes NO_APTO", "estudiantes-no-apto", "/estudiantes?estado=NO_APTO", indicadores.estudiantesNoApto()),
                        crearSeccion("Estudiantes APTO", "estudiantes-apto", "/estudiantes?estado=APTO", indicadores.estudiantesApto())
                ))
                .build();
    }

    /**
     * Panel de Coordinación Académica.
     * Su función principal es validar estudiantes (cambiarlos de NO_APTO a APTO)
     * antes de que puedan ser asignados a una práctica empresarial.
     * La etiqueta de cargo (ej. Decano, Jefe de Dpto.) aparece en la cabecera del panel.
     */
    private DashboardResponse construirDashboardCoordinacionAcademica(CustomUserDetails ud, DashboardIndicadores indicadores) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .etiquetaCargo(ud.getEtiquetaCargo()) // muestra el cargo específico en el panel
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Coordinación Académica")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Estudiantes NO_APTO pendientes de validación", "estudiantes-no-apto", "/estudiantes?estado=NO_APTO", indicadores.estudiantesNoAptoPendientesValidacion()),
                        crearSeccion("Estudiantes APTO pendientes de enviar", "estudiantes-apto-pendiente", "/estudiantes?estado=APTO&enviado=false", indicadores.estudiantesAptoPendientesEnvioProceso()),
                        crearSeccion("Estudiantes APTO enviados", "estudiantes-apto-enviados", "/estudiantes?estado=APTO&enviado=true", indicadores.estudiantesApto())
                ))
                .build();
    }

    /**
     * Panel del Coordinador de Prácticas.
     * Se encarga de asignar estudiantes APTOS a las vacantes disponibles
     * y hacer seguimiento de las prácticas en curso.
     */
    private DashboardResponse construirDashboardCoordinadorPracticas(CustomUserDetails ud, DashboardIndicadores indicadores) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Coordinador de Prácticas")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Estudiantes APTOS disponibles", "estudiantes-aptos", "/estudiantes?estado=APTO&disponibles=true", indicadores.estudiantesAptoDisponibles()),
                        crearSeccion("Vacantes disponibles", "vacantes", "/vacantes?estado=DISPONIBLE", indicadores.vacantesDisponibles()),
                        crearSeccion("Prácticas EN_CURSO", "practicas-activas", "/practicas?estado=EN_CURSO", indicadores.practicasEnCurso()),
                        crearSeccion("Planes pendientes de aprobación", "planes", "/planes?pendientes=true", indicadores.planesPendientesAprobacion()),
                        crearSeccion("Cierres pendientes", "cierres", "/practicas?estado=FINALIZADA&sinCierre=true", 0)
                ))
                .build();
    }

    /**
     * Panel del Docente Asesor.
     * Hace seguimiento académico de los estudiantes asignados a él
     * y programa las sustentaciones al finalizar la práctica.
     */
    private DashboardResponse construirDashboardDocenteAsesor(CustomUserDetails ud, DashboardIndicadores indicadores) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Docente Asesor")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Estudiantes asignados", "mis-estudiantes", "/mis-estudiantes", indicadores.estudiantesAsignadosDocente()),
                        crearSeccion("Seguimientos pendientes de revisión", "seguimientos", "/seguimientos?pendientes=true", indicadores.seguimientosPendientesRevision()),
                        crearSeccion("Sustentaciones programadas", "sustentaciones", "/sustentaciones", indicadores.sustentacionesProgramadas())
                ))
                .build();
    }

    /**
     * Panel del Tutor Empresarial.
     * Es el supervisor dentro de la empresa donde el estudiante realiza la práctica.
     * Aprueba planes de trabajo y diligencia encuestas de evaluación.
     */
    private DashboardResponse construirDashboardTutor(CustomUserDetails ud, DashboardIndicadores indicadores) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Tutor Empresarial")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Practicantes a cargo", "practicantes", "/mis-practicantes", indicadores.practicantesACargo()),
                        crearSeccion("Planes pendientes de aprobación", "planes", "/planes?pendientes=true", indicadores.planesPendientesAprobacion()),
                        crearSeccion("Encuestas pendientes", "encuestas", "/encuestas?pendientes=true", indicadores.encuestasPendientes())
                ))
                .build();
    }

    /**
     * Panel del Estudiante.
     * Ve únicamente su propia práctica, el seguimiento de la semana actual
     * y los documentos que tiene pendientes por entregar.
     */
    private DashboardResponse construirDashboardEstudiante(CustomUserDetails ud, DashboardIndicadores indicadores) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Mi Panel")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Estado de mi práctica", "mi-practica", "/mi-practica", indicadores.semanaSeguimientoActual()),
                        crearSeccion("Semana de seguimiento actual", "seguimiento", "/mi-practica/seguimiento/actual", indicadores.semanaSeguimientoActual()),
                        crearSeccion("Documentos pendientes", "documentos", "/mi-practica/documentos", indicadores.documentosPendientes())
                ))
                .build();
    }

    /**
     * Panel de Dirección.
     * Solo tiene acceso de lectura para ver indicadores y reportes gerenciales.
     * El flag soloLectura=true es detectado por el ScopeValidationAspect (@SoloLectura)
     * para bloquear cualquier operación de escritura que intente ejecutar.
     */
    private DashboardResponse construirDashboardDireccion(CustomUserDetails ud, DashboardIndicadores indicadores) {
        List<Map<String, Object>> secciones = new java.util.ArrayList<>(List.of(
                crearSeccion("Prácticas EN_CURSO", "indicadores", "/reportes/indicadores", indicadores.practicasEnCursoDireccion()),
                crearSeccion("Tasa de aprobación global (%)", "reportes", "/reportes", indicadores.tasaAprobacionGlobal())
        ));

        if (indicadores.practicasEnCursoPorPrograma() != null) {
            for (ProgramaIndicador programaIndicador : indicadores.practicasEnCursoPorPrograma()) {
                secciones.add(crearSeccion(
                        "Prácticas EN_CURSO - " + programaIndicador.nombrePrograma(),
                        "programa-" + programaIndicador.programaId(),
                        "/reportes/indicadores?programaId=" + programaIndicador.programaId(),
                        programaIndicador.practicasEnCurso()));
            }
        }

        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Dirección — Solo Lectura")
                .soloLectura(true) // bloquea operaciones de escritura para este rol
                .secciones(secciones)
                .build();
    }

    /**
     * Crea un mapa con la estructura de una sección del dashboard.
     * Cada sección tiene un id único, un título visible, la ruta del endpoint
     * y un contador (ej. número de estudiantes pendientes).
     * En Sprint 1 el contador siempre es 0 porque no hay datos reales aún.
     */
    private Map<String, Object> crearSeccion(String titulo, String id, String ruta, long contador) {
        return Map.of(
                "id", id,
                "titulo", titulo,
                "ruta", ruta,
                "contador", contador
        );
    }
}
