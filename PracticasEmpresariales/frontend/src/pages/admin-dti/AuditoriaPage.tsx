import { useState, useEffect, useMemo } from 'react'
import { TipoAccion } from '../../types'
import api from '../../services/api'
import { ApiResponse, Pageable } from '../../types'
import { Select } from '../../components/common/Select/Select'
import { ListFilters } from '../../components/common/ListFilters'
import { Pagination } from '../../components/common/Table/Pagination'

const TIPO_ACCCION_BADGE: Partial<Record<TipoAccion, string>> = {
  LOGIN_EXITOSO:        'bg-green-100 text-green-800',
  LOGIN_FALLIDO:        'bg-red-100 text-red-800',
  ACCESO_NO_AUTORIZADO: 'bg-red-200 text-red-900',
  CREAR:                'bg-blue-100 text-blue-800',
  EDITAR:               'bg-yellow-100 text-yellow-800',
  DESACTIVAR:           'bg-orange-100 text-orange-800',
  ACTIVAR:              'bg-green-100 text-green-800',
  CAMBIO_PASSWORD:      'bg-purple-100 text-purple-800',
}

interface BitacoraResponseExt {
  id: number
  nombreUsuario: string
  rolUsuario: string
  etiquetaCargoUsuario?: string
  fechaHora: string
  modulo: string
  tipoAccion: TipoAccion
  exitoso: boolean
  ipOrigen?: string
}

export default function AuditoriaPage() {
  const [entradas, setEntradas] = useState<BitacoraResponseExt[]>([])
  const [loading, setLoading] = useState(true)
  const [busqueda, setBusqueda] = useState('')
  const [filtroAccion, setFiltroAccion] = useState('')
  const [filtroModulo, setFiltroModulo] = useState('')
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<BitacoraResponseExt> | null>(null)

  const cargar = () => {
    setLoading(true)
    const params = new URLSearchParams()
    if (filtroAccion) params.set('tipoAccion', filtroAccion)
    params.set('page', String(pagina))
    params.set('size', '20')

    api.get<ApiResponse<Pageable<BitacoraResponseExt>>>(`/auditoria?${params}`)
      .then(r => {
        setEntradas(r.data.datos?.content ?? [])
        setPageData(r.data.datos ?? null)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [pagina, filtroAccion])

  const modulosDisponibles = [...new Set(entradas.map(e => e.modulo))].sort()

  const entradasFiltradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return entradas.filter(e => {
      const coincideModulo = !filtroModulo || e.modulo === filtroModulo
      const coincideAccion = !filtroAccion || e.tipoAccion === filtroAccion
      const coincideTexto = !texto || [
        e.nombreUsuario,
        e.rolUsuario,
        e.etiquetaCargoUsuario,
        e.modulo,
        e.tipoAccion,
        e.ipOrigen,
      ].some(valor => valor?.toLowerCase().includes(texto))
      return coincideModulo && coincideAccion && coincideTexto
    })
  }, [busqueda, entradas, filtroAccion, filtroModulo])

  const limpiarFiltros = () => {
    setBusqueda('')
    setFiltroAccion('')
    setFiltroModulo('')
    setPagina(0)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Bitácora de Auditoría</h1>
          <p className="text-sm text-gray-500 mt-1">Solo lectura — No se puede modificar ni eliminar ninguna entrada</p>
        </div>
      </div>

      {/* Filtros */}
      <ListFilters
        search={{
          label: 'Buscar en auditoría',
          placeholder: 'Usuario, rol, módulo, acción o IP...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${entradasFiltradas.length} de ${entradas.length}`}
        onClear={limpiarFiltros}
      >
        <Select
          label="Tipo de acción"
          className="w-auto"
          value={filtroAccion}
          onChange={e => { setFiltroAccion(e.target.value); setPagina(0) }}
        >
          <option value="">Todos los tipos</option>
          {[
            'LOGIN_EXITOSO','LOGIN_FALLIDO','LOGOUT',
            'CREAR','EDITAR','CONFIRMAR','ASIGNAR',
            'DESACTIVAR','ACTIVAR','CAMBIO_ESTADO',
            'SUBIR_DOCUMENTO','FIRMAR','CERRAR','CALIFICAR',
            'ACCESO_NO_AUTORIZADO','CAMBIO_PASSWORD','RESET_PASSWORD',
            'EXPORTAR','CONSULTAR',
          ].map(t => (
            <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
          ))}
        </Select>
        <Select
          label="Módulo"
          className="w-auto"
          value={filtroModulo}
          onChange={e => setFiltroModulo(e.target.value)}
        >
          <option value="">Todos los módulos</option>
          {modulosDisponibles.map(m => (
            <option key={m} value={m}>{m}</option>
          ))}
        </Select>
      </ListFilters>

      {/* Tabla */}
      <div className="card overflow-x-auto p-0">
        <table className="w-full text-xs">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {['Fecha/Hora', 'Usuario', 'Rol / Cargo', 'Módulo', 'Acción', 'Estado', 'IP'].map(h => (
                <th key={h} className="text-left px-3 py-3 text-gray-600 font-semibold whitespace-nowrap">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">Cargando...</td></tr>
            ) : entradasFiltradas.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">No hay registros con esos filtros.</td></tr>
            ) : entradasFiltradas.map(e => (
              <tr key={e.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="px-3 py-2 text-gray-500 whitespace-nowrap">
                  {new Date(e.fechaHora).toLocaleString('es-CO')}
                </td>
                <td className="px-3 py-2 font-medium">{e.nombreUsuario}</td>
                <td className="px-3 py-2 text-gray-500">
                  {e.rolUsuario}{e.etiquetaCargoUsuario ? ` / ${e.etiquetaCargoUsuario}` : ''}
                </td>
                <td className="px-3 py-2 text-gray-500">{e.modulo}</td>
                <td className="px-3 py-2">
                  <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${TIPO_ACCCION_BADGE[e.tipoAccion] ?? 'bg-gray-100 text-gray-700'}`}>
                    {e.tipoAccion.replace(/_/g, ' ')}
                  </span>
                </td>
                <td className="px-3 py-2">
                  {e.exitoso
                    ? <span className="text-green-600 font-medium">✓</span>
                    : <span className="text-red-600 font-medium">✗</span>}
                </td>
                <td className="px-3 py-2 text-gray-400">{e.ipOrigen ?? '—'}</td>
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
