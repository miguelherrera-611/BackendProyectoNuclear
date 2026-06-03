import { useEffect, useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { dashboardService } from '../../services/dashboardService'
import type { DashboardResponse } from '../../types'
import { useNavigate } from 'react-router-dom'

/**
 * PATRON MEDIATOR + OBSERVER — Frontend
 *
 * Este componente consulta al DashboardMediator (backend) y renderiza
 * el panel correcto según el rol. Está preparado para recibir
 * actualizaciones en tiempo real en Sprint 3 (Observer/WebSocket).
 */
export default function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const cargar = () => {
    setError(null)
    setLoading(true)
    dashboardService.obtener()
      .then(setDashboard)
      .catch(() => setError('No se pudo cargar el panel. Verifica que el servidor esté activo.'))
      .finally(() => setLoading(false))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { cargar() }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin text-4xl">⟳</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <p className="text-red-600 font-semibold text-lg">{error}</p>
          <button
            onClick={cargar}
            className="mt-4 px-4 py-2 bg-cue-primary text-white rounded hover:bg-cue-primary/90 transition-colors"
          >
            Reintentar
          </button>
        </div>
      </div>
    )
  }

  const soloLectura = dashboard?.soloLectura ?? false

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {dashboard?.titulo ?? 'Panel de Inicio'}
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            Bienvenido/a, <strong>{user?.nombre}</strong>
          </p>
        </div>
        {dashboard?.soloLectura && (
          <span className="bg-amber-100 text-amber-800 text-sm font-medium px-4 py-2 rounded-full">
            Modo lectura — Dirección
          </span>
        )}
      </div>

      {/* Secciones del panel */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {dashboard?.secciones?.map((seccion) => (
          soloLectura ? (
            <div
              key={seccion.id}
              className="card text-left border-amber-200 bg-amber-50/30 group"
              aria-disabled="true"
            >
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-800">
                    {seccion.titulo}
                  </h3>
                  <p className={`text-3xl font-bold mt-3 ${seccion.contador > 0 ? 'text-red-600' : 'text-cue-primary'}`}>
                    {seccion.contador}
                  </p>
                  <p className={`text-xs mt-1 ${seccion.contador > 0 ? 'text-red-500 font-medium' : 'text-gray-400'}`}>
                    {seccion.contador > 0 ? 'Requiere atención' : 'Sin registros aún'}
                  </p>
                </div>
                <span className="text-amber-400 text-lg">→</span>
              </div>
            </div>
          ) : (
          <button
            key={seccion.id}
            onClick={() => navigate(seccion.ruta)}
            className={`card text-left hover:shadow-md transition-shadow hover:border-cue-accent group cursor-pointer ${seccion.contador > 0 ? 'border-red-200' : ''}`}
          >
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold text-gray-800 group-hover:text-cue-primary transition-colors">
                  {seccion.titulo}
                </h3>
                <p className={`text-3xl font-bold mt-3 ${seccion.contador > 0 ? 'text-red-600' : 'text-cue-primary'}`}>
                  {seccion.contador}
                </p>
                <p className={`text-xs mt-1 ${seccion.contador > 0 ? 'text-red-500 font-medium' : 'text-gray-400'}`}>
                  {seccion.contador === 0 ? 'Sin registros aún' : 'Requiere atención'}
                </p>
              </div>
              <span className="text-gray-300 group-hover:text-cue-accent transition-colors text-lg">→</span>
            </div>
          </button>
          )
        ))}
      </div>
    </div>
  )
}
