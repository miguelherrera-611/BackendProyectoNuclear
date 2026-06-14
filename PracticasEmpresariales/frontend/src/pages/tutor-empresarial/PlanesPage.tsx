import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { planPracticaService } from '../../services/planPracticaService'
import type { PlanPracticaResponse, EstadoPlan } from '../../types'

const PLAN_BADGE: Record<EstadoPlan, string> = {
  BORRADOR:         'bg-amber-100 text-amber-800',
  APROBADO_TUTOR:   'bg-blue-100 text-blue-800',
  APROBADO_DOCENTE: 'bg-green-100 text-green-800',
  RECHAZADO:        'bg-red-100 text-red-800',
}

interface PracticaConPlan extends InstanciaPracticaResponseV2 {
  plan?: PlanPracticaResponse | null
  loadingPlan?: boolean
}

export default function PlanesPage() {
  const navigate = useNavigate()
  const [practicas, setPracticas] = useState<PracticaConPlan[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')
  const [success, setSuccess]     = useState('')

  const showSuccess = (msg: string) => {
    setSuccess(msg)
    setTimeout(() => setSuccess(''), 3000)
  }

  useEffect(() => {
    seguimientoService.misPracticantes()
      .then(async (lista) => {
        const conPlanes: PracticaConPlan[] = lista.map(p => ({ ...p, loadingPlan: true }))
        setPracticas(conPlanes)

        const actualizadas = await Promise.all(
          lista.map(async (p) => {
            try {
              const plan = await planPracticaService.obtenerActual(p.id)
              return { ...p, plan, loadingPlan: false }
            } catch {
              return { ...p, plan: null, loadingPlan: false }
            }
          })
        )
        setPracticas(actualizadas)
      })
      .catch(() => setError('No se pudieron cargar los planes.'))
      .finally(() => setLoading(false))
  }, [])

  const handleAprobar = async (practica: PracticaConPlan) => {
    if (!practica.plan) return
    try {
      await planPracticaService.aprobarTutor(practica.plan.id)
      showSuccess(`Plan de "${practica.nombre}" aprobado.`)
      setPracticas(prev => prev.map(p =>
        p.id === practica.id
          ? { ...p, plan: { ...p.plan!, estado: 'APROBADO_TUTOR' } }
          : p
      ))
    } catch (err: unknown) {
      setError((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al aprobar.')
    }
  }

  const pendientesAprobacion = practicas.filter(
    p => p.plan?.estado === 'BORRADOR'
  )
  const resto = practicas.filter(p => p.plan?.estado !== 'BORRADOR')

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Aprobar Planes</h1>
        <p className="text-sm text-gray-500 mt-1">
          Revisa y aprueba los planes de práctica de tus practicantes.
        </p>
      </div>

      {success && (
        <div className="bg-green-50 border border-green-200 text-green-800 rounded-lg px-4 py-3 text-sm flex items-center gap-2">
          <span className="text-green-500">✓</span> {success}
        </div>
      )}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">{error}</div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
        </div>
      ) : practicas.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">📋</div>
          <p className="text-gray-500 text-sm">No tienes practicantes asignados.</p>
        </div>
      ) : (
        <>
          {/* Pendientes de aprobación */}
          {pendientesAprobacion.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
                <h2 className="font-semibold text-gray-700 text-sm uppercase tracking-wide">
                  Pendientes de tu aprobación ({pendientesAprobacion.length})
                </h2>
              </div>
              {pendientesAprobacion.map(p => (
                <PlanCard
                  key={p.id}
                  practica={p}
                  onAprobar={() => handleAprobar(p)}
                  onVerPlan={() => navigate(`/plan/${p.id}`)}
                />
              ))}
            </div>
          )}

          {/* Resto */}
          {resto.length > 0 && (
            <div className="space-y-3">
              <h2 className="font-semibold text-gray-700 text-sm uppercase tracking-wide">
                Otros planes
              </h2>
              {resto.map(p => (
                <PlanCard
                  key={p.id}
                  practica={p}
                  onVerPlan={() => navigate(`/plan/${p.id}`)}
                />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}

function PlanCard({
  practica,
  onAprobar,
  onVerPlan,
}: {
  practica: PracticaConPlan
  onAprobar?: () => void
  onVerPlan: () => void
}) {
  const plan = practica.plan

  return (
    <div className="card flex items-center justify-between gap-4">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap mb-1">
          <h3 className="font-semibold text-gray-900 truncate">{practica.nombre}</h3>
          {plan ? (
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
              PLAN_BADGE[plan.estado] ?? 'bg-gray-100 text-gray-600'
            }`}>
              {plan.estado.replace(/_/g, ' ')}
            </span>
          ) : practica.loadingPlan ? (
            <span className="text-xs text-gray-400">Cargando plan...</span>
          ) : (
            <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">Sin plan</span>
          )}
        </div>
        <p className="text-sm text-gray-500">{practica.estado.replace(/_/g, ' ')}</p>
        {plan && (plan.documentoNombre || plan.objetivos) && (
          <p className="text-xs text-gray-400 mt-1 line-clamp-1">
            {plan.documentoNombre
              ? `Documento: ${plan.documentoNombre}`
              : `Objetivos: ${plan.objetivos}`}
          </p>
        )}
      </div>
      <div className="flex gap-2 shrink-0">
        {plan?.estado === 'BORRADOR' && onAprobar && (
          <button
            className="text-sm bg-green-100 text-green-700 px-4 py-1.5 rounded-lg hover:bg-green-200 transition-colors font-medium"
            onClick={onAprobar}
          >
            Aprobar
          </button>
        )}
        <button className="btn-secondary text-sm" onClick={onVerPlan}>
          Ver plan
        </button>
      </div>
    </div>
  )
}
