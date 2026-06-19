import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2, PlanPracticaResponse, EstadoPlan, Pageable } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { planPracticaService } from '../../services/planPracticaService'
import { ListFilters } from '../../components/common/ListFilters'
import { Pagination } from '../../components/common/Table/Pagination'

const PLAN_BADGE: Record<EstadoPlan, string> = {
  BORRADOR:         'bg-amber-100 text-amber-800',
  APROBADO_TUTOR:   'bg-blue-100 text-blue-800',
  APROBADO_DOCENTE: 'bg-green-100 text-green-800',
  RECHAZADO:        'bg-red-100 text-red-800',
}

const PLAN_LABEL: Record<EstadoPlan, string> = {
  BORRADOR:         'Pendiente de revisión',
  APROBADO_TUTOR:   'Revisado por tutor',
  APROBADO_DOCENTE: 'Aprobado por docente',
  RECHAZADO:        'Rechazado',
}

interface PracticaConPlan extends InstanciaPracticaResponseV2 {
  plan?: PlanPracticaResponse | null
  loadingPlan: boolean
}

export default function PlanEstudiantePage() {
  const navigate = useNavigate()
  const [practicas, setPracticas] = useState<PracticaConPlan[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<InstanciaPracticaResponseV2> | null>(null)
  const [busqueda, setBusqueda] = useState('')
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')

  useEffect(() => {
    setLoading(true)
    seguimientoService.misPracticantesPaginado(pagina)
      .then(async (data) => {
        const lista = data.content
        setPageData(data)
        setPracticas(lista.map(p => ({ ...p, loadingPlan: true })))

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
  }, [pagina])

  const practicasFiltradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return practicas.filter(p => !texto || [p.nombre, p.nombreEstudiante, p.razonSocialEmpresa, p.plan?.documentoNombre].some(valor => valor?.toLowerCase().includes(texto)))
  }, [busqueda, practicas])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Plan Estudiante</h1>
        <p className="text-sm text-gray-500 mt-1">
          Consulta el plan de práctica de cada uno de tus practicantes.
        </p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">{error}</div>
      )}

      {!loading && practicas.length > 0 && (
        <ListFilters
          search={{
            label: 'Buscar plan',
            placeholder: 'Estudiante, empresa o documento...',
            value: busqueda,
            onChange: setBusqueda,
          }}
          summary={`${practicasFiltradas.length} de ${practicas.length}`}
        />
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
        </div>
      ) : practicasFiltradas.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">📋</div>
          <p className="text-gray-500 text-sm">
            {practicas.length === 0 ? 'No tienes practicantes asignados.' : 'No hay planes que coincidan con la búsqueda.'}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {practicasFiltradas.map(p => (
            <div key={p.id} className="card flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">

                {/* Encabezado */}
                <div className="flex items-center gap-2 flex-wrap mb-1">
                  <h3 className="font-semibold text-gray-900">{p.nombre}</h3>
                  {p.loadingPlan ? (
                    <span className="text-xs text-gray-400">Cargando plan...</span>
                  ) : p.plan ? (
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${PLAN_BADGE[p.plan.estado]}`}>
                      {PLAN_LABEL[p.plan.estado]}
                    </span>
                  ) : (
                    <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">Sin plan</span>
                  )}
                </div>

                <p className="text-sm text-gray-500 mb-2">{p.estado.replace(/_/g, ' ')}</p>

                {/* Detalle del plan */}
                {p.plan && (
                  <div className="space-y-1">
                    {p.plan.documentoNombre && (
                      <p className="text-xs text-gray-500">
                        Documento:{' '}
                        <span className="text-gray-700 font-medium">{p.plan.documentoNombre}</span>
                      </p>
                    )}
                    {p.plan.objetivos && (
                      <p className="text-xs text-gray-500 line-clamp-1">
                        Objetivos:{' '}
                        <span className="text-gray-700">{p.plan.objetivos}</span>
                      </p>
                    )}
                    {p.plan.motivoRechazo && (
                      <p className="text-xs text-red-500">
                        Motivo rechazo: {p.plan.motivoRechazo}
                      </p>
                    )}
                  </div>
                )}

                {!p.loadingPlan && !p.plan && (
                  <p className="text-xs text-gray-400">El estudiante aún no ha subido su plan.</p>
                )}
              </div>

              <button
                className="btn-secondary text-sm shrink-0 disabled:opacity-40 disabled:cursor-not-allowed"
                onClick={() => navigate(`/plan/${p.id}`)}
                disabled={!p.plan}
              >
                Ver plan
              </button>
            </div>
          ))}
        </div>
      )}

      <Pagination
        page={pagina}
        totalPages={pageData?.totalPages ?? 0}
        totalElements={pageData?.totalElements}
        onPageChange={setPagina}
        disabled={loading}
      />
    </div>
  )
}
