import { useState, useEffect, useCallback, useMemo } from 'react'
import type { EncuestaCoordinadorResumen } from '../../types'
import { sprint4Service, type EnviarEncuestaRequest } from '../../services/sprint4Service'
import { Button } from '../../components/common/Button/Button'
import { Modal } from '../../components/common/Modal/Modal'
import { Select } from '../../components/common/Select/Select'
import { useToast } from '../../components/common/Notifications/Toast'

const TITULO_DEFAULT = 'Encuesta de Satisfaccion 2026-I'
const PREGUNTAS_DEFAULT = [
  '¿Cómo evalúa el proceso de prácticas empresariales?',
  '¿Qué aspectos recomienda mejorar en el proceso?',
  '¿La empresa brindó las condiciones adecuadas para el desarrollo de las prácticas?',
]

type ModalEnvio = {
  open: boolean
  instanciaId: number
  nombrePractica: string
  nombreEstudiante: string
  tutorId?: number
  destino: 'tutor' | 'estudiante'
}

const MODAL_VACIO: ModalEnvio = {
  open: false, instanciaId: 0, nombrePractica: '', nombreEstudiante: '', tutorId: undefined, destino: 'tutor',
}

type FiltroEstado = 'todos' | 'pendiente_eval' | 'lista_enviar' | 'tutor_completada' | 'estudiante_completada' | 'ambas_completadas'

const ESTADO_OPCIONES: { value: FiltroEstado; label: string }[] = [
  { value: 'todos',               label: 'Todos los estados' },
  { value: 'pendiente_eval',      label: 'Pendiente eval. docente' },
  { value: 'lista_enviar',        label: 'Lista para enviar' },
  { value: 'tutor_completada',    label: 'Encuesta tutor completada' },
  { value: 'estudiante_completada', label: 'Encuesta estudiante completada' },
  { value: 'ambas_completadas',   label: 'Ambas encuestas completadas' },
]

function matchEstado(item: EncuestaCoordinadorResumen, filtro: FiltroEstado): boolean {
  switch (filtro) {
    case 'todos': return true
    case 'pendiente_eval': return !item.evaluacionDocenteCompleta
    case 'lista_enviar':
      return item.evaluacionDocenteCompleta &&
        (!item.encuestaTutorEnviada || !item.encuestaEstudianteEnviada)
    case 'tutor_completada': return item.encuestaTutorCompletada
    case 'estudiante_completada': return item.encuestaEstudianteCompletada
    case 'ambas_completadas': return item.encuestaTutorCompletada && item.encuestaEstudianteCompletada
  }
}

function EstadoBadge({ enviada, completada }: { enviada: boolean; completada: boolean }) {
  if (completada) return <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">Completada</span>
  if (enviada)    return <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-700">Enviada</span>
  return <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">Pendiente</span>
}

