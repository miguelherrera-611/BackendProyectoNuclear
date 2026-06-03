import { useEffect, useState } from 'react'
import { sprint4Service } from '../../services/sprint4Service'
import type { TableroGerencialResponse } from '../../types'

export default function IndicadoresPage() {
  const [tablero, setTablero] = useState<TableroGerencialResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    sprint4Service.tableroDireccion()
      .then(setTablero)
      .catch(() => setError('No se pudieron cargar los indicadores.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return <div className="flex justify-center py-20"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" /></div>
  }

  const porPrograma = Object.entries(tablero?.practicantesEnCursoPorPrograma ?? {})
  const maxPrograma = porPrograma.reduce((max, [, val]) => Math.max(max, val), 1)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Tablero gerencial</h1>
        <p className="text-sm text-gray-500 mt-1">Vista exclusiva de Direccion, solo agregados y solo lectura.</p>
      </div>

      {error && <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">{error}</div>}

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="card">
          <p className="text-sm text-gray-500">Tasa aprobacion global</p>
          <p className="text-3xl font-bold text-gray-900 mt-2">{tablero?.tasaAprobacionGlobal ?? 0}%</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">Empresas activas</p>
          <p className="text-3xl font-bold text-gray-900 mt-2">{tablero?.empresasActivas ?? 0}</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">Modo</p>
          <p className="text-xl font-semibold text-gray-900 mt-2">Solo lectura</p>
        </div>
      </div>

      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-800">Practicantes EN_CURSO por programa</h2>
        {porPrograma.length === 0 ? (
          <p className="text-gray-400 text-sm text-center py-6">Sin datos</p>
        ) : porPrograma.map(([programa, cant]) => (
          <div key={programa} className="space-y-1">
            <div className="flex justify-between text-sm">
              <span className="text-gray-700 truncate mr-2">{programa}</span>
              <span className="font-semibold text-gray-900 shrink-0">{cant}</span>
            </div>
            <div className="w-full bg-gray-100 rounded-full h-2">
              <div className="h-2 rounded-full bg-cue-primary transition-all duration-700" style={{ width: `${(cant / maxPrograma) * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
