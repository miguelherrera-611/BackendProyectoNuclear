import { useState } from 'react'

interface Props {
  titulo: string
  preguntas: string[]
  respuestasIniciales?: string[]
  permiteBorrador?: boolean
  onCompletar: (respuestas: string[]) => Promise<void>
  onBorrador?: (respuestas: string[]) => Promise<void>
}

export function EncuestaRespuestaForm({ titulo, preguntas, respuestasIniciales, permiteBorrador, onCompletar, onBorrador }: Props) {
  // SPRINT 4 - State visual: permite borrador solo cuando el backend autoriza ese estado.
  const [respuestas, setRespuestas] = useState<string[]>(
    preguntas.map((_, idx) => respuestasIniciales?.[idx] ?? '')
  )
  const [saving, setSaving] = useState(false)
  const [mensaje, setMensaje] = useState('')

  const update = (idx: number, value: string) => {
    setRespuestas(prev => prev.map((r, i) => i === idx ? value : r))
  }

  const run = async (modo: 'borrador' | 'completar') => {
    setSaving(true)
    setMensaje('')
    try {
      if (modo === 'borrador' && onBorrador) await onBorrador(respuestas)
      if (modo === 'completar') await onCompletar(respuestas)
      setMensaje(modo === 'borrador' ? 'Borrador guardado.' : 'Encuesta completada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo enviar la encuesta.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="card space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{titulo}</h1>
        <p className="text-sm text-gray-500 mt-1">Responde cada pregunta antes de enviar.</p>
      </div>

      {preguntas.map((pregunta, idx) => (
        <div key={idx}>
          <label className="block text-sm font-medium text-gray-700 mb-1">{pregunta}</label>
          <textarea className="input-field" rows={3} value={respuestas[idx]} onChange={e => update(idx, e.target.value)} />
        </div>
      ))}

      {mensaje && <div className="text-sm bg-gray-50 border border-gray-200 rounded-lg px-4 py-3">{mensaje}</div>}

      <div className="flex flex-col sm:flex-row gap-3">
        {permiteBorrador && (
          <button className="btn-secondary flex-1" disabled={saving} onClick={() => run('borrador')}>Guardar borrador</button>
        )}
        <button className="btn-primary flex-1" disabled={saving} onClick={() => run('completar')}>
          {saving ? 'Enviando...' : 'Completar encuesta'}
        </button>
      </div>
    </div>
  )
}
