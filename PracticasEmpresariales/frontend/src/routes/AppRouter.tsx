import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ProtectedRoute from './ProtectedRoute'
import MainLayout from '../layouts/MainLayout'

// Auth
import LoginPage            from '../pages/auth/LoginPage'
import CambiarPasswordPage  from '../pages/auth/CambiarPasswordPage'

// Common
import DashboardPage        from '../pages/dashboard/DashboardPage'
import NoAutorizadoPage     from '../pages/NoAutorizadoPage'

// Admin DTI
import FacultadesPage       from '../pages/admin-dti/FacultadesPage'
import ProgramasPage        from '../pages/admin-dti/ProgramasPage'
import AuditoriaPage        from '../pages/admin-dti/AuditoriaPage'

// Usuarios (feature folder)
import UsuariosPage         from '../pages/usuarios/UsuariosPage'

// Empresas (feature folder)
import EmpresasPage         from '../pages/empresas/EmpresasPage'
import TutoresPage          from '../pages/empresas/TutoresPage'

// Vacantes (feature folder)
import VacantesPage         from '../pages/vacantes/VacantesPage'

// Asignaciones (feature folder)
import AsignacionesPage     from '../pages/asignaciones/AsignacionesPage'
import NuevaAsignacionPage  from '../pages/asignaciones/NuevaAsignacionPage'

// Vinculación (feature folder)
import VinculacionPage      from '../pages/vinculacion/VinculacionPage'

// Seguimiento (feature folder)
import TableroSeguimientoPage from '../pages/seguimiento/TableroSeguimientoPage'
import SeguimientoDetallePage from '../pages/seguimiento/SeguimientoDetallePage'

// Estudiantes (feature folder — listado coordinador)
import EstudiantesPage      from '../pages/estudiantes/EstudiantesPage'

// Coordinación Académica
import PracticasPage              from '../pages/coordinacion-academica/PracticasPage'
import EstudiantesValidacionPage  from '../pages/coordinacion-academica/EstudiantesValidacionPage'

// Docente Asesor (role folder)
import MisPracticantesPage  from '../pages/docente-asesor/MisPracticantesPage'
import SustentacionesPage   from '../pages/docente-asesor/SustentacionesPage'

// Tutor Empresarial (role folder)
import TutorMisPracticantesPage from '../pages/tutor-empresarial/MisPracticantesPage'
import PlanesPage               from '../pages/tutor-empresarial/PlanesPage'
import EncuestasPage            from '../pages/tutor-empresarial/EncuestasPage'

// Estudiante (role folder — vista del propio estudiante)
import MiPracticaPage           from '../pages/estudiante/MiPracticaPage'
import PlanPracticaPage         from '../pages/estudiante/PlanPracticaPage'
import SeguimientoEstudiantePage from '../pages/estudiante/SeguimientoEstudiantePage'
import MiExpedientePage         from '../pages/estudiante/MiExpedientePage'
import MisDocumentosPage        from '../pages/estudiante/MisDocumentosPage'

// Dirección (role folder)
import IndicadoresPage      from '../pages/direccion/IndicadoresPage'
import ReportesPage         from '../pages/reportes/ReportesPage'

