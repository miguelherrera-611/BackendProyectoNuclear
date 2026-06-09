import { useMemo, useState } from 'react'
import type { CriterioEvaluacion } from '../../types'

interface Props {
  titulo: string
  descripcion?: string
  onSubmit: (criterios: CriterioEvaluacion[], observaciones: string) => Promise<void>
}

interface FilaCriterio {
  nombre: string
  porcentaje: number  // 0–100, e.g. 40 means 40%
  nota: number        // 0.0–5.0
}

const filasIniciales: FilaCriterio[] = [
  { nombre: 'Desempeño tecnico', porcentaje: 40, nota: 4.0 },
  { nombre: 'Trabajo en equipo', porcentaje: 30, nota: 4.0 },
  { nombre: 'Responsabilidad',   porcentaje: 30, nota: 4.0 },
]

const clamp = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v))

export function EvaluacionFinalForm({ titulo, descripcion, onSubmit }: Props) {
  const [filas, setFilas] = useState<FilaCriterio[]>(filasIniciales)
  const [observaciones, setObservaciones] = useState('')
  const [saving, setSaving] = useState(false)
  const [mensaje, setMensaje] = useState('')

  const sumaPorcentajes = useMemo(
    () => filas.reduce((acc, f) => acc + f.porcentaje, 0),
    [filas],
  )

  // promedio ponderado en escala 0.0–5.0
  const promedio = useMemo(
    () => Math.round(filas.reduce((acc, f) => acc + (f.porcentaje / 100) * f.nota, 0) * 100) / 100,
    [filas],
  )

  const updateFila = (idx: number, patch: Partial<FilaCriterio>) =>
    setFilas(prev => prev.map((f, i) => i === idx ? { ...f, ...patch } : f))

  const ejecutarGuardado = async () => {
    if (sumaPorcentajes !== 100) {
      setMensaje('Los porcentajes deben sumar exactamente 100%.')
      return
    }
    setMensaje('')
    setSaving(true)
    try {
      // Convert to backend format: peso = porcentaje / 100, puntaje = nota
      const criterios: CriterioEvaluacion[] = filas.map(f => ({
        nombre: f.nombre,
        peso: f.porcentaje / 100,
        puntaje: f.nota,
      }))
      await onSubmit(criterios, observaciones)
      setMensaje('Registro guardado correctamente.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo guardar el registro.')
    } finally {
      setSaving(false)
    }
  }

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    ejecutarGuardado()
  }

  return (
    <form onSubmit={submit} className="card space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="font-semibold text-gray-900">{titulo}</h2>
          {descripcion && <p className="text-sm text-gray-500 mt-1">{descripcion}</p>}
        </div>
        <div className="text-right">
          <p className="text-xs text-gray-400">Nota final calculada</p>
          <p className="text-3xl font-bold text-cue-primary">{promedio.toFixed(2)}</p>
          <p className="text-xs text-gray-400">escala 0.0 – 5.0</p>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-100">
            <tr>
              <th className="text-left px-3 py-2">Criterio</th>
              <th className="text-left px-3 py-2 w-32">Porcentaje (%)</th>
              <th className="text-left px-3 py-2 w-36">Nota (0.0 – 5.0)</th>
            </tr>
          </thead>
          <tbody>
            {filas.map((f, idx) => (
              <tr key={idx} className="border-b border-gray-100">
                <td className="px-3 py-2">
                  <input
                    className="input-field"
                    value={f.nombre}
                    onChange={e => updateFila(idx, { nombre: e.target.value })}
                  />
                </td>
                <td className="px-3 py-2">
                  <div className="flex items-center gap-1">
                    <input
                      className="input-field"
                      type="number"
                      min={0}
                      max={100}
                      step={1}
                      value={f.porcentaje}
                      onChange={e => updateFila(idx, { porcentaje: clamp(Number(e.target.value), 0, 100) })}
                    />
                    <span className="text-gray-500 text-xs">%</span>
                  </div>
                </td>
                <td className="px-3 py-2">
                  <input
                    className="input-field"
                    type="number"
                    min={0}
                    max={5}
                    step={0.1}
                    value={f.nota}
                    onChange={e => updateFila(idx, { nota: clamp(Number(e.target.value), 0, 5) })}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className={`text-xs font-medium ${sumaPorcentajes === 100 ? 'text-green-700' : 'text-red-600'}`}>
        Suma de porcentajes: {sumaPorcentajes}% {sumaPorcentajes === 100 ? '✓' : `— deben sumar 100%`}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Observaciones</label>
        <textarea
          className="input-field"
          rows={3}
          value={observaciones}
          onChange={e => setObservaciones(e.target.value)}
        />
      </div>

      {mensaje && (
        <div className={`text-sm rounded-lg px-4 py-3 border ${mensaje.includes('correctamente') ? 'bg-green-50 border-green-200 text-green-700' : 'bg-gray-50 border-gray-200 text-gray-700'}`}>
          {mensaje}
        </div>
      )}

      <button className="btn-primary" disabled={saving || sumaPorcentajes !== 100}>
        {saving ? 'Guardando...' : 'Guardar evaluacion final'}
      </button>
    </form>
  )
}
