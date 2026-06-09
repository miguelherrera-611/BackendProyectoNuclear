import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { useAuth } from '../../context/AuthContext'

export default function TableroSeguimientoPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const esCoordinador = user?.rol === 'COORDINADOR_PRACTICAS'
  const [practicas, setPracticas] = useState<InstanciaPracticaResponseV2[]>([])
  const [filtroEmpresa, setFiltroEmpresa] = useState('')
  const [filtroDocente, setFiltroDocente] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    seguimientoService.tableroGeneral()
      .then(setPracticas)
      .catch(() => setError('No se pudo cargar el tablero de seguimiento.'))
      .finally(() => setLoading(false))
  }, [])

  const filtradas = practicas.filter(p => {
    const porEmpresa = !filtroEmpresa || (p.razonSocialEmpresa ?? '').toLowerCase().includes(filtroEmpresa.toLowerCase())
    const porDocente = !filtroDocente || (p.nombreDocenteAsesor ?? '').toLowerCase().includes(filtroDocente.toLowerCase())
    return porEmpresa && porDocente
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Tablero de seguimiento</h1>
        <p className="text-sm text-gray-500">Prácticas EN_CURSO — {practicas.length} activas en total.</p>
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      <div className="card flex gap-4 items-end flex-wrap">
        <div className="flex-1 min-w-48">
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar empresa</label>
          <input className="input-field" placeholder="Nombre empresa..." value={filtroEmpresa} onChange={e => setFiltroEmpresa(e.target.value)} />
        </div>
        <div className="flex-1 min-w-48">
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar docente</label>
          <input className="input-field" placeholder="Nombre docente..." value={filtroDocente} onChange={e => setFiltroDocente(e.target.value)} />
        </div>
        <span className="text-sm text-gray-500">{filtradas.length} resultado{filtradas.length !== 1 ? 's' : ''}</span>
      </div>

      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {['Práctica', 'Empresa', 'Docente Asesor', 'Tutor', 'Inicio', 'Fin', 'Firmas', 'Acciones'].map(h => (
                <th key={h} className="text-left px-4 py-3 text-gray-600 font-semibold whitespace-nowrap">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={8} className="py-12 text-center"><div className="flex justify-center"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" /></div></td></tr>
            ) : filtradas.length === 0 ? (
              <tr><td colSpan={8} className="text-center py-12 text-gray-400">No hay prácticas EN_CURSO.</td></tr>
            ) : filtradas.map(p => (
              <tr key={p.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="px-4 py-3">
                  <div className="font-medium text-gray-900">{p.nombre}</div>
                  <div className="text-xs text-gray-500">ID #{p.id}</div>
                </td>
                <td className="px-4 py-3 text-gray-600">{p.razonSocialEmpresa ?? '—'}</td>
                <td className="px-4 py-3 text-gray-600">{p.nombreDocenteAsesor ?? <span className="text-amber-600 text-xs">Sin asignar</span>}</td>
                <td className="px-4 py-3 text-gray-600">{p.nombreTutorEmpresarial ?? <span className="text-amber-600 text-xs">Sin asignar</span>}</td>
                <td className="px-4 py-3 text-gray-600 whitespace-nowrap">{p.fechaInicio ?? '—'}</td>
                <td className="px-4 py-3 text-gray-600 whitespace-nowrap">{p.fechaFin ?? '—'}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-1 text-xs">
                    <span className={p.firmaTutor ? 'text-green-600 font-semibold' : 'text-gray-300'}>T</span>
                    <span className={p.firmaDocente ? 'text-green-600 font-semibold' : 'text-gray-300'}>D</span>
                    <span className={p.firmaEstudiante ? 'text-green-600 font-semibold' : 'text-gray-300'}>E</span>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    {esCoordinador && (
                      <>
                        <button className="btn-secondary text-xs" onClick={() => navigate(`/vinculacion/${p.id}`)}>Ver</button>
                        <button className="btn-secondary text-xs" onClick={() => navigate(`/seguimiento/${p.id}`)}>Seguimientos</button>
                        <button className="btn-secondary text-xs" onClick={() => navigate(`/nota-final/${p.id}`)}>Nota final</button>
                        <button className="btn-primary text-xs" onClick={() => navigate(`/cierre-practica/${p.id}`)}>Cierre</button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
