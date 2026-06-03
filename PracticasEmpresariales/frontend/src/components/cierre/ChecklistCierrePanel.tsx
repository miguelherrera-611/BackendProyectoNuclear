import type { ChecklistCierreResponse } from '../../types'

interface Props {
  checklist?: ChecklistCierreResponse
  onCerrar?: () => void
  cerrando?: boolean
}

export function ChecklistCierrePanel({ checklist, onCerrar, cerrando }: Props) {
  // SPRINT 4 - Chain of Responsibility visual: renderiza cada requisito validado por el backend.
  if (!checklist) {
    return <div className="card text-sm text-gray-500">Selecciona una practica para consultar el checklist.</div>
  }

  return (
    <div className="card space-y-4">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="font-semibold text-gray-900">Checklist de cierre</h2>
          <p className="text-sm text-gray-500">Todos los requisitos deben estar completos.</p>
        </div>
        <span className={checklist.puedeEjecutarCierre ? 'badge-apto' : 'badge-no-apto'}>
          {checklist.puedeEjecutarCierre ? 'Listo' : 'Pendiente'}
        </span>
      </div>

      <div className="space-y-2">
        {checklist.items.map(item => (
          <div key={item.codigo} className={`rounded-lg border px-4 py-3 flex items-center justify-between gap-3 ${item.completo ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'}`}>
            <div>
              <p className={item.completo ? 'font-medium text-green-900' : 'font-medium text-red-900'}>{item.nombre}</p>
              {!item.completo && <p className="text-xs text-red-700 mt-0.5">{item.accionRequerida}</p>}
            </div>
            <span className={item.completo ? 'text-green-700 text-sm font-semibold' : 'text-red-700 text-sm font-semibold'}>
              {item.completo ? 'Completo' : 'Pendiente'}
            </span>
          </div>
        ))}
      </div>

      {onCerrar && (
        <button className="btn-primary w-full" disabled={!checklist.puedeEjecutarCierre || cerrando} onClick={onCerrar}>
          {cerrando ? 'Ejecutando cierre...' : 'Ejecutar cierre irreversible'}
        </button>
      )}
    </div>
  )
}
