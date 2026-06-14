import { useEffect, useState } from 'react'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { sustentacionDocenteService } from '../../services/sustentacionDocenteService'

const TODAY = new Date().toISOString().split('T')[0]

function fmt(dateStr?: string): string {
  if (!dateStr) return '—'
  const [y, m, d] = dateStr.split('-')
  return `${d}/${m}/${y}`
}

interface CardProps {
  p: InstanciaPracticaResponseV2
  agendandoId: number | null
  fechaSeleccionada: string
  savingId: number | null
  errorAgenda: string
  onAgendar: (p: InstanciaPracticaResponseV2) => void
  onCancelar: () => void
  onFechaChange: (v: string) => void
  onGuardar: (id: number) => void
}

function PracticaCard({ p, agendandoId, fechaSeleccionada, savingId, errorAgenda, onAgendar, onCancelar, onFechaChange, onGuardar }: CardProps) {
  const esEnCurso = p.estado === 'EN_CURSO'
  const agendando = agendandoId === p.id

  return (
    <div className="card space-y-3">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-10 h-10 bg-cue-light rounded-xl flex items-center justify-center text-xl shrink-0">🎓</div>
          <div className="min-w-0">
            <p className="font-medium text-gray-800 truncate">{p.nombreEstudiante ?? 'Estudiante'}</p>
            <p className="text-sm text-gray-500 truncate">{p.nombre} · {p.razonSocialEmpresa ?? '—'}</p>
            {p.fechaFin && (
              <p className="text-xs text-gray-400 mt-0.5">
                Fecha límite práctica: <strong>{fmt(p.fechaFin)}</strong>
              </p>
            )}
          </div>
        </div>
        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full shrink-0 whitespace-nowrap ${
          esEnCurso ? 'bg-green-100 text-green-800' : 'bg-blue-100 text-blue-800'
        }`}>
          {esEnCurso ? 'En curso' : 'Finalizada'}
        </span>
      </div>

      {agendando ? (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 space-y-3">
          <p className="text-sm font-semibold text-blue-800">
            {p.fechaSustentacion ? 'Reagendar sustentación' : 'Programar sustentación'}
          </p>
          <div>
            <label className="block text-xs text-gray-600 mb-1">Fecha de sustentación *</label>
            <input
              type="date"
              className="input-field"
              value={fechaSeleccionada}
              min={TODAY}
              max={p.fechaFin ?? undefined}
              onChange={e => onFechaChange(e.target.value)}
            />
            {p.fechaFin && (
              <p className="text-xs text-gray-500 mt-1">
                La fecha debe estar dentro del período de práctica (máximo {fmt(p.fechaFin)}).
              </p>
            )}
          </div>
          {errorAgenda && <p className="text-sm text-red-600">{errorAgenda}</p>}
          <div className="flex gap-2">
            <button className="btn-secondary flex-1 text-sm py-2" onClick={onCancelar}>
              Cancelar
            </button>
            <button
              className="btn-primary flex-1 text-sm py-2"
              disabled={!fechaSeleccionada || savingId === p.id}
              onClick={() => onGuardar(p.id)}
            >
              {savingId === p.id ? 'Guardando...' : 'Confirmar fecha'}
            </button>
          </div>
        </div>
      ) : p.fechaSustentacion ? (
        <div className="bg-green-50 border border-green-200 rounded-lg px-4 py-3 flex items-center justify-between gap-4">
          <div>
            <p className="text-xs font-medium text-green-700 uppercase tracking-wide">Sustentación programada</p>
            <p className="text-xl font-bold text-green-800 mt-0.5">📅 {fmt(p.fechaSustentacion)}</p>
          </div>
          {esEnCurso && (
            <button
              onClick={() => onAgendar(p)}
              className="text-sm text-green-700 hover:underline shrink-0"
            >
              Reagendar
            </button>
          )}
        </div>
      ) : esEnCurso ? (
        <div className="bg-amber-50 border border-amber-200 rounded-lg px-4 py-3 flex items-center justify-between gap-4">
          <p className="text-sm text-amber-700">Sin fecha de sustentación programada</p>
          <button
            onClick={() => onAgendar(p)}
            className="btn-primary text-sm px-4 py-1.5 shrink-0"
          >
            Agendar fecha
          </button>
        </div>
      ) : (
        <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 py-3">
          <p className="text-sm text-gray-400 italic">Sin sustentación registrada en esta práctica.</p>
        </div>
      )}
    </div>
  )
}

export default function SustentacionesPage() {
  const [practicas, setPracticas]           = useState<InstanciaPracticaResponseV2[]>([])
  const [loading, setLoading]               = useState(true)
  const [agendandoId, setAgendandoId]       = useState<number | null>(null)
  const [fechaSeleccionada, setFechaSeleccionada] = useState('')
  const [savingId, setSavingId]             = useState<number | null>(null)
  const [errorAgenda, setErrorAgenda]       = useState('')

  useEffect(() => {
    seguimientoService.misPracticantes()
      .then(lista => setPracticas(lista.filter(p => p.estado === 'EN_CURSO' || p.estado === 'FINALIZADA')))
      .finally(() => setLoading(false))
  }, [])

  const enCurso    = practicas.filter(p => p.estado === 'EN_CURSO')
  const finalizadas = practicas.filter(p => p.estado === 'FINALIZADA')

  const handleAgendar = (p: InstanciaPracticaResponseV2) => {
    setAgendandoId(p.id)
    setFechaSeleccionada(p.fechaSustentacion ?? '')
    setErrorAgenda('')
  }

  const handleCancelar = () => {
    setAgendandoId(null)
    setFechaSeleccionada('')
    setErrorAgenda('')
  }

  const handleGuardar = async (instanciaId: number) => {
    if (!fechaSeleccionada) { setErrorAgenda('Selecciona una fecha.'); return }
    setSavingId(instanciaId)
    setErrorAgenda('')
    try {
      const actualizada = await sustentacionDocenteService.agendar(instanciaId, fechaSeleccionada)
      setPracticas(prev =>
        prev.map(p => p.id === instanciaId ? { ...p, fechaSustentacion: actualizada.fechaSustentacion } : p)
      )
      setAgendandoId(null)
      setFechaSeleccionada('')
    } catch (e: unknown) {
      setErrorAgenda(
        (e as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
        ?? 'No se pudo guardar la fecha de sustentación.'
      )
    } finally {
      setSavingId(null)
    }
  }

  const cardProps = {
    agendandoId,
    fechaSeleccionada,
    savingId,
    errorAgenda,
    onAgendar: handleAgendar,
    onCancelar: handleCancelar,
    onFechaChange: (v: string) => { setFechaSeleccionada(v); setErrorAgenda('') },
    onGuardar: handleGuardar,
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Sustentaciones</h1>
        <p className="text-sm text-gray-500 mt-1">
          Programa las fechas de sustentación de tus practicantes. Al confirmar, se notificará al estudiante y al tutor empresarial por correo.
        </p>
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
        </div>
      ) : practicas.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">🎓</div>
          <p className="text-gray-500 text-sm">No tienes practicantes asignados aún.</p>
        </div>
      ) : (
        <>
          {enCurso.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
                En curso — programar sustentación ({enCurso.length})
              </h2>
              {enCurso.map(p => <PracticaCard key={p.id} p={p} {...cardProps} />)}
            </div>
          )}
          {finalizadas.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
                Finalizadas ({finalizadas.length})
              </h2>
              {finalizadas.map(p => <PracticaCard key={p.id} p={p} {...cardProps} />)}
            </div>
          )}
        </>
      )}
    </div>
  )
}
