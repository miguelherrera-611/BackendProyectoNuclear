import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import type { SeguimientoSemanalResponse, EstadoSeguimiento } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'

const BADGE: Record<EstadoSeguimiento, string> = {
  ENVIADO:   'bg-blue-100 text-blue-800',
  REVISADO:  'bg-green-100 text-green-800',
  RECHAZADO: 'bg-red-100 text-red-800',
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  APROBADO:  'bg-green-100 text-green-800',
}

export default function SeguimientoDetallePage() {
  const { instanciaId } = useParams<{ instanciaId: string }>()
  const navigate = useNavigate()
  const [seguimientos, setSeguimientos] = useState<SeguimientoSemanalResponse[]>([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState('')
  const [procesando, setProcesando]     = useState<number | null>(null)
  const [observacion, setObservacion]   = useState<Record<number, string>>({})

  const cargar = async () => {
    if (!instanciaId) return
    setLoading(true)
    try {
      const data = await seguimientoService.listar(Number(instanciaId))
      setSeguimientos(data)
    } catch {
      setError('No se pudieron cargar los seguimientos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { cargar() }, [instanciaId])

  const handleRevisar = async (id: number) => {
    setProcesando(id)
    try {
      await seguimientoService.marcarRevisado(id)
      await cargar()
    } catch {
      setError('Error al marcar el seguimiento como revisado.')
    } finally {
      setProcesando(null)
    }
  }

  const handleRechazar = async (id: number) => {
    const obs = observacion[id]?.trim()
    if (!obs) { setError('La observación es obligatoria para rechazar.'); return }
    setProcesando(id)
    try {
      await seguimientoService.rechazar(id, obs)
      setObservacion(prev => { const c = { ...prev }; delete c[id]; return c })
      await cargar()
    } catch {
      setError('Error al rechazar el seguimiento.')
    } finally {
      setProcesando(null)
    }
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <button className="text-sm text-gray-500 hover:text-gray-700 mb-2" onClick={() => navigate(-1)}>
          ← Volver
        </button>
        <h1 className="text-2xl font-bold text-gray-900">Seguimientos semanales</h1>
        <p className="text-sm text-gray-500">Práctica #{instanciaId}</p>
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" /></div>
      ) : seguimientos.length === 0 ? (
        <div className="card text-center py-12">
          <div className="text-gray-300 text-4xl mb-3">📝</div>
          <p className="text-gray-400 text-sm">El estudiante no ha registrado seguimientos aún.</p>
        </div>
      ) : seguimientos.map(s => (
        <div key={s.id} className="card space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-gray-900">Semana {s.semana}</h3>
            <span className={`text-xs font-medium px-2 py-1 rounded-full ${BADGE[s.estado]}`}>{s.estado}</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
            <div>
              <p className="font-medium text-gray-700">Actividades</p>
              <p className="text-gray-600 mt-1 whitespace-pre-wrap">{s.actividades}</p>
            </div>
            <div>
              <p className="font-medium text-gray-700">Logros</p>
              <p className="text-gray-600 mt-1 whitespace-pre-wrap">{s.logros}</p>
            </div>
            {s.dificultades && (
              <div className="md:col-span-2">
                <p className="font-medium text-gray-700">Dificultades</p>
                <p className="text-gray-600 mt-1 whitespace-pre-wrap">{s.dificultades}</p>
              </div>
            )}
          </div>

          {s.observacionesDocente && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-800">
              <strong>Observación anterior:</strong> {s.observacionesDocente}
            </div>
          )}

          {s.estado === 'ENVIADO' && (
            <div className="space-y-2 border-t border-gray-100 pt-4">
              <textarea
                className="input-field text-sm"
                placeholder="Observación (obligatoria para rechazar)..."
                rows={2}
                value={observacion[s.id] ?? ''}
                onChange={e => setObservacion(prev => ({ ...prev, [s.id]: e.target.value }))}
              />
              <div className="flex gap-2">
                <button
                  className="btn-primary flex-1"
                  onClick={() => handleRevisar(s.id)}
                  disabled={procesando === s.id}
                >
                  {procesando === s.id ? 'Procesando...' : 'Marcar como revisado'}
                </button>
                <button
                  className="btn-secondary flex-1 text-red-600 border-red-200 hover:bg-red-50"
                  onClick={() => handleRechazar(s.id)}
                  disabled={procesando === s.id}
                >
                  Rechazar con observación
                </button>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
