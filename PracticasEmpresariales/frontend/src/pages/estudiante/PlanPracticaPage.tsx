import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { PlanPracticaResponse, InstanciaPracticaResponse, EstadoPlan } from '../../types'
import { planPracticaService } from '../../services/planPracticaService'
import { seguimientoService } from '../../services/seguimientoService'
import { useAuth } from '../../context/AuthContext'

const BADGE: Record<EstadoPlan, string> = {
  BORRADOR: 'bg-gray-100 text-gray-700',
  APROBADO_TUTOR: 'bg-blue-100 text-blue-700',
  APROBADO_DOCENTE: 'bg-green-100 text-green-800',
  RECHAZADO: 'bg-red-100 text-red-700',
}

export default function PlanPracticaPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [practica, setPractica] = useState<InstanciaPracticaResponse | null>(null)
  const [plan, setPlan] = useState<PlanPracticaResponse | null>(null)
  const [objetivos, setObjetivos] = useState('')
  const [cronograma, setCronograma] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const esEstudiante = user?.rol === 'ESTUDIANTE'
  const esDocente = user?.rol === 'DOCENTE_ASESOR'
  const esTutor = user?.rol === 'TUTOR_EMPRESARIAL'

  useEffect(() => {
    const init = async () => {
      try {
        const p = await seguimientoService.miPractica()
        setPractica(p)
        try {
          const planData = await planPracticaService.obtenerActual(p.id)
          setPlan(planData)
          setObjetivos(planData.objetivos)
          setCronograma(planData.cronograma)
        } catch {
          // No plan yet
        }
      } catch {
        setError('No se pudo cargar la práctica activa.')
      } finally {
        setLoading(false)
      }
    }
    init()
  }, [])

  const handleGuardar = async () => {
    if (!practica) return
    if (!objetivos.trim() || !cronograma.trim()) {
      setError('Los objetivos y el cronograma son obligatorios.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const planData = await planPracticaService.crearOActualizar(practica.id, { objetivos, cronograma })
      setPlan(planData)
      setSuccess('Plan guardado correctamente.')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al guardar el plan.')
    } finally {
      setSaving(false)
    }
  }

  const handleAprobarTutor = async () => {
    if (!plan) return
    setSaving(true)
    try {
      const updated = await planPracticaService.aprobarTutor(plan.id)
      setPlan(updated)
      setSuccess('Plan aprobado como tutor.')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al aprobar.')
    } finally {
      setSaving(false)
    }
  }

  const handleAprobarDocente = async () => {
    if (!plan) return
    setSaving(true)
    try {
      const updated = await planPracticaService.aprobarDocente(plan.id)
      setPlan(updated)
      setSuccess('Plan aprobado. El estudiante puede iniciar seguimientos.')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al aprobar.')
    } finally {
      setSaving(false)
    }
  }

  const handleRechazar = async () => {
    if (!plan) return
    const motivo = window.prompt('Motivo de rechazo (obligatorio):')?.trim()
    if (!motivo) return
    setSaving(true)
    try {
      const updated = await planPracticaService.rechazar(plan.id, motivo)
      setPlan(updated)
      setSuccess('Plan rechazado. El estudiante podrá corregirlo.')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al rechazar.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="flex justify-center py-16 text-gray-400">Cargando...</div>

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div>
        {esEstudiante && (
          <button className="text-sm text-gray-500 hover:text-gray-700 mb-2" onClick={() => navigate('/mi-practica')}>
            ← Volver a mi práctica
          </button>
        )}
        <h1 className="text-2xl font-bold text-gray-900">Plan de práctica</h1>
        {practica && <p className="text-sm text-gray-500">{practica.nombre} · {practica.razonSocialEmpresa}</p>}
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}
      {success && <div className="card border-green-200 bg-green-50 text-green-700 text-sm">{success}</div>}

      {plan && (
        <div className="card flex items-center justify-between">
          <div>
            <span className="text-sm font-medium text-gray-700">Estado del plan:</span>
            <span className={`ml-2 text-xs font-medium px-2 py-1 rounded-full ${BADGE[plan.estado]}`}>
              {plan.estado.replace(/_/g, ' ')}
            </span>
          </div>
          {plan.motivoRechazo && (
            <div className="text-sm text-red-600">Motivo rechazo: {plan.motivoRechazo}</div>
          )}
        </div>
      )}

      {(esEstudiante && (!plan || plan.estado === 'RECHAZADO' || plan.estado === 'BORRADOR')) && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">{plan ? 'Editar plan' : 'Crear plan de práctica'}</h2>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Objetivos *</label>
            <textarea className="input-field" rows={4} placeholder="Describe los objetivos de tu práctica..." value={objetivos} onChange={e => setObjetivos(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Cronograma *</label>
            <textarea className="input-field" rows={6} placeholder="Describe el cronograma de actividades semana a semana..." value={cronograma} onChange={e => setCronograma(e.target.value)} />
          </div>
          <button className="btn-primary w-full" onClick={handleGuardar} disabled={saving}>
            {saving ? 'Guardando...' : (plan ? 'Actualizar y re-enviar' : 'Guardar plan')}
          </button>
        </div>
      )}

      {plan && (plan.estado === 'APROBADO_DOCENTE' || plan.estado === 'APROBADO_TUTOR' || plan.estado === 'BORRADOR') && (
        <div className="card space-y-3">
          <h2 className="font-semibold text-gray-800">Contenido del plan</h2>
          <div>
            <p className="text-sm font-medium text-gray-700">Objetivos</p>
            <p className="text-sm text-gray-600 mt-1 whitespace-pre-wrap">{plan.objetivos}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-700">Cronograma</p>
            <p className="text-sm text-gray-600 mt-1 whitespace-pre-wrap">{plan.cronograma}</p>
          </div>

          <div className="flex gap-2 border-t border-gray-100 pt-3">
            {esTutor && plan.estado === 'BORRADOR' && (
              <>
                <button className="btn-primary flex-1" onClick={handleAprobarTutor} disabled={saving}>Aprobar como tutor</button>
                <button className="btn-secondary flex-1 text-red-600" onClick={handleRechazar} disabled={saving}>Rechazar</button>
              </>
            )}
            {esDocente && plan.estado === 'APROBADO_TUTOR' && (
              <>
                <button className="btn-primary flex-1" onClick={handleAprobarDocente} disabled={saving}>Aprobar como docente</button>
                <button className="btn-secondary flex-1 text-red-600" onClick={handleRechazar} disabled={saving}>Rechazar</button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
