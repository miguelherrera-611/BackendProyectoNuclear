import { useMemo, useState } from 'react'
import type { CriterioEvaluacion } from '../../types'

interface Props {
  titulo: string
  descripcion?: string
  onSubmit: (criterios: CriterioEvaluacion[], observaciones: string) => Promise<void>
}

const criteriosIniciales: CriterioEvaluacion[] = [
  { nombre: 'Desempeno tecnico', peso: 0.4, puntaje: 4.0 },
  { nombre: 'Trabajo en equipo', peso: 0.3, puntaje: 4.0 },
  { nombre: 'Responsabilidad', peso: 0.3, puntaje: 4.0 },
]

export function EvaluacionFinalForm({ titulo, descripcion, onSubmit }: Props) {
  // SPRINT 4 - Template Method visual: criterios -> promedio automatico -> envio al backend.
  const [criterios, setCriterios] = useState<CriterioEvaluacion[]>(criteriosIniciales)
  const [observaciones, setObservaciones] = useState('')
  const [saving, setSaving] = useState(false)
  const [mensaje, setMensaje] = useState('')

  const promedio = useMemo(
    () => Math.round(criterios.reduce((acc, c) => acc + c.peso * c.puntaje, 0) * 100) / 100,
    [criterios],
  )

  const sumaPesos = useMemo(
    () => Math.round(criterios.reduce((acc, c) => acc + Number(c.peso), 0) * 100) / 100,
    [criterios],
  )

  const update = (idx: number, patch: Partial<CriterioEvaluacion>) => {
    setCriterios(prev => prev.map((c, i) => i === idx ? { ...c, ...patch } : c))
  }

  const ejecutarGuardado = async () => {
    setMensaje('')
    setSaving(true)
    try {
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
          <p className="text-xs text-gray-400">Promedio calculado</p>
          <p className="text-3xl font-bold text-cue-primary">{promedio.toFixed(2)}</p>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-100">
            <tr>
              <th className="text-left px-3 py-2">Criterio</th>
              <th className="text-left px-3 py-2 w-28">Peso</th>
              <th className="text-left px-3 py-2 w-28">Puntaje</th>
            </tr>
          </thead>
          <tbody>
            {criterios.map((c, idx) => (
              <tr key={idx} className="border-b border-gray-100">
                <td className="px-3 py-2">
                  <input className="input-field" value={c.nombre} onChange={e => update(idx, { nombre: e.target.value })} />
                </td>
                <td className="px-3 py-2">
                  <input className="input-field" type="number" step="0.1" min={0} max={1} value={c.peso}
                    onChange={e => update(idx, { peso: Number(e.target.value) })} />
                </td>
                <td className="px-3 py-2">
                  <input className="input-field" type="number" step="0.1" min={0} max={5} value={c.puntaje}
                    onChange={e => update(idx, { puntaje: Number(e.target.value) })} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className={sumaPesos === 1 ? 'text-xs text-green-700' : 'text-xs text-red-700'}>
        Suma de pesos: {sumaPesos.toFixed(2)}
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Observaciones</label>
        <textarea className="input-field" rows={3} value={observaciones} onChange={e => setObservaciones(e.target.value)} />
      </div>

      {mensaje && <div className="text-sm bg-gray-50 border border-gray-200 rounded-lg px-4 py-3">{mensaje}</div>}

      <button className="btn-primary" disabled={saving || sumaPesos !== 1}>
        {saving ? 'Guardando...' : 'Guardar evaluacion final'}
      </button>
    </form>
  )
}
