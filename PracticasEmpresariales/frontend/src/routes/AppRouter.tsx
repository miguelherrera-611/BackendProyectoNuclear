import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ProtectedRoute from './ProtectedRoute'
import LoginPage from '../pages/auth/LoginPage'
import CambiarPasswordPage from '../pages/auth/CambiarPasswordPage'
import DashboardPage from '../pages/dashboard/DashboardPage'
import UsuariosPage from '../pages/admin-dti/UsuariosPage'
import FacultadesPage from '../pages/admin-dti/FacultadesPage'
import ProgramasPage from '../pages/admin-dti/ProgramasPage'
import AuditoriaPage from '../pages/admin-dti/AuditoriaPage'
import EmpresasPage from '../pages/coordinador-practicas/EmpresasPage'
import VacantesPage from '../pages/coordinador-practicas/VacantesPage'
import TutoresPage from '../pages/coordinador-practicas/TutoresPage'
import AsignacionesPage from '../pages/coordinador-practicas/AsignacionesPage'
import NuevaAsignacionPage from '../pages/coordinador-practicas/NuevaAsignacionPage'
import VinculacionPage from '../pages/coordinador-practicas/VinculacionPage'
import TableroSeguimientoPage from '../pages/coordinador-practicas/TableroSeguimientoPage'
import MisPracticantesPage from '../pages/docente/MisPracticantesPage'
import SeguimientoDetallePage from '../pages/docente/SeguimientoDetallePage'
import MiPracticaPage from '../pages/estudiante/MiPracticaPage'
import PlanPracticaPage from '../pages/estudiante/PlanPracticaPage'
import SeguimientoEstudiantePage from '../pages/estudiante/SeguimientoEstudiantePage'
import NoAutorizadoPage from '../pages/NoAutorizadoPage'
import MainLayout from '../layouts/MainLayout'

export default function AppRouter() {
  const { user } = useAuth()

  return (
    <BrowserRouter>
      <Routes>
        {/* Públicas */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/no-autorizado" element={<NoAutorizadoPage />} />

        {/* Cambio de contraseña obligatorio en primer ingreso */}
        <Route element={<ProtectedRoute />}>
          <Route path="/cambiar-password" element={<CambiarPasswordPage />} />
        </Route>

        {/* Rutas protegidas con layout */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />

            {/* DTI only */}
            <Route element={<ProtectedRoute rolesPermitidos={['ADMIN_DTI']} />}>
              <Route path="/usuarios" element={<UsuariosPage />} />
              <Route path="/facultades" element={<FacultadesPage />} />
              <Route path="/programas" element={<ProgramasPage />} />
              <Route path="/auditoria" element={<AuditoriaPage />} />
            </Route>

            {/* Coordinador de Prácticas — Sprint 1-2 */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/empresas" element={<EmpresasPage />} />
              <Route path="/vacantes" element={<VacantesPage />} />
              <Route path="/tutores" element={<TutoresPage />} />
            </Route>

            {/* Coordinador de Prácticas — Sprint 3 (GPE-157/158/159/162/163/164/167) */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/asignaciones" element={<AsignacionesPage />} />
              <Route path="/asignaciones/nueva" element={<NuevaAsignacionPage />} />
              <Route path="/vinculacion/:instanciaId" element={<VinculacionPage />} />
            </Route>

            {/* Tablero de seguimiento — coordinador y docente */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS', 'DOCENTE_ASESOR', 'ADMIN_DTI', 'DIRECCION']} />}>
              <Route path="/tablero-seguimiento" element={<TableroSeguimientoPage />} />
              <Route path="/seguimiento/:instanciaId" element={<SeguimientoDetallePage />} />
            </Route>

            {/* Docente Asesor — GPE-168 */}
            <Route element={<ProtectedRoute rolesPermitidos={['DOCENTE_ASESOR']} />}>
              <Route path="/mis-practicantes" element={<MisPracticantesPage />} />
            </Route>

            {/* Docente y Tutor — plan de práctica */}
            <Route element={<ProtectedRoute rolesPermitidos={['DOCENTE_ASESOR', 'TUTOR_EMPRESARIAL']} />}>
              <Route path="/plan/:instanciaId" element={<PlanPracticaPage />} />
            </Route>

            {/* Estudiante — GPE-132 / GPE-167 / GPE-170 */}
            <Route element={<ProtectedRoute rolesPermitidos={['ESTUDIANTE']} />}>
              <Route path="/mi-practica" element={<MiPracticaPage />} />
              <Route path="/mi-practica/plan" element={<PlanPracticaPage />} />
              <Route path="/mi-practica/seguimiento" element={<SeguimientoEstudiantePage />} />
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
