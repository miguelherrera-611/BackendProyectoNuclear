import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { SeguimientoSemanalResponse, InstanciaPracticaResponse, PlanPracticaResponse, EstadoSeguimiento } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { planPracticaService } from '../../services/planPracticaService'

const BADGE: Record<EstadoSeguimiento, string> = {
  ENVIADO:   'bg-blue-100 text-blue-800',
  REVISADO:  'bg-green-100 text-green-800',
  RECHAZADO: 'bg-red-100 text-red-800',
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  APROBADO:  'bg-green-100 text-green-800',
}

export default function SeguimientoEstudiantePage() {
  const navigate = useNavigate()
  const [practica, setPractica] = useState<InstanciaPracticaResponse | null>(null)
  const [plan, setPlan] = useState<PlanPracticaResponse | null>(null)
  const [seguimientos, setSeguimientos] = useState<SeguimientoSemanalResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [saving, setSaving] = useState(false)

  // Formulario nuevo seguimiento
  const [creando, setCreando] = useState(false)
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [semana, setSemana] = useState(1)
  const [actividades, setActividades] = useState('')
  const [logros, setLogros] = useState('')
  const [dificultades, setDificultades] = useState('')

  const ultimoSeguimiento = seguimientos[seguimientos.length - 1]
  const congelada = practica?.evaluacionDocenteRegistrada === true

  useEffect(() => {
    const init = async () => {
      try {
        const p = await seguimientoService.miPractica()
        setPractica(p)
        const [segs, planData] = await Promise.all([
          seguimientoService.listar(p.id),
          planPracticaService.obtenerActual(p.id),
        ])
        setSeguimientos(segs)
        setPlan(planData)
        setSemana(segs.length + 1)
      } catch {
        setError('No se pudieron cargar los datos de tu práctica.')
      } finally {
        setLoading(false)
      }
    }
    init()
  }, [])

  const resetForm = () => {
    setActividades('')
    setLogros('')
    setDificultades('')
    setCreando(false)
    setEditandoId(null)
  }

  const handleCrear = async () => {
    if (!practica || !actividades.trim() || !logros.trim()) {
      setError('Actividades y logros son obligatorios.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const nuevo = await seguimientoService.crear(practica.id, { semana, actividades, logros, dificultades })
      setSeguimientos(prev => [...prev, nuevo])
      setSemana(prev => prev + 1)
      resetForm()
      setSuccess('Seguimiento registrado correctamente.')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al registrar el seguimiento.')
    } finally {
      setSaving(false)
    }
  }

  const handleEditar = async () => {
    if (!editandoId || !actividades.trim() || !logros.trim()) {
      setError('Actividades y logros son obligatorios.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const actualizado = await seguimientoService.editar(editandoId, { semana, actividades, logros, dificultades })
      setSeguimientos(prev => prev.map(s => s.id === editandoId ? actualizado : s))
      resetForm()
      setSuccess('Seguimiento actualizado y re-enviado.')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al actualizar el seguimiento.')
    } finally {
      setSaving(false)
    }
  }

  const abrirEdicion = (seg: SeguimientoSemanalResponse) => {
    setEditandoId(seg.id)
    setSemana(seg.semana)
    setActividades(seg.actividades)
    setLogros(seg.logros)
    setDificultades(seg.dificultades ?? '')
    setCreando(false)
  }

  if (loading) return <div className="flex justify-center py-16 text-gray-400">Cargando...</div>

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div>
        <button className="text-sm text-gray-500 hover:text-gray-700 mb-2" onClick={() => navigate('/mi-practica')}>
          ← Volver a mi práctica
        </button>
        <h1 className="text-2xl font-bold text-gray-900">Seguimientos semanales</h1>
        {practica && <p className="text-sm text-gray-500">{practica.nombre} · {practica.duracionSemanas} semanas totales</p>}
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}
      {success && <div className="card border-green-200 bg-green-50 text-green-700 text-sm">{success}</div>}

      {congelada && (
        <div className="card border-purple-200 bg-purple-50 text-purple-800 text-sm">
          <strong>Práctica calificada.</strong> El docente asesor ya registró la evaluación final. Esta práctica está en modo solo lectura — no puedes enviar ni editar seguimientos.
        </div>
      )}

      {!congelada && plan?.estado !== 'APROBADO_DOCENTE' && (
        <div className="card border-amber-200 bg-amber-50 text-amber-800 text-sm">
          {plan
            ? 'Tu plan de práctica aún no ha sido aprobado por el docente. Podrás registrar seguimientos una vez que el docente lo apruebe.'
            : 'No tienes un plan de práctica registrado. Debes crear y obtener la aprobación del docente antes de registrar seguimientos.'}
        </div>
      )}

      {/* Formulario crear/editar */}
      {!congelada && (creando || editandoId !== null) && (
        <div className="card space-y-4 border-cue-primary border-2">
          <h2 className="font-semibold text-gray-800">
            {editandoId ? `Editar seguimiento semana ${semana}` : `Nuevo seguimiento — Semana ${semana}`}
          </h2>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Actividades realizadas *</label>
            <textarea className="input-field" rows={3} value={actividades} onChange={e => setActividades(e.target.value)} placeholder="Describe las actividades realizadas esta semana..." />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Logros *</label>
            <textarea className="input-field" rows={3} value={logros} onChange={e => setLogros(e.target.value)} placeholder="¿Qué lograste esta semana?" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Dificultades encontradas</label>
            <textarea className="input-field" rows={2} value={dificultades} onChange={e => setDificultades(e.target.value)} placeholder="Opcional: describe las dificultades..." />
          </div>
          <div className="flex gap-2">
            <button className="btn-secondary flex-1" onClick={resetForm}>Cancelar</button>
            <button className="btn-primary flex-1" onClick={editandoId ? handleEditar : handleCrear} disabled={saving}>
              {saving ? 'Guardando...' : (editandoId ? 'Actualizar y re-enviar' : 'Enviar seguimiento')}
            </button>
          </div>
        </div>
      )}

      {/* Botón nuevo seguimiento — oculto si la práctica está congelada */}
      {!congelada && !creando && !editandoId && (
        <button
          className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={() => setCreando(true)}
          disabled={
            plan?.estado !== 'APROBADO_DOCENTE' ||
            ultimoSeguimiento?.estado === 'ENVIADO' ||
            ultimoSeguimiento?.estado === 'PENDIENTE'
          }
          title={
            plan?.estado !== 'APROBADO_DOCENTE'
              ? 'El plan debe estar aprobado por el docente para poder registrar seguimientos.'
              : (ultimoSeguimiento?.estado === 'ENVIADO' || ultimoSeguimiento?.estado === 'PENDIENTE')
                ? 'Espera a que el docente revise el seguimiento actual antes de enviar el siguiente.'
                : ''
          }
        >
          + Registrar seguimiento semana {semana}
        </button>
      )}

      {/* Historial */}
      <div className="space-y-4">
        {seguimientos.length === 0 ? (
          <div className="card text-center text-gray-400">Aún no has registrado seguimientos.</div>
        ) : [...seguimientos].reverse().map((s, idx) => {
          const esUltimo = idx === 0
          const puedeEditar = !congelada && esUltimo && s.estado === 'RECHAZADO'
          return (
            <div key={s.id} className="card space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-gray-900">Semana {s.semana}</h3>
                <div className="flex items-center gap-2">
                  <span className={`text-xs font-medium px-2 py-1 rounded-full ${BADGE[s.estado]}`}>{s.estado}</span>
                  {puedeEditar && (
                    <button className="btn-secondary text-xs" onClick={() => abrirEdicion(s)}>Editar y re-enviar</button>
                  )}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="font-medium text-gray-700">Actividades</p>
                  <p className="text-gray-600 mt-1">{s.actividades}</p>
                </div>
                <div>
                  <p className="font-medium text-gray-700">Logros</p>
                  <p className="text-gray-600 mt-1">{s.logros}</p>
                </div>
              </div>
              {s.observacionesDocente && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-800">
                  <strong>Observación del docente:</strong> {s.observacionesDocente}
                </div>
              )}
              <p className="text-xs text-gray-400">Registrado: {new Date(s.creadoEn).toLocaleDateString('es-CO')}</p>
            </div>
          )
        })}
      </div>
    </div>
  )
}
