import { useEffect, useRef, useState } from 'react'
import type { InstanciaPracticaResponseV2, Pageable } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'
import { sustentacionDocenteService } from '../../services/sustentacionDocenteService'
import { Pagination } from '../../components/common/Table/Pagination'

const TODAY = new Date().toISOString().split('T')[0]

function fmt(dateStr?: string): string {
  if (!dateStr) return '—'
  const [y, m, d] = dateStr.split('-')
  return `${d}/${m}/${y}`
}

function puedeSubirActa(p: InstanciaPracticaResponseV2): boolean {
  if (p.estado !== 'EN_CURSO' || !p.fechaSustentacion) return false
  // fecha sustentacion ya pasó o es hoy
  if (p.fechaSustentacion > TODAY) return false
  // todavía dentro del período de la práctica
  if (p.fechaFin && p.fechaFin <= TODAY) return false
  return true
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
  actaSubida: boolean
  subiendoActaId: number | null
  errorActa: string
  onSubirActa: (id: number, file: File) => void
}

function PracticaCard({
  p, agendandoId, fechaSeleccionada, savingId, errorAgenda,
  onAgendar, onCancelar, onFechaChange, onGuardar,
  actaSubida, subiendoActaId, errorActa, onSubirActa,
}: CardProps) {
  const esEnCurso = p.estado === 'EN_CURSO'
  const agendando = agendandoId === p.id
  const fileRef = useRef<HTMLInputElement>(null)
  const mostrarSubidaActa = puedeSubirActa(p)

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

      {/* Sección de acta firmada */}
      {mostrarSubidaActa && (
        <div className="border-t border-gray-100 pt-3 space-y-2">
          <p className="text-xs font-semibold text-gray-700 uppercase tracking-wide">Acta de sustentación firmada</p>
          {actaSubida ? (
            <div className="flex items-center gap-2 text-sm text-green-700 font-medium">
              <span>✓</span>
              <span>Acta subida correctamente.</span>
            </div>
          ) : (
            <>
              <p className="text-xs text-gray-500">
                La sustentación ya ocurrió. Sube el acta firmada antes del {fmt(p.fechaFin)} (fecha fin de práctica).
              </p>
              <div className="flex items-center gap-3">
                <input
                  ref={fileRef}
                  type="file"
                  accept=".pdf,.jpg,.jpeg,.png"
                  className="text-sm text-gray-600 file:mr-2 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-medium file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200 disabled:opacity-50"
                  disabled={subiendoActaId === p.id}
                  onChange={e => {
                    const file = e.target.files?.[0]
                    if (file) onSubirActa(p.id, file)
                  }}
                />
                {subiendoActaId === p.id && (
                  <span className="text-xs text-gray-500">Subiendo...</span>
                )}
              </div>
              {errorActa && subiendoActaId !== p.id && (
                <p className="text-xs text-red-600">{errorActa}</p>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}

export default function SustentacionesPage() {
  const [practicas, setPracticas]               = useState<InstanciaPracticaResponseV2[]>([])
  const [pagina, setPagina]                     = useState(0)
  const [pageData, setPageData]                 = useState<Pageable<InstanciaPracticaResponseV2> | null>(null)
  const [loading, setLoading]                   = useState(true)
  const [agendandoId, setAgendandoId]           = useState<number | null>(null)
  const [fechaSeleccionada, setFechaSeleccionada] = useState('')
  const [savingId, setSavingId]                 = useState<number | null>(null)
  const [errorAgenda, setErrorAgenda]           = useState('')
  const [actasSubidas, setActasSubidas]         = useState<Set<number>>(new Set())
  const [subiendoActaId, setSubiendoActaId]     = useState<number | null>(null)
  const [errorActa, setErrorActa]               = useState('')

  useEffect(() => {
    setLoading(true)
    seguimientoService.misPracticantesPaginado(pagina)
      .then(data => {
        setPracticas(data.content.filter(p => p.estado === 'EN_CURSO' || p.estado === 'FINALIZADA'))
        setPageData(data)
      })
      .finally(() => setLoading(false))
  }, [pagina])

  const enCurso     = practicas.filter(p => p.estado === 'EN_CURSO')
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

  const handleSubirActa = async (instanciaId: number, file: File) => {
    setSubiendoActaId(instanciaId)
    setErrorActa('')
    try {
      await sustentacionDocenteService.subirActa(instanciaId, file)
      setActasSubidas(prev => new Set([...prev, instanciaId]))
    } catch (e: unknown) {
      setErrorActa(
        (e as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
        ?? 'No se pudo subir el acta. Intenta de nuevo.'
      )
    } finally {
      setSubiendoActaId(null)
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
    subiendoActaId,
    errorActa,
    onSubirActa: handleSubirActa,
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Sustentaciones</h1>
        <p className="text-sm text-gray-500 mt-1">
          Programa las fechas de sustentación de tus practicantes. Tras la sustentación, sube el acta firmada para habilitar el cierre.
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
              {enCurso.map(p => (
                <PracticaCard
                  key={p.id}
                  p={p}
                  actaSubida={actasSubidas.has(p.id)}
                  {...cardProps}
                />
              ))}
            </div>
          )}
          {finalizadas.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
                Finalizadas ({finalizadas.length})
              </h2>
              {finalizadas.map(p => (
                <PracticaCard
                  key={p.id}
                  p={p}
                  actaSubida={actasSubidas.has(p.id)}
                  {...cardProps}
                />
              ))}
            </div>
          )}
        </>
      )}

      <Pagination
        page={pagina}
        totalPages={pageData?.totalPages ?? 0}
        totalElements={pageData?.totalElements}
        onPageChange={setPagina}
        disabled={loading}
      />
    </div>
  )
}
