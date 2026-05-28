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

            {/* Coordinador de Prácticas */}
            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/empresas" element={<EmpresasPage />} />
              <Route path="/vacantes" element={<VacantesPage />} />
              <Route path="/tutores" element={<TutoresPage />} />
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
