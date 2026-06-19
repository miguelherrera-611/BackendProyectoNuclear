import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { Select } from '../../components/common/Select/Select'
import { ListFilters } from '../../components/common/ListFilters'

export default function MisPracticantesPage() {
  const navigate = useNavigate()
  const [practicas, setPracticas] = useState<InstanciaPracticaResponseV2[]>([])
  const [busqueda, setBusqueda] = useState('')
  const [estadoFiltro, setEstadoFiltro] = useState<'todos' | string>('todos')
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')

  useEffect(() => {
    seguimientoService.misPracticantes()
      .then(setPracticas)
      .catch(() => setError('No se pudieron cargar tus practicantes.'))
      .finally(() => setLoading(false))
  }, [])

  const practicasFiltradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return practicas.filter(p => {
      const coincideTexto = !texto || [p.nombre, p.nombreEstudiante, p.razonSocialEmpresa].some(valor => valor?.toLowerCase().includes(texto))
      const coincideEstado = estadoFiltro === 'todos' || p.estado === estadoFiltro
      return coincideTexto && coincideEstado
    })
  }, [busqueda, estadoFiltro, practicas])

  const limpiarFiltros = () => {
    setBusqueda('')
    setEstadoFiltro('todos')
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Mis practicantes</h1>
        <p className="text-sm text-gray-500">Estudiantes asignados a ti como docente asesor.</p>
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      {!loading && practicas.length > 0 && (
        <ListFilters
          search={{
            label: 'Buscar practicante',
            placeholder: 'Estudiante, empresa o práctica...',
            value: busqueda,
            onChange: setBusqueda,
          }}
          summary={`${practicasFiltradas.length} de ${practicas.length}`}
          onClear={limpiarFiltros}
        >
          <div className="w-full sm:w-56">
            <Select label="Estado" value={estadoFiltro} onChange={e => setEstadoFiltro(e.target.value)}>
              <option value="todos">Todos los estados</option>
              <option value="EN_CURSO">En curso</option>
              <option value="ASIGNADA_PENDIENTE_INICIO">Pendiente inicio</option>
              <option value="FINALIZADA">Finalizada</option>
              <option value="CANCELADA">Cancelada</option>
            </Select>
          </div>
        </ListFilters>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
        </div>
      ) : practicasFiltradas.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">👨‍🎓</div>
          <p className="text-gray-400 text-sm">
            {practicas.length === 0 ? 'No tienes practicantes asignados aún.' : 'No hay practicantes que coincidan con los filtros.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {practicasFiltradas.map(p => (
            <div key={p.id} className="card hover:shadow-md transition-shadow space-y-3">
              <div>
                <p className="text-xs font-semibold text-cue-primary uppercase tracking-wide mb-0.5">
                  {p.nombreEstudiante ?? 'Estudiante no asignado'}
                </p>
                <h3 className="font-semibold text-gray-900">{p.nombre}</h3>
                <p className="text-sm text-gray-500">{p.razonSocialEmpresa ?? 'Sin empresa'}</p>
              </div>
              <div className="text-sm text-gray-600 space-y-1">
                <p>Estado: <span className="font-medium text-green-700">{p.estado.replace(/_/g, ' ')}</span></p>
                <p>Inicio: {p.fechaInicio ?? '—'} · Fin: {p.fechaFin ?? '—'}</p>
              </div>
              <div className="flex gap-2">
                <button className="btn-secondary text-xs flex-1" onClick={() => navigate(`/seguimiento/${p.id}`)}>
                  Seguimientos
                </button>
                <button className="btn-secondary text-xs flex-1" onClick={() => navigate(`/plan/${p.id}`)}>
                  Plan
                </button>
                <button className="btn-primary text-xs flex-1" onClick={() => navigate(`/evaluacion-final/${p.id}`)}>
                  Evaluar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
