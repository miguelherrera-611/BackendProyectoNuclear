import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function NoAutorizadoPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center max-w-md">
        <div className="text-6xl mb-4">🚫</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Acceso no autorizado</h1>
        <p className="text-gray-500 mb-6">
          No tienes permiso para acceder a este módulo.
          Esta acción ha sido registrada en la bitácora de auditoría.
        </p>
        <button
          onClick={() => navigate(isAuthenticated ? '/dashboard' : '/login')}
          className="btn-primary"
        >
          {isAuthenticated ? 'Volver al panel' : 'Ir al login'}
        </button>
      </div>
    </div>
  )
}
