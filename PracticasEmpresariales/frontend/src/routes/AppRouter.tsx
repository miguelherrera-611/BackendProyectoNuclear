import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ProtectedRoute from './ProtectedRoute'
import MainLayout from '../layouts/MainLayout'

import LoginPage from '../pages/auth/LoginPage'
import CambiarPasswordPage from '../pages/auth/CambiarPasswordPage'
import DashboardPage from '../pages/dashboard/DashboardPage'
import NoAutorizadoPage from '../pages/NoAutorizadoPage'
import FacultadesPage from '../pages/admin-dti/FacultadesPage'
import ProgramasPage from '../pages/admin-dti/ProgramasPage'
import AuditoriaPage from '../pages/admin-dti/AuditoriaPage'
import UsuariosPage from '../pages/usuarios/UsuariosPage'
import EmpresasPage from '../pages/empresas/EmpresasPage'
import TutoresPage from '../pages/empresas/TutoresPage'
import VacantesPage from '../pages/vacantes/VacantesPage'
import AsignacionesPage from '../pages/asignaciones/AsignacionesPage'
import NuevaAsignacionPage from '../pages/asignaciones/NuevaAsignacionPage'
import VinculacionPage from '../pages/vinculacion/VinculacionPage'
import TableroSeguimientoPage from '../pages/seguimiento/TableroSeguimientoPage'
import SeguimientoDetallePage from '../pages/seguimiento/SeguimientoDetallePage'
import EstudiantesPage from '../pages/estudiantes/EstudiantesPage'
import PracticasPage from '../pages/coordinacion-academica/PracticasPage'
import EstudiantesValidacionPage from '../pages/coordinacion-academica/EstudiantesValidacionPage'
import ParametrosProgramaPage from '../pages/coordinacion-academica/ParametrosProgramaPage'
import MisPracticantesPage from '../pages/docente-asesor/MisPracticantesPage'
import SustentacionesPage from '../pages/docente-asesor/SustentacionesPage'
import TutorMisPracticantesPage from '../pages/tutor-empresarial/MisPracticantesPage'
import PlanesPage from '../pages/tutor-empresarial/PlanesPage'
import EncuestasPage from '../pages/tutor-empresarial/EncuestasPage'
import MiPracticaPage from '../pages/estudiante/MiPracticaPage'
import PlanPracticaPage from '../pages/estudiante/PlanPracticaPage'
import SeguimientoEstudiantePage from '../pages/estudiante/SeguimientoEstudiantePage'
import MiExpedientePage from '../pages/estudiante/MiExpedientePage'
import MisDocumentosPage from '../pages/estudiante/MisDocumentosPage'
import EncuestasEstudiantePage from '../pages/estudiante/EncuestasEstudiantePage'
import IndicadoresPage from '../pages/direccion/IndicadoresPage'
import ReportesPage from '../pages/reportes/ReportesPage'
import EvaluacionFinalPage from '../pages/calificaciones/EvaluacionFinalPage'
import NotaFinalCoordinadorPage from '../pages/calificaciones/NotaFinalCoordinadorPage'
import CierrePracticaPage from '../pages/cierre/CierrePracticaPage'
import ConfiguracionSprint4Page from '../pages/configuracion/ConfiguracionSprint4Page'
import EncuestaPublicaPage from '../pages/encuestas/EncuestaPublicaPage'
import EncuestasCoordinadorPage from '../pages/encuestas/EncuestasCoordinadorPage'

export default function AppRouter() {
  const { user } = useAuth()

  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/no-autorizado" element={<NoAutorizadoPage />} />
        <Route path="/encuesta-publica/:token" element={<EncuestaPublicaPage />} />
        <Route path="/api/v1/encuestas-satisfaccion/publica/:token" element={<EncuestaPublicaPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/cambiar-password" element={<CambiarPasswordPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />

            <Route element={<ProtectedRoute rolesPermitidos={['ADMIN_DTI']} />}>
              <Route path="/facultades" element={<FacultadesPage />} />
              <Route path="/programas" element={<ProgramasPage />} />
              <Route path="/auditoria" element={<AuditoriaPage />} />
              <Route path="/usuarios" element={<UsuariosPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS']} />}>
              <Route path="/empresas" element={<EmpresasPage />} />
              <Route path="/tutores" element={<TutoresPage />} />
              <Route path="/vacantes" element={<VacantesPage />} />
              <Route path="/asignaciones" element={<AsignacionesPage />} />
              <Route path="/asignaciones/nueva" element={<NuevaAsignacionPage />} />
              <Route path="/vinculacion/:instanciaId" element={<VinculacionPage />} />
              <Route path="/nota-final/:instanciaId" element={<NotaFinalCoordinadorPage />} />
              <Route path="/cierre-practica/:instanciaId" element={<CierrePracticaPage />} />
              <Route path="/encuestas-coordinador" element={<EncuestasCoordinadorPage />} />
              <Route path="/plantillas-correo" element={<ConfiguracionSprint4Page />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['COORDINACION_ACADEMICA']} />}>
              <Route path="/estudiantes" element={<EstudiantesPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['COORDINACION_ACADEMICA']} />}>
              <Route path="/validacion-estudiantes" element={<EstudiantesValidacionPage />} />
              <Route path="/practicas" element={<PracticasPage />} />
              <Route path="/parametros-programa" element={<ParametrosProgramaPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['DIRECCION']} />}>
              <Route path="/tablero-seguimiento" element={<TableroSeguimientoPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS', 'DOCENTE_ASESOR']} />}>
              <Route path="/seguimiento/:instanciaId" element={<SeguimientoDetallePage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['COORDINADOR_PRACTICAS', 'COORDINACION_ACADEMICA', 'DIRECCION']} />}>
              <Route path="/reportes-proceso" element={<ReportesPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['DOCENTE_ASESOR']} />}>
              <Route path="/mis-practicantes" element={<MisPracticantesPage />} />
              <Route path="/sustentaciones" element={<SustentacionesPage />} />
              <Route path="/evaluacion-final/:instanciaId" element={<EvaluacionFinalPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['DOCENTE_ASESOR', 'TUTOR_EMPRESARIAL']} />}>
              <Route path="/plan/:instanciaId" element={<PlanPracticaPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['TUTOR_EMPRESARIAL']} />}>
              <Route path="/mis-practicantes-empresa" element={<TutorMisPracticantesPage />} />
              <Route path="/planes" element={<PlanesPage />} />
              <Route path="/encuestas" element={<EncuestasPage />} />
              <Route path="/evaluacion-tutor/:instanciaId" element={<EvaluacionFinalPage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['ESTUDIANTE']} />}>
              <Route path="/mi-practica" element={<MiPracticaPage />} />
              <Route path="/mi-practica/plan" element={<PlanPracticaPage />} />
              <Route path="/mi-practica/seguimiento" element={<SeguimientoEstudiantePage />} />
              <Route path="/mi-expediente" element={<MiExpedientePage />} />
              <Route path="/mis-documentos" element={<MisDocumentosPage />} />
              <Route path="/mis-encuestas" element={<EncuestasEstudiantePage />} />
            </Route>

            <Route element={<ProtectedRoute rolesPermitidos={['DIRECCION']} />}>
              <Route path="/indicadores" element={<IndicadoresPage />} />
              <Route path="/reportes" element={<ReportesPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="/" element={user ? <Navigate to="/dashboard" replace /> : <Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