export default function AppRouter() {
  const { user } = useAuth()

  return (
    <BrowserRouter>
      <Routes>
        {/* Públicas */}
        <Route path="/login"         element={<LoginPage />} />
        <Route path="/no-autorizado" element={<NoAutorizadoPage />} />

        {/* Cambio de contraseña obligatorio */}
        <Route element={<ProtectedRoute />}>
          <Route path="/cambiar-password" element={<CambiarPasswordPage />} />
        </Route>

        {/* Rutas protegidas con layout */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>

            {/* Dashboard — todos los roles */}
            <Route path="/dashboard" element={<DashboardPage />} />

            {/* ─── Admin DTI ─────────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['ADMIN_DTI']} />}>
              <Route path="/facultades" element={<FacultadesPage />} />
              <Route path="/programas"  element={<ProgramasPage />} />
              <Route path="/auditoria"  element={<AuditoriaPage />} />
            </Route>

            {/* Usuarios — Admin DTI */}
            <Route element={<ProtectedRoute rolesPermitidos={['ADMIN_DTI']} />}>
              <Route path="/usuarios" element={<UsuariosPage />} />
            </Route>

            {/* ─── Empresas ──────────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/empresas" element={<EmpresasPage />} />
              <Route path="/tutores"  element={<TutoresPage />} />
            </Route>

            {/* ─── Vacantes ──────────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/vacantes" element={<VacantesPage />} />
            </Route>

            {/* ─── Asignaciones ──────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/asignaciones"       element={<AsignacionesPage />} />
              <Route path="/asignaciones/nueva" element={<NuevaAsignacionPage />} />
            </Route>

            {/* ─── Vinculación ───────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/vinculacion/:instanciaId" element={<VinculacionPage />} />
            </Route>

            {/* ─── Estudiantes (listado coordinadores) ───────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS', 'COORDINACION_ACADEMICA']} />}>
              <Route path="/estudiantes" element={<EstudiantesPage />} />
            </Route>

            {/* ─── Coordinación Académica ────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINACION_ACADEMICA']} />}>
              <Route path="/validacion-estudiantes" element={<EstudiantesValidacionPage />} />
              <Route path="/practicas"              element={<PracticasPage />} />
            </Route>

            {/* ─── Seguimiento — tablero y detalle ───────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS', 'DOCENTE_ASESOR', 'ADMIN_DTI', 'DIRECCION']} />}>
              <Route path="/tablero-seguimiento"      element={<TableroSeguimientoPage />} />
              <Route path="/seguimientos"             element={<TableroSeguimientoPage />} />
              <Route path="/seguimiento/:instanciaId" element={<SeguimientoDetallePage />} />
            </Route>

            {/* ─── Docente Asesor ────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['DOCENTE_ASESOR']} />}>
              <Route path="/mis-practicantes" element={<MisPracticantesPage />} />
              <Route path="/sustentaciones"   element={<SustentacionesPage />} />
            </Route>

            {/* Plan — docente y tutor */}
            <Route element={<ProtectedRoute rolesPermitidos={['DOCENTE_ASESOR', 'TUTOR_EMPRESARIAL']} />}>
              <Route path="/plan/:instanciaId" element={<PlanPracticaPage />} />
            </Route>

            {/* ─── Tutor Empresarial ─────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['TUTOR_EMPRESARIAL']} />}>
              <Route path="/mis-practicantes-empresa" element={<TutorMisPracticantesPage />} />
              <Route path="/planes"                   element={<PlanesPage />} />
              <Route path="/encuestas"                element={<EncuestasPage />} />
            </Route>

            {/* ─── Estudiante ────────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['ESTUDIANTE']} />}>
              <Route path="/mi-practica"             element={<MiPracticaPage />} />
              <Route path="/mi-practica/plan"        element={<PlanPracticaPage />} />
              <Route path="/mi-practica/seguimiento" element={<SeguimientoEstudiantePage />} />
              <Route path="/mi-expediente"           element={<MiExpedientePage />} />
              <Route path="/mis-documentos"          element={<MisDocumentosPage />} />
            </Route>

            {/* ─── Dirección ─────────────────────────────────────────────── */}
            <Route element={<ProtectedRoute rolesPermitidos={['DIRECCION']} />}>
              <Route path="/indicadores" element={<IndicadoresPage />} />
              <Route path="/reportes"    element={<ReportesPage />} />
            </Route>

          </Route>
        </Route>

        {/* Redirección raíz */}
        <Route path="/" element={
          user ? <Navigate to="/dashboard" replace /> : <Navigate to="/login" replace />
        } />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
