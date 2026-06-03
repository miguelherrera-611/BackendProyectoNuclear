import { useState } from 'react'
import { sprint4Service } from '../../services/sprint4Service'
import type { ReporteEstadoProcesoResponse, TipoExportacionReporte } from '../../types'

export default function ReportesPage() {
  const [semestreAcademico, setSemestreAcademico] = useState('')
  const [formato, setFormato] = useState<TipoExportacionReporte>('EXCEL')
  const [reporte, setReporte] = useState<ReporteEstadoProcesoResponse | null>(null)
  const [mensaje, setMensaje] = useState('')

  const generar = async () => {
    setMensaje('')
    try {
      setReporte(await sprint4Service.reporteEstadoProceso({ semestreAcademico, formato }))
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo generar el reporte.')
    }
  }

  const descargar = () => {
    if (!reporte?.exportacion || !reporte.nombreArchivo) return
    const bytes = Uint8Array.from(atob(reporte.exportacion), c => c.charCodeAt(0))
    const blob = new Blob([bytes], { type: reporte.contentType ?? 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = reporte.nombreArchivo
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Reporte de estado del proceso</h1>
        <p className="text-sm text-gray-500 mt-1">Datos en tiempo real con scope del usuario autenticado.</p>
      </div>

      {mensaje && <div className="card text-sm">{mensaje}</div>}

      <div className="card grid grid-cols-1 md:grid-cols-3 gap-4">
        <input className="input-field self-end" placeholder="Semestre academico, ej. 2026-I" value={semestreAcademico} onChange={e => setSemestreAcademico(e.target.value)} />
        <select className="input-field self-end" value={formato} onChange={e => setFormato(e.target.value as TipoExportacionReporte)}>
          <option value="EXCEL">Excel / CSV</option>
          <option value="PDF">PDF</option>
        </select>
        <button className="btn-primary" onClick={generar}>Generar reporte</button>
      </div>

      {reporte && (
        <div className="card space-y-4">
          <div className="flex items-center justify-between gap-3">
            <h2 className="font-semibold text-gray-900">Totales por estado</h2>
            <button className="btn-secondary" onClick={descargar}>Descargar</button>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-5 gap-3">
            {Object.entries(reporte.estados).map(([estado, total]) => (
              <div key={estado} className="bg-gray-50 rounded-lg p-4">
                <p className="text-xs text-gray-500">{estado.replace(/_/g, ' ')}</p>
                <p className="text-2xl font-bold text-gray-900 mt-1">{total}</p>
              </div>
            ))}
          </div>
          <p className="text-sm text-gray-500">Total general: <strong>{reporte.total}</strong></p>
        </div>
      )}
    </div>
  )
}
