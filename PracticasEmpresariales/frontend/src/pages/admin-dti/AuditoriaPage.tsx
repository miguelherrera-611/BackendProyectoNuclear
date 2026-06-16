import { useState, useEffect } from 'react'
import { TipoAccion } from '../../types'
import api from '../../services/api'
import { ApiResponse, Pageable } from '../../types'
import { Select } from '../../components/common/Select/Select'

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
  const [filtroAccion, setFiltroAccion] = useState('')
  const [filtroModulo, setFiltroModulo] = useState('')

  const cargar = () => {
    setLoading(true)
    const params = new URLSearchParams()
    if (filtroAccion) params.set('tipoAccion', filtroAccion)
    params.set('size', '200')

    api.get<ApiResponse<Pageable<BitacoraResponseExt>>>(`/auditoria?${params}`)
      .then(r => setEntradas(r.data.datos?.content ?? []))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const modulosDisponibles = [...new Set(entradas.map(e => e.modulo))].sort()

  const entradasFiltradas = filtroModulo
    ? entradas.filter(e => e.modulo === filtroModulo)
    : entradas

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Bitácora de Auditoría</h1>
          <p className="text-sm text-gray-500 mt-1">Solo lectura — No se puede modificar ni eliminar ninguna entrada</p>
        </div>
      </div>

      {/* Filtros */}
      <div className="card flex gap-4 flex-wrap items-end">
        <Select
          label="Tipo de acción"
          className="w-auto"
          value={filtroAccion}
          onChange={e => { setFiltroAccion(e.target.value); setFiltroModulo('') }}
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
        <button className="btn-primary" onClick={cargar}>Buscar</button>
      </div>

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
    </div>
  )
}
