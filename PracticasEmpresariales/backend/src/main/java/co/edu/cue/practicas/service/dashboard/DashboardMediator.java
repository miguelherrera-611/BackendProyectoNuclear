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
        Rol rol = userDetails.getRol();

        // Cada caso del switch delega a un método privado especializado para ese rol
        return switch (rol) {
            case ADMIN_DTI              -> construirDashboardDTI(userDetails);
            case COORDINACION_ACADEMICA -> construirDashboardCoordinacionAcademica(userDetails);
            case COORDINADOR_PRACTICAS  -> construirDashboardCoordinadorPracticas(userDetails);
            case DOCENTE_ASESOR         -> construirDashboardDocenteAsesor(userDetails);
            case TUTOR_EMPRESARIAL      -> construirDashboardTutor(userDetails);
            case ESTUDIANTE             -> construirDashboardEstudiante(userDetails);
            case DIRECCION              -> construirDashboardDireccion(userDetails);
        };
    }

    /**
     * Panel del Administrador DTI.
     * Tiene acceso completo: puede gestionar usuarios, facultades,
     * programas y ver la bitácora de auditoría del sistema.
     */
    private DashboardResponse construirDashboardDTI(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Administrador DTI")
                .soloLectura(false) // el DTI tiene permisos de escritura en todo el sistema
                .secciones(List.of(
                        crearSeccion("Gestión de Usuarios", "usuarios", "/usuarios", 0),
                        crearSeccion("Facultades y Programas", "facultades", "/facultades", 0),
                        crearSeccion("Bitácora de Auditoría", "auditoria", "/auditoria", 0),
                        crearSeccion("Resumen del Sistema", "sistema", "/sistema", 0)
                ))
                .build();
    }

    /**
     * Panel de Coordinación Académica.
     * Su función principal es validar estudiantes (cambiarlos de NO_APTO a APTO)
     * antes de que puedan ser asignados a una práctica empresarial.
     * La etiqueta de cargo (ej. Decano, Jefe de Dpto.) aparece en la cabecera del panel.
     */
    private DashboardResponse construirDashboardCoordinacionAcademica(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .etiquetaCargo(ud.getEtiquetaCargo()) // muestra el cargo específico en el panel
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Coordinación Académica")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Estudiantes Pendientes (NO_APTO)", "estudiantes-no-apto", "/estudiantes?estado=NO_APTO", 0),
                        crearSeccion("Estudiantes Validados (APTO)", "estudiantes-apto", "/estudiantes?estado=APTO", 0),
                        crearSeccion("Catálogo de Prácticas", "practicas", "/practicas", 0)
                ))
                .build();
    }

    /**
     * Panel del Coordinador de Prácticas.
     * Se encarga de asignar estudiantes APTOS a las vacantes disponibles
     * y hacer seguimiento de las prácticas en curso.
     */
    private DashboardResponse construirDashboardCoordinadorPracticas(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Coordinador de Prácticas")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Estudiantes APTOS Disponibles", "estudiantes-aptos", "/estudiantes?estado=APTO&disponibles=true", 0),
                        crearSeccion("Vacantes Activas", "vacantes", "/vacantes", 0),
                        crearSeccion("Asignaciones Pendientes", "asignaciones", "/asignaciones?estado=PENDIENTE", 0),
                        crearSeccion("Prácticas Activas", "practicas-activas", "/practicas?estado=EN_CURSO", 0),
                        crearSeccion("Cierres Pendientes", "cierres", "/practicas?estado=FINALIZADA&sinCierre=true", 0)
                ))
                .build();
    }

    /**
     * Panel del Docente Asesor.
     * Hace seguimiento académico de los estudiantes asignados a él
     * y programa las sustentaciones al finalizar la práctica.
     */
    private DashboardResponse construirDashboardDocenteAsesor(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Docente Asesor")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Mis Estudiantes", "mis-estudiantes", "/mis-estudiantes", 0),
                        crearSeccion("Seguimientos Pendientes", "seguimientos", "/seguimientos?pendientes=true", 0),
                        crearSeccion("Sustentaciones Programadas", "sustentaciones", "/sustentaciones", 0)
                ))
                .build();
    }

    /**
     * Panel del Tutor Empresarial.
     * Es el supervisor dentro de la empresa donde el estudiante realiza la práctica.
     * Aprueba planes de trabajo y diligencia encuestas de evaluación.
     */
    private DashboardResponse construirDashboardTutor(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Tutor Empresarial")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Practicantes a Cargo", "practicantes", "/mis-practicantes", 0),
                        crearSeccion("Planes Pendientes de Aprobación", "planes", "/planes?pendientes=true", 0),
                        crearSeccion("Encuestas Pendientes", "encuestas", "/encuestas?pendientes=true", 0)
                ))
                .build();
    }

    /**
     * Panel del Estudiante.
     * Ve únicamente su propia práctica, el seguimiento de la semana actual
     * y los documentos que tiene pendientes por entregar.
     */
    private DashboardResponse construirDashboardEstudiante(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Mi Panel")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Mi Práctica", "mi-practica", "/mi-practica", 0),
                        crearSeccion("Seguimiento Semana Actual", "seguimiento", "/mi-practica/seguimiento/actual", 0),
                        crearSeccion("Documentos Pendientes", "documentos", "/mi-practica/documentos", 0)
                ))
                .build();
    }

    /**
     * Panel de Dirección.
     * Solo tiene acceso de lectura para ver indicadores y reportes gerenciales.
     * El flag soloLectura=true es detectado por el ScopeValidationAspect (@SoloLectura)
     * para bloquear cualquier operación de escritura que intente ejecutar.
     */
    private DashboardResponse construirDashboardDireccion(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Dirección — Solo Lectura")
                .soloLectura(true) // bloquea operaciones de escritura para este rol
                .secciones(List.of(
                        crearSeccion("Indicadores Institucionales", "indicadores", "/reportes/indicadores", 0),
                        crearSeccion("Reportes Gerenciales", "reportes", "/reportes", 0),
                        crearSeccion("Resumen Global", "resumen", "/reportes/resumen-global", 0)
                ))
                .build();
    }

    /**
     * Crea un mapa con la estructura de una sección del dashboard.
     * Cada sección tiene un id único, un título visible, la ruta del endpoint
     * y un contador (ej. número de estudiantes pendientes).
     * En Sprint 1 el contador siempre es 0 porque no hay datos reales aún.
     */
    private Map<String, Object> crearSeccion(String titulo, String id, String ruta, int contador) {
        return Map.of(
                "id", id,
                "titulo", titulo,
                "ruta", ruta,
                "contador", contador
        );
    }
}
