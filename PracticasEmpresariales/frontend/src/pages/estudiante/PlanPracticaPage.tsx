import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
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
  const { instanciaId } = useParams<{ instanciaId: string }>()
  const [practica, setPractica] = useState<InstanciaPracticaResponse | null>(null)
  const [plan, setPlan] = useState<PlanPracticaResponse | null>(null)
  const [objetivos, setObjetivos] = useState('')
  const [cronograma, setCronograma] = useState('')
  const [documento, setDocumento] = useState<File | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  const esEstudiante = user?.rol === 'ESTUDIANTE'
  const esDocente = user?.rol === 'DOCENTE_ASESOR'
  const esTutor = user?.rol === 'TUTOR_EMPRESARIAL'

  useEffect(() => {
    const init = async () => {
      try {
        const p = instanciaId
          ? await seguimientoService.obtenerInstancia(Number(instanciaId))
          : await seguimientoService.miPractica()
        setPractica(p)
        const planData = await planPracticaService.obtenerActual(p.id)
        if (planData) {
          setPlan(planData)
          setObjetivos(planData.objetivos ?? '')
          setCronograma(planData.cronograma ?? '')
        }
      } catch {
        setError('No se pudo cargar la práctica activa.')
      } finally {
        setLoading(false)
      }
    }
    init()
  }, [instanciaId])

  const handleGuardar = async () => {
    if (!practica) return
    if (!documento && !objetivos.trim() && !cronograma.trim()) {
      setError('Debes subir un documento o completar al menos uno de los campos de texto.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const planData = await planPracticaService.crearOActualizar(practica.id, { objetivos, cronograma }, documento)
      setPlan(planData)
      setDocumento(null)
      if (fileInputRef.current) fileInputRef.current.value = ''
      setSuccess('Plan guardado correctamente.')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al guardar el plan.')
    } finally {
      setSaving(false)
    }
  }

  const handleDescargar = async () => {
    if (!plan?.documentoNombre) return
    setDownloading(true)
    try {
      await planPracticaService.descargarDocumento(plan.id, plan.documentoNombre)
    } catch {
      setError('No se pudo descargar el documento.')
    } finally {
      setDownloading(false)
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

  const puedeEditar = esEstudiante && (!plan || plan.estado === 'RECHAZADO' || plan.estado === 'BORRADOR')
  const tieneDocumento = !!plan?.documentoNombre

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

      {/* Formulario de carga (solo estudiante en estado editable) */}
      {puedeEditar && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">{plan ? 'Editar plan' : 'Subir plan de práctica'}</h2>

          {/* Carga de documento */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Documento del plan <span className="text-gray-400 font-normal">(PDF, DOC, DOCX — máx. 10 MB)</span>
            </label>
            <div className="flex items-center gap-3">
              <label className="cursor-pointer inline-flex items-center gap-2 px-4 py-2 rounded-lg border border-gray-300 bg-white text-sm text-gray-700 hover:bg-gray-50 transition-colors">
                <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                </svg>
                {documento ? 'Cambiar archivo' : 'Seleccionar archivo'}
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf,.doc,.docx"
                  className="hidden"
                  onChange={e => setDocumento(e.target.files?.[0] ?? null)}
                />
              </label>
              {documento && (
                <span className="text-sm text-gray-600 truncate max-w-xs">{documento.name}</span>
              )}
            </div>
            {tieneDocumento && !documento && (
              <p className="text-xs text-gray-500 mt-1.5 flex items-center gap-1.5">
                Documento actual:
                <button
                  onClick={handleDescargar}
                  disabled={downloading}
                  className="text-blue-600 hover:underline font-medium"
                >
                  {downloading ? 'Descargando...' : plan!.documentoNombre}
                </button>
                <span className="text-gray-400">(sube uno nuevo para reemplazarlo)</span>
              </p>
            )}
          </div>

          {/* Campos de texto opcionales */}
          <div className="border-t border-gray-100 pt-3 space-y-3">
            <p className="text-xs text-gray-500">
              Opcionalmente puedes agregar texto complementario al documento.
            </p>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Objetivos</label>
              <textarea
                className="input-field"
                rows={3}
                placeholder="Describe los objetivos de tu práctica..."
                value={objetivos}
                onChange={e => setObjetivos(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Cronograma</label>
              <textarea
                className="input-field"
                rows={4}
                placeholder="Describe el cronograma de actividades semana a semana..."
                value={cronograma}
                onChange={e => setCronograma(e.target.value)}
              />
            </div>
          </div>

          <button className="btn-primary w-full" onClick={handleGuardar} disabled={saving}>
            {saving ? 'Guardando...' : (plan ? 'Actualizar y re-enviar' : 'Guardar plan')}
          </button>
        </div>
      )}

      {/* Vista del plan para docente / tutor / estudiante en solo lectura */}
      {plan && (plan.estado === 'APROBADO_DOCENTE' || plan.estado === 'APROBADO_TUTOR' || plan.estado === 'BORRADOR') && (
        <div className="card space-y-3">
          <h2 className="font-semibold text-gray-800">Contenido del plan</h2>

          {tieneDocumento && (
            <div className="flex items-center gap-3 p-3 bg-blue-50 rounded-lg border border-blue-100">
              <svg className="w-8 h-8 text-blue-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-800 truncate">{plan.documentoNombre}</p>
                <p className="text-xs text-gray-500">Documento adjunto del plan</p>
              </div>
              <button
                onClick={handleDescargar}
                disabled={downloading}
                className="shrink-0 text-sm text-blue-600 font-medium hover:underline disabled:opacity-50"
              >
                {downloading ? 'Descargando...' : 'Descargar'}
              </button>
            </div>
          )}

          {plan.objetivos && (
            <div>
              <p className="text-sm font-medium text-gray-700">Objetivos</p>
              <p className="text-sm text-gray-600 mt-1 whitespace-pre-wrap">{plan.objetivos}</p>
            </div>
          )}
          {plan.cronograma && (
            <div>
              <p className="text-sm font-medium text-gray-700">Cronograma</p>
              <p className="text-sm text-gray-600 mt-1 whitespace-pre-wrap">{plan.cronograma}</p>
            </div>
          )}

          <div className="flex gap-2 border-t border-gray-100 pt-3">
            {esTutor && plan.estado === 'BORRADOR' && (
              <>
                <button className="btn-primary flex-1" onClick={handleAprobarTutor} disabled={saving}>Aprobar como tutor</button>
                <button className="btn-secondary flex-1 text-red-600" onClick={handleRechazar} disabled={saving}>Rechazar</button>
              </>
            )}
            {esDocente && (plan.estado === 'APROBADO_TUTOR' || plan.estado === 'BORRADOR') && (
              <>
                <button className="btn-primary flex-1" onClick={handleAprobarDocente} disabled={saving}>Aprobar plan</button>
                <button className="btn-secondary flex-1 text-red-600" onClick={handleRechazar} disabled={saving}>Rechazar</button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
