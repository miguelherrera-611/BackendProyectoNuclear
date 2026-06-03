import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { EncuestaRespuestaForm } from '../../components/encuestas/EncuestaRespuestaForm'
import { sprint4Service } from '../../services/sprint4Service'
import type { EncuestaResponse } from '../../types'

export default function EncuestaPublicaPage() {
  const { token } = useParams()
  const [encuesta, setEncuesta] = useState<EncuestaResponse | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    sprint4Service.consultarEncuestaPublica(token)
      .then(setEncuesta)
      .catch(() => setError('No se pudo cargar la encuesta.'))
  }, [token])

  return (
    <div className="min-h-screen bg-gray-50 px-4 py-10">
      <div className="max-w-2xl mx-auto">
        {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}
        {!error && !encuesta && <div className="card text-center py-16 text-gray-400">Cargando encuesta...</div>}
        {encuesta && (
          <EncuestaRespuestaForm
            titulo={encuesta.titulo}
            preguntas={encuesta.preguntas}
            onCompletar={async (respuestas) => {
              await sprint4Service.completarEncuestaPublica(token ?? '', respuestas)
            }}
          />
        )}
      </div>
    </div>
  )
}
