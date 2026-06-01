import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'

export default function TutorMisPracticantesPage() {
  const navigate  = useNavigate()
  const [practicas, setPracticas] = useState<InstanciaPracticaResponseV2[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')
  const [busqueda, setBusqueda]   = useState('')

  useEffect(() => {
    seguimientoService.misPracticantes()
      .then(setPracticas)
      .catch(() => setError('No se pudieron cargar tus practicantes.'))
      .finally(() => setLoading(false))
  }, [])

  const filtradas: InstanciaPracticaResponseV2[] = busqueda
    ? practicas.filter(p =>
        p.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
        (p.razonSocialEmpresa ?? '').toLowerCase().includes(busqueda.toLowerCase())
      )
    : practicas

  const ESTADO_BADGE: Record<string, string> = {
    EN_CURSO:                  'bg-green-100 text-green-800',
    ASIGNADA_PENDIENTE_INICIO: 'bg-yellow-100 text-yellow-800',
    FINALIZADA:                'bg-blue-100 text-blue-800',
    CANCELADA:                 'bg-red-100 text-red-800',
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Mis Practicantes</h1>
        <p className="text-sm text-gray-500 mt-1">
          Estudiantes que estás tutorando en la empresa.
        </p>
      </div>

      {error && (
        <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>
      )}

      {/* Filtro */}
      {!loading && practicas.length > 0 && (
        <div className="card py-3 flex gap-3 items-center">
          <input
            className="input-field flex-1"
            placeholder="Buscar por estudiante o empresa..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
          />
          <span className="text-sm text-gray-500 whitespace-nowrap">
            {filtradas.length} de {practicas.length}
          </span>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
        </div>
      ) : filtradas.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">👨‍💼</div>
          <h3 className="font-medium text-gray-600 mb-1">
            {busqueda ? 'Sin resultados' : 'Sin practicantes asignados'}
          </h3>
          <p className="text-gray-400 text-sm">
            {busqueda ? 'Intenta con otro término de búsqueda.' : 'Aún no tienes practicantes asignados a tu cargo.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtradas.map(p => (
            <div key={p.id} className="card hover:shadow-md transition-shadow space-y-4">
              {/* Cabecera */}
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-900 truncate">{p.nombre}</h3>
                  <p className="text-xs text-gray-400 mt-0.5">{p.codigoMateria}</p>
                </div>
                <span className={`text-xs font-semibold px-2 py-0.5 rounded-full whitespace-nowrap ml-2 ${ESTADO_BADGE[p.estado] ?? 'bg-gray-100 text-gray-600'}`}>
                  {p.estado.replace(/_/g, ' ')}
                </span>
              </div>

              {/* Datos */}
              <div className="space-y-1.5 text-sm text-gray-600">
                <div className="flex justify-between">
                  <span className="text-gray-400">Docente asesor</span>
                  <span className="font-medium text-right">{p.nombreDocenteAsesor ?? '—'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Duración</span>
                  <span>{p.duracionSemanas} semanas</span>
                </div>
              </div>

              {/* Firmas (si aplica) */}
              {p.estado === 'ASIGNADA_PENDIENTE_INICIO' && (
                <div className="border-t border-gray-100 pt-3">
                  <p className="text-xs text-gray-500 mb-2 font-medium">Estado de firmas</p>
                  <div className="flex gap-3">
                    {[
                      { label: 'Tutor',     done: p.firmaTutor },
                      { label: 'Docente',   done: p.firmaDocente },
                      { label: 'Estudiante',done: p.firmaEstudiante },
                    ].map(firma => (
                      <div key={firma.label} className={`flex items-center gap-1 text-xs ${firma.done ? 'text-green-600' : 'text-gray-400'}`}>
                        <span>{firma.done ? '✓' : '○'}</span>
                        <span>{firma.label}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Acciones */}
              <div className="border-t border-gray-100 pt-3 flex gap-2">
                <button
                  className="btn-primary text-xs flex-1"
                  onClick={() => navigate(`/plan/${p.id}`)}
                >
                  Ver Plan
                </button>
                <button
                  className="btn-secondary text-xs flex-1"
                  onClick={() => navigate(`/seguimiento/${p.id}`)}
                >
                  Seguimientos
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
