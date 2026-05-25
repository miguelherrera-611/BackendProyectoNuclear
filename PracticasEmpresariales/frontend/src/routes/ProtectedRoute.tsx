import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Rol } from '../types'

interface Props {
  rolesPermitidos?: Rol[]
}

/**
 * PATRON PROXY — Frontend
 *
 * Intercepta la navegación a rutas protegidas.
 * Verifica autenticación y rol antes de renderizar.
 * Si no está autenticado → redirige a /login.
 * Si no tiene el rol requerido → redirige a /no-autorizado.
 */
export default function ProtectedRoute({ rolesPermitidos }: Props) {
  const { user, isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (rolesPermitidos && user && !rolesPermitidos.includes(user.rol)) {
    return <Navigate to="/no-autorizado" replace />
  }

  return <Outlet />
}
