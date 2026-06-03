import { useParams } from 'react-router-dom'
import { EvaluacionFinalForm } from '../../components/calificaciones/EvaluacionFinalForm'
import { sprint4Service } from '../../services/sprint4Service'
import type { CriterioEvaluacion } from '../../types'
import { useAuth } from '../../context/AuthContext'

export default function EvaluacionFinalPage() {
  const { instanciaId } = useParams()
  const { user } = useAuth()
  const id = Number(instanciaId)

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
      <EvaluacionFinalForm
        titulo={user?.rol === 'TUTOR_EMPRESARIAL' ? 'Evaluacion del Tutor Empresarial' : 'Evaluacion del Docente Asesor'}
        descripcion="El promedio se calcula automaticamente con los pesos definidos."
        onSubmit={guardar}
      />
    </div>
  )
}
