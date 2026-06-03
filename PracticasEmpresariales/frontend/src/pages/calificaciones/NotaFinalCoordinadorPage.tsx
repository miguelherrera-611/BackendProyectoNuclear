import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { sprint4Service } from '../../services/sprint4Service'
import type { EvaluacionFinalResponse, NotaFinalResponse } from '../../types'

export default function NotaFinalCoordinadorPage() {
  const { instanciaId } = useParams()
  const id = Number(instanciaId)
  const [referencias, setReferencias] = useState<Record<string, EvaluacionFinalResponse | null>>({})
  const [notaFinal, setNotaFinal] = useState(3.5)
  const [observaciones, setObservaciones] = useState('')
  const [resultado, setResultado] = useState<NotaFinalResponse | null>(null)
  const [mensaje, setMensaje] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    sprint4Service.referencias(id).then(setReferencias).catch(() => setMensaje('No se pudieron cargar referencias.'))
  }, [id])

  const guardar = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setMensaje('')
    try {
      const res = await sprint4Service.registrarNotaFinal(id, notaFinal, observaciones)
      setResultado(res)
      setMensaje('Nota final registrada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo registrar la nota final.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Nota final del Coordinador</h1>
        <p className="text-sm text-gray-500 mt-1">Consulta las referencias y registra la nota definitiva.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {(['docente', 'tutor'] as const).map(key => {
          const ref = referencias[key]
          return (
            <div key={key} className="card">
              <p className="text-sm text-gray-500 capitalize">Referencia {key}</p>
              <p className="text-3xl font-bold text-gray-900 mt-2">{ref ? ref.promedioFinal.toFixed(2) : '---'}</p>
              <p className="text-xs text-gray-400 mt-1">{ref?.evaluadorNombre ?? 'Pendiente'}</p>
            </div>
          )
        })}
      </div>

      <form onSubmit={guardar} className="card space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Nota final</label>
          <input className="input-field" type="number" min={0} max={5} step="0.1" value={notaFinal}
            onChange={e => setNotaFinal(Number(e.target.value))} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Observaciones</label>
          <textarea className="input-field" rows={3} value={observaciones} onChange={e => setObservaciones(e.target.value)} />
        </div>
        {resultado && (
          <div className={resultado.resultado === 'APROBADO' ? 'badge-apto inline-block' : 'badge-no-apto inline-block'}>
            {resultado.resultado} con minima {resultado.notaMinimaAplicada.toFixed(1)}
          </div>
        )}
        {mensaje && <div className="text-sm bg-gray-50 border border-gray-200 rounded-lg px-4 py-3">{mensaje}</div>}
        <button className="btn-primary" disabled={saving}>{saving ? 'Guardando...' : 'Registrar nota final'}</button>
      </form>
    </div>
  )
}
