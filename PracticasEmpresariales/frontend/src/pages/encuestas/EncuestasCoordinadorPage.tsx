import { useState, useEffect, useCallback } from 'react'
import type { EncuestaCoordinadorResumen } from '../../types'
import { sprint4Service, type EnviarEncuestaRequest } from '../../services/sprint4Service'
import { Button } from '../../components/common/Button/Button'
import { Modal } from '../../components/common/Modal/Modal'
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
  open: false,
  instanciaId: 0,
  nombrePractica: '',
  nombreEstudiante: '',
  tutorId: undefined,
  destino: 'tutor',
}

function EstadoBadge({ enviada, completada }: { enviada: boolean; completada: boolean }) {
  if (completada) return <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">Completada</span>
  if (enviada) return <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-700">Enviada</span>
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

  const cargar = useCallback(() => {
    setLoading(true)
    sprint4Service.listarEncuestasCoordinador()
      .then(setLista)
      .catch(() => showToast('No se pudo cargar la lista de prácticas.', 'error'))
      .finally(() => setLoading(false))
  }, [showToast])

  useEffect(() => { cargar() }, [cargar])

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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Encuestas de Satisfacción</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Solo puedes enviar encuestas cuando el docente asesor haya registrado su evaluación final.
          </p>
        </div>
        <Button onClick={cargar} variant="secondary" disabled={loading}>Refrescar</Button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
        </div>
      ) : lista.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">📋</div>
          <p className="text-gray-400 text-sm">No hay prácticas EN CURSO en este momento.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {lista.map(item => (
            <div key={item.instanciaId} className="card">
              <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold text-gray-800">{item.nombrePractica}</h3>
                    <span className="text-xs text-gray-400">#{item.instanciaId}</span>
                  </div>
                  <p className="text-sm text-gray-600 mt-0.5">👤 {item.nombreEstudiante}</p>
                  {item.nombreTutor && (
                    <p className="text-sm text-gray-600">🏢 Tutor: {item.nombreTutor}</p>
                  )}
                  {!item.evaluacionDocenteCompleta && (
                    <p className="text-xs text-amber-600 font-medium mt-1">
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
                      className="text-xs px-3 py-1.5 rounded-lg font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed bg-cue-primary text-white hover:opacity-90"
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