export default function EncuestasCoordinadorPage() {
  const { showToast } = useToast()
  const [lista, setLista] = useState<EncuestaCoordinadorResumen[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [modal, setModal] = useState<ModalEnvio>(MODAL_VACIO)
  const [titulo, setTitulo] = useState(TITULO_DEFAULT)
  const [preguntas, setPreguntas] = useState(PREGUNTAS_DEFAULT.join('\n'))
  const [errorModal, setErrorModal] = useState('')

  // Filtros
  const [busqueda, setBusqueda] = useState('')
  const [filtroPrograma, setFiltroPrograma] = useState('')
  const [filtroEmpresa, setFiltroEmpresa] = useState('')
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('todos')

  const cargar = useCallback(() => {
    setLoading(true)
    sprint4Service.listarEncuestasCoordinador()
      .then(setLista)
      .catch(() => showToast('No se pudo cargar la lista de prácticas.', 'error'))
      .finally(() => setLoading(false))
  }, [showToast])

  useEffect(() => { cargar() }, [cargar])

  // Opciones únicas para los dropdowns, derivadas de los datos
  const programas = useMemo(() =>
    [...new Set(lista.map(i => i.programaNombre).filter(Boolean) as string[])].sort(),
    [lista]
  )
  const empresas = useMemo(() =>
    [...new Set(lista.map(i => i.nombreEmpresa).filter(Boolean) as string[])].sort(),
    [lista]
  )

  const listaFiltrada = useMemo(() => {
    const texto = busqueda.toLowerCase().trim()
    return lista.filter(item => {
      if (filtroPrograma && item.programaNombre !== filtroPrograma) return false
      if (filtroEmpresa && item.nombreEmpresa !== filtroEmpresa) return false
      if (!matchEstado(item, filtroEstado)) return false
      if (texto) {
        const hayMatch = [
          item.nombrePractica,
          item.nombreEstudiante,
          item.nombreTutor,
          item.nombreDocenteAsesor,
          item.nombreEmpresa,
          item.programaNombre,
        ].some(campo => campo?.toLowerCase().includes(texto))
        if (!hayMatch) return false
      }
      return true
    })
  }, [lista, busqueda, filtroPrograma, filtroEmpresa, filtroEstado])

  const hayFiltros = busqueda || filtroPrograma || filtroEmpresa || filtroEstado !== 'todos'

  const limpiarFiltros = () => {
    setBusqueda('')
    setFiltroPrograma('')
    setFiltroEmpresa('')
    setFiltroEstado('todos')
  }

  const abrirModal = (item: EncuestaCoordinadorResumen, destino: 'tutor' | 'estudiante') => {
    setTitulo(TITULO_DEFAULT)
    setPreguntas(PREGUNTAS_DEFAULT.join('\n'))
    setErrorModal('')
    setModal({
      open: true,
      instanciaId: item.instanciaId,
      nombrePractica: item.nombrePractica,
      nombreEstudiante: item.nombreEstudiante,
      tutorId: item.tutorEmpresarialId,
      destino,
    })
  }

  const handleEnviar = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    const preguntasArr = preguntas.split('\n').map(p => p.trim()).filter(Boolean)
    if (preguntasArr.length === 0) { setErrorModal('Debes ingresar al menos una pregunta.'); return }
    setSaving(true)
    try {
      const req: EnviarEncuestaRequest = { titulo, preguntas: preguntasArr, tutorEmpresarialId: modal.tutorId }
      if (modal.destino === 'tutor') {
        await sprint4Service.enviarEncuestaTutor(modal.instanciaId, req)
      } else {
        await sprint4Service.enviarEncuestaEstudiante(modal.instanciaId, req)
      }
      showToast(`Encuesta enviada al ${modal.destino === 'tutor' ? 'tutor' : 'estudiante'} correctamente.`)
      setModal(MODAL_VACIO)
      cargar()
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al enviar la encuesta.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Encabezado */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Encuestas de Satisfacción</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Solo puedes enviar encuestas cuando el docente asesor haya registrado su evaluación final.
          </p>
        </div>
        <Button onClick={cargar} variant="secondary" disabled={loading}>Refrescar</Button>
      </div>

      {/* Panel de filtros */}
      <div className="card space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Búsqueda libre */}
          <div className="lg:col-span-1">
            <input
              type="text"
              placeholder="Buscar práctica, estudiante, tutor..."
              value={busqueda}
              onChange={e => setBusqueda(e.target.value)}
              className="input-field"
            />
          </div>

          {/* Filtro por programa */}
          <Select
            value={filtroPrograma}
            onChange={e => setFiltroPrograma(e.target.value)}
          >
            <option value="">Todos los programas</option>
            {programas.map(p => <option key={p} value={p}>{p}</option>)}
          </Select>

          {/* Filtro por empresa */}
          <Select
            value={filtroEmpresa}
            onChange={e => setFiltroEmpresa(e.target.value)}
          >
            <option value="">Todas las empresas</option>
            {empresas.map(e => <option key={e} value={e}>{e}</option>)}
          </Select>

          {/* Filtro por estado */}
          <Select
            value={filtroEstado}
            onChange={e => setFiltroEstado(e.target.value as FiltroEstado)}
          >
            {ESTADO_OPCIONES.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </Select>
        </div>

        {/* Resultado + limpiar */}
        <div className="flex items-center justify-between">
          <p className="text-xs text-gray-400">
            {loading ? 'Cargando...' : `${listaFiltrada.length} de ${lista.length} práctica${lista.length !== 1 ? 's' : ''}`}
          </p>
          {hayFiltros && (
            <button onClick={limpiarFiltros} className="text-xs text-cue-primary hover:underline">
              Limpiar filtros
            </button>
          )}
        </div>
      </div>

      {/* Contenido */}
      {loading ? (
        <div className="flex justify-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
        </div>
      ) : lista.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">📋</div>
          <p className="text-gray-400 text-sm">No hay prácticas EN CURSO en este momento.</p>
        </div>
      ) : listaFiltrada.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-400 text-sm">Ninguna práctica coincide con los filtros aplicados.</p>
          <button onClick={limpiarFiltros} className="mt-2 text-sm text-cue-primary hover:underline">
            Limpiar filtros
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {listaFiltrada.map(item => (
            <div key={item.instanciaId} className="card">
              <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                <div className="flex-1 min-w-0 space-y-0.5">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold text-gray-800">{item.nombrePractica}</h3>
                    <span className="text-xs text-gray-400">#{item.instanciaId}</span>
                    {item.programaNombre && (
                      <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                        {item.programaNombre}
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600">👤 {item.nombreEstudiante}</p>
                  {item.nombreEmpresa && (
                    <p className="text-sm text-gray-600">🏢 {item.nombreEmpresa}</p>
                  )}
                  {item.nombreDocenteAsesor && (
                    <p className="text-sm text-gray-600">🎓 {item.nombreDocenteAsesor}</p>
                  )}
                  {item.nombreTutor && (
                    <p className="text-sm text-gray-600">👔 Tutor: {item.nombreTutor}</p>
                  )}
                  {!item.evaluacionDocenteCompleta && (
                    <p className="text-xs text-amber-600 font-medium pt-1">
                      ⚠ El docente asesor aún no ha registrado su evaluación final
                    </p>
                  )}
                </div>

                <div className="flex flex-col gap-3 min-w-[260px]">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="text-sm text-gray-600 w-16">Tutor</span>
                      <EstadoBadge enviada={item.encuestaTutorEnviada} completada={item.encuestaTutorCompletada} />
                    </div>
                    <button
                      disabled={!item.evaluacionDocenteCompleta || item.encuestaTutorEnviada || !item.tutorEmpresarialId}
                      onClick={() => abrirModal(item, 'tutor')}
                      className="text-xs px-3 py-1.5 rounded-lg font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed bg-cue-primary text-white hover:opacity-90"
                      title={
                        !item.evaluacionDocenteCompleta ? 'Esperando evaluación del docente'
                        : !item.tutorEmpresarialId ? 'Sin tutor asignado'
                        : item.encuestaTutorEnviada ? 'Ya enviada'
                        : 'Enviar encuesta al tutor'
                      }
                    >
                      Enviar
                    </button>
                  </div>
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="text-sm text-gray-600 w-16">Estudiante</span>
                      <EstadoBadge enviada={item.encuestaEstudianteEnviada} completada={item.encuestaEstudianteCompletada} />
                    </div>
                    <button
                      disabled={!item.evaluacionDocenteCompleta || item.encuestaEstudianteEnviada}
                      onClick={() => abrirModal(item, 'estudiante')}
                      className="text-xs px-3 py-1.5 rounded-full font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed bg-cue-primary text-white hover:opacity-90"
                      title={
                        !item.evaluacionDocenteCompleta ? 'Esperando evaluación del docente'
                        : item.encuestaEstudianteEnviada ? 'Ya enviada'
                        : 'Enviar encuesta al estudiante'
                      }
                    >
                      Enviar
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal de envío */}
      {modal.open && (
        <Modal
          title={`Enviar encuesta al ${modal.destino === 'tutor' ? 'tutor empresarial' : 'estudiante'}`}
          subtitle={`${modal.nombrePractica} — ${modal.nombreEstudiante}`}
          onClose={() => setModal(MODAL_VACIO)}
        >
          {errorModal && (
            <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>
          )}
          <form onSubmit={handleEnviar} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Título de la encuesta <span className="text-red-500">*</span>
              </label>
              <input
                className="input-field"
                required
                value={titulo}
                onChange={e => setTitulo(e.target.value)}
                placeholder="Ej. Encuesta de Satisfacción 2026-I"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Preguntas <span className="text-red-500">*</span>
                <span className="text-gray-400 font-normal ml-1">(una por línea)</span>
              </label>
              <textarea
                className="input-field"
                rows={5}
                required
                value={preguntas}
                onChange={e => setPreguntas(e.target.value)}
                placeholder="Escribe cada pregunta en una línea diferente..."
              />
            </div>
            <div className="flex gap-3 pt-1">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModal(MODAL_VACIO)}>
                Cancelar
              </Button>
              <Button className="flex-1" type="submit" loading={saving}>
                Enviar encuesta
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
