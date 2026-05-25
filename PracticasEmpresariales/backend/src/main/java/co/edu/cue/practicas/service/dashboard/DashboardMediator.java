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
 */
@Component
public class DashboardMediator {

    /**
     * Punto central de coordinación: recibe el usuario autenticado
     * y devuelve la estructura del panel correcta para su rol.
     *
     * En Sprint 1 los contadores están vacíos (sin datos reales aún).
     * En Sprint 3 estos métodos retornarán datos reales.
     */
    public DashboardResponse resolverDashboard(CustomUserDetails userDetails) {
        Rol rol = userDetails.getRol();

        return switch (rol) {
            case ADMIN_DTI        -> construirDashboardDTI(userDetails);
            case COORDINACION_ACADEMICA -> construirDashboardCoordinacionAcademica(userDetails);
            case COORDINADOR_PRACTICAS -> construirDashboardCoordinadorPracticas(userDetails);
            case DOCENTE_ASESOR  -> construirDashboardDocenteAsesor(userDetails);
            case TUTOR_EMPRESARIAL -> construirDashboardTutor(userDetails);
            case ESTUDIANTE      -> construirDashboardEstudiante(userDetails);
            case DIRECCION       -> construirDashboardDireccion(userDetails);
        };
    }

    private DashboardResponse construirDashboardDTI(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Administrador DTI")
                .soloLectura(false)
                .secciones(List.of(
                        crearSeccion("Gestión de Usuarios", "usuarios", "/usuarios", 0),
                        crearSeccion("Facultades y Programas", "facultades", "/facultades", 0),
                        crearSeccion("Bitácora de Auditoría", "auditoria", "/auditoria", 0),
                        crearSeccion("Resumen del Sistema", "sistema", "/sistema", 0)
                ))
                .build();
    }

    private DashboardResponse construirDashboardCoordinacionAcademica(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .etiquetaCargo(ud.getEtiquetaCargo())
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

    private DashboardResponse construirDashboardDireccion(CustomUserDetails ud) {
        return DashboardResponse.builder()
                .rol(ud.getRol())
                .nombreUsuario(ud.getNombre())
                .titulo("Panel Dirección — Solo Lectura")
                .soloLectura(true)
                .secciones(List.of(
                        crearSeccion("Indicadores Institucionales", "indicadores", "/reportes/indicadores", 0),
                        crearSeccion("Reportes Gerenciales", "reportes", "/reportes", 0),
                        crearSeccion("Resumen Global", "resumen", "/reportes/resumen-global", 0)
                ))
                .build();
    }

    private Map<String, Object> crearSeccion(String titulo, String id, String ruta, int contador) {
        return Map.of(
                "id", id,
                "titulo", titulo,
                "ruta", ruta,
                "contador", contador
        );
    }
}
