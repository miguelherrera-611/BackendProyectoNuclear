import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2, Pageable } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { useAuth } from '../../context/AuthContext'
import { Select } from '../../components/common/Select/Select'
import { ListFilters } from '../../components/common/ListFilters'
import { Pagination } from '../../components/common/Table/Pagination'

export default function TableroSeguimientoPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const esCoordinador = user?.rol === 'COORDINADOR_PRACTICAS'
  const [practicas, setPracticas] = useState<InstanciaPracticaResponseV2[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<InstanciaPracticaResponseV2> | null>(null)
  const [busqueda, setBusqueda] = useState('')
  const [filtroEmpresa, setFiltroEmpresa] = useState('')
  const [filtroDocente, setFiltroDocente] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    seguimientoService.tableroGeneralPaginado(pagina)
      .then(p => {
        setPracticas(p.content)
        setPageData(p)
      })
      .catch(() => setError('No se pudo cargar el tablero de seguimiento.'))
      .finally(() => setLoading(false))
  }, [pagina])

  const filtradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return practicas.filter(p => {
      const porEmpresa = !filtroEmpresa || (p.razonSocialEmpresa ?? '').toLowerCase().includes(filtroEmpresa.toLowerCase())
      const porDocente = !filtroDocente || (p.nombreDocenteAsesor ?? '').toLowerCase().includes(filtroDocente.toLowerCase())
      const coincideTexto = !texto || [p.nombre, p.nombreEstudiante, p.razonSocialEmpresa, p.nombreDocenteAsesor, p.nombreTutorEmpresarial].some(valor => valor?.toLowerCase().includes(texto))
      return porEmpresa && porDocente && coincideTexto
    })
  }, [busqueda, filtroDocente, filtroEmpresa, practicas])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Tablero de seguimiento</h1>
        <p className="text-sm text-gray-500">Prácticas EN_CURSO — {practicas.length} activas en total.</p>
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      <ListFilters
        search={{
          label: 'Buscar práctica',
          placeholder: 'Práctica, estudiante, empresa, tutor o docente...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${filtradas.length} resultado${filtradas.length !== 1 ? 's' : ''}`}
        className="card"
      >
        <div className="flex-1 min-w-48">
          <Select label="Empresa" value={filtroEmpresa} onChange={e => setFiltroEmpresa(e.target.value)}>
            <option value="">Todas las empresas</option>
            {[...new Set(practicas.map(p => p.razonSocialEmpresa).filter(Boolean))].sort().map(e => (
              <option key={e} value={e!}>{e}</option>
            ))}
          </Select>
        </div>
        <div className="flex-1 min-w-48">
          <Select label="Docente asesor" value={filtroDocente} onChange={e => setFiltroDocente(e.target.value)}>
            <option value="">Todos los docentes</option>
            {[...new Set(practicas.map(p => p.nombreDocenteAsesor).filter(Boolean))].sort().map(d => (
              <option key={d} value={d!}>{d}</option>
            ))}
          </Select>
        </div>
      </ListFilters>

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
