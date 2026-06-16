import { useState } from 'react'
import { sprint4Service } from '../../services/sprint4Service'
import type { ReporteEstadoProcesoResponse, TipoExportacionReporte } from '../../types'
import { Select } from '../../components/common/Select/Select'

const AÑO_ACTUAL = new Date().getFullYear()

const SEMESTRES = [
  { label: 'Todos los semestres', value: '' },
  ...Array.from({ length: 5 }, (_, i) => {
    const year = AÑO_ACTUAL - i
    return [
      { label: `${year} — Período II  (Jul – Dic)`, value: `${year}-II` },
      { label: `${year} — Período I   (Ene – Jun)`, value: `${year}-I`  },
    ]
  }).flat(),
]

const ESTADO_CONFIG: Record<string, { label: string; color: string }> = {
  NO_APTO:          { label: 'No aptos',            color: 'bg-red-50 text-red-700 border-red-200' },
  APTO_DISPONIBLE:  { label: 'Aptos sin práctica',  color: 'bg-yellow-50 text-yellow-700 border-yellow-200' },
  EN_CURSO:         { label: 'En curso',             color: 'bg-blue-50 text-blue-700 border-blue-200' },
  FINALIZADO:       { label: 'Finalizados',          color: 'bg-green-50 text-green-700 border-green-200' },
  CANCELADO:        { label: 'Cancelados',           color: 'bg-gray-50 text-gray-500 border-gray-200' },
}

export default function ReportesPage() {
  const [semestre, setSemestre]   = useState('')
  const [formato, setFormato]     = useState<TipoExportacionReporte>('EXCEL')
  const [reporte, setReporte]     = useState<ReporteEstadoProcesoResponse | null>(null)
  const [cargando, setCargando]   = useState(false)
  const [error, setError]         = useState('')

  const generar = async () => {
    setError('')
    setCargando(true)
    try {
      const data = await sprint4Service.reporteEstadoProceso({
        semestreAcademico: semestre || undefined,
        formato,
      })
      setReporte(data)
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
        ?? 'No se pudo generar el reporte.'
      )
    } finally {
      setCargando(false)
    }
  }

  const descargar = () => {
    if (!reporte?.exportacion || !reporte.nombreArchivo) return
    const bytes = Uint8Array.from(atob(reporte.exportacion), c => c.charCodeAt(0))
    const blob  = new Blob([bytes], { type: reporte.contentType ?? 'application/octet-stream' })
    const url   = URL.createObjectURL(blob)
    const a     = document.createElement('a')
    a.href      = url
    a.download  = reporte.nombreArchivo
    a.click()
    URL.revokeObjectURL(url)
  }

  const etiquetaSemestre = SEMESTRES.find(s => s.value === semestre)?.label ?? 'Todos los semestres'

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Reporte de estado del proceso</h1>
        <p className="text-sm text-gray-500 mt-1">
          Datos en tiempo real con el scope del usuario autenticado.
        </p>
      </div>

      {/* Filtros */}
      <div className="card space-y-4">
        <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">Filtros</h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Semestre */}
          <Select
            label="Semestre académico"
            value={semestre}
            onChange={e => { setSemestre(e.target.value); setReporte(null) }}
            hint={semestre ? `Filtrado por: ${semestre}` : 'Sin filtro de semestre — muestra todos los registros'}
          >
            {SEMESTRES.map(s => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </Select>

          {/* Formato */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Formato de exportación
            </label>
            <div className="flex gap-3">
              {(['EXCEL', 'PDF'] as TipoExportacionReporte[]).map(f => (
                <button
                  key={f}
                  type="button"
                  onClick={() => setFormato(f)}
                  className={`flex-1 py-2 px-4 rounded-lg border text-sm font-medium transition-colors ${
                    formato === f
                      ? 'bg-cue-primary text-white border-cue-primary'
                      : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {f === 'EXCEL' ? 'Excel / CSV' : 'PDF'}
                </button>
              ))}
            </div>
          </div>
        </div>

        <button
          className="btn-primary w-full md:w-auto"
          onClick={generar}
          disabled={cargando}
        >
          {cargando ? 'Generando...' : 'Generar reporte'}
        </button>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {/* Resultados */}
      {reporte && (
        <div className="card space-y-5">
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <div>
              <h2 className="font-semibold text-gray-900">Resultados</h2>
              <p className="text-xs text-gray-400 mt-0.5">{etiquetaSemestre}</p>
            </div>
            <button className="btn-secondary" onClick={descargar}>
              Descargar {formato === 'EXCEL' ? 'CSV' : 'PDF'}
            </button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            {Object.entries(reporte.estados).map(([estado, total]) => {
              const cfg = ESTADO_CONFIG[estado] ?? { label: estado.replace(/_/g, ' '), color: 'bg-gray-50 text-gray-700 border-gray-200' }
              return (
                <div key={estado} className={`rounded-xl border p-4 ${cfg.color}`}>
                  <p className="text-xs font-medium leading-tight">{cfg.label}</p>
                  <p className="text-3xl font-bold mt-2">{total}</p>
                </div>
              )
            })}
          </div>

          <div className="flex items-center justify-between border-t pt-4">
            <span className="text-sm text-gray-500">Total general</span>
            <span className="text-xl font-bold text-gray-900">{reporte.total}</span>
          </div>
        </div>
      )}
    </div>
  )
}
