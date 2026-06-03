import { useEffect, useState } from 'react'
import { sprint4Service } from '../../services/sprint4Service'
import type { EncuestaResponse } from '../../types'
import { EncuestaRespuestaForm } from './EncuestaRespuestaForm'

interface Props {
  titulo: string
  descripcion: string
  permiteBorradorTutor?: boolean
}

export function EncuestasAsignadasPanel({ titulo, descripcion, permiteBorradorTutor = false }: Props) {
  // SPRINT 4 - Decorator frontend: la misma lista base agrega comportamiento de borrador para tutor.
  const [encuestas, setEncuestas] = useState<EncuestaResponse[]>([])
  const [seleccionada, setSeleccionada] = useState<EncuestaResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const cargar = () => {
    setLoading(true)
    setError('')
    sprint4Service.misEncuestas()
      .then(setEncuestas)
      .catch(() => setError('No se pudieron cargar las encuestas.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  if (seleccionada) {
    return (
      <div className="max-w-3xl">
        <button className="btn-secondary mb-4" onClick={() => setSeleccionada(null)}>Volver</button>
        <EncuestaRespuestaForm
          titulo={seleccionada.titulo}
          preguntas={seleccionada.preguntas}
          respuestasIniciales={seleccionada.respuestas}
          permiteBorrador={permiteBorradorTutor && seleccionada.tipo === 'PARA_TUTOR'}
          onBorrador={async respuestas => {
            await sprint4Service.guardarBorradorEncuesta(seleccionada.id, respuestas)
            cargar()
          }}
          onCompletar={async respuestas => {
            await sprint4Service.completarEncuesta(seleccionada.id, respuestas)
            cargar()
            setSeleccionada(null)
          }}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{titulo}</h1>
        <p className="text-sm text-gray-500 mt-1">{descripcion}</p>
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      {loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" /></div>
      ) : encuestas.length === 0 ? (
        <div className="card text-center py-16 text-gray-400">No tienes encuestas asignadas.</div>
      ) : (
        <div className="space-y-3">
          {encuestas.map(enc => (
            <div key={enc.id} className="card flex items-center justify-between gap-4">
              <div>
                <p className="font-medium text-gray-900">{enc.titulo}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                  {enc.tipo.replace(/_/g, ' ')} - {enc.fechaEnvio ?? 'Sin fecha'}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <span className={enc.completada ? 'badge-apto' : 'badge-no-apto'}>{enc.estado.replace(/_/g, ' ')}</span>
                <button className="btn-primary text-xs" disabled={enc.completada} onClick={() => setSeleccionada(enc)}>
                  {enc.completada ? 'Cerrada' : 'Responder'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
