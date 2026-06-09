import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { EvaluacionFinalForm } from '../../components/calificaciones/EvaluacionFinalForm'
import { sprint4Service } from '../../services/sprint4Service'
import { seguimientoService } from '../../services/seguimientoService'
import type { CriterioEvaluacion, SeguimientoSemanalResponse } from '../../types'
import { useAuth } from '../../context/AuthContext'

const MINIMO_REVISADOS = 3

export default function EvaluacionFinalPage() {
  const { instanciaId } = useParams()
  const { user } = useAuth()
  const id = Number(instanciaId)

  const [seguimientos, setSeguimientos] = useState<SeguimientoSemanalResponse[]>([])
  const [loadingSeguimientos, setLoadingSeguimientos] = useState(false)

  const esDocente = user?.rol === 'DOCENTE_ASESOR'

  useEffect(() => {
    if (!esDocente) return
    setLoadingSeguimientos(true)
    seguimientoService.listar(id)
      .then(setSeguimientos)
      .catch(() => setSeguimientos([]))
      .finally(() => setLoadingSeguimientos(false))
  }, [id, esDocente])

  const revisados = seguimientos.filter(s => s.estado === 'REVISADO').length
  const bloqueado = esDocente && revisados < MINIMO_REVISADOS

  const guardar = async (criterios: CriterioEvaluacion[], observaciones: string) => {
    if (user?.rol === 'TUTOR_EMPRESARIAL') {
      await sprint4Service.registrarEvaluacionTutor(id, { criterios, observaciones })
      return
    }
    await sprint4Service.registrarEvaluacionDocente(id, { criterios, observaciones })
  }

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Evaluacion final</h1>
        <p className="text-sm text-gray-500 mt-1">Registro unico antes del cierre formal.</p>
      </div>

      {esDocente && !loadingSeguimientos && (
        <div className={`card text-sm ${bloqueado ? 'border-amber-200 bg-amber-50 text-amber-800' : 'border-green-200 bg-green-50 text-green-800'}`}>
          {bloqueado
            ? `Se requieren al menos ${MINIMO_REVISADOS} seguimientos semanales en estado REVISADO para registrar la evaluación. Actualmente hay ${revisados}.`
            : `Condición cumplida: ${revisados} seguimiento${revisados !== 1 ? 's' : ''} revisado${revisados !== 1 ? 's' : ''}.`}
        </div>
      )}

      {!bloqueado && (
        <EvaluacionFinalForm
          titulo={user?.rol === 'TUTOR_EMPRESARIAL' ? 'Evaluacion del Tutor Empresarial' : 'Evaluacion del Docente Asesor'}
          descripcion="El promedio se calcula automaticamente con los pesos definidos."
          onSubmit={guardar}
        />
      )}
    </div>
  )
}
