import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ChecklistCierrePanel } from '../../components/cierre/ChecklistCierrePanel'
import { sprint4Service } from '../../services/sprint4Service'
import { usuarioService } from '../../services/usuarioService'
import type { ChecklistCierreResponse, CierreFormalResponse, ResultadoSustentacion, UsuarioResponse } from '../../types'
import { Select } from '../../components/common/Select/Select'

export default function CierrePracticaPage() {
  // SPRINT 4 - Facade UI: una pantalla coordina sustentacion, encuestas, checklist y cierre formal.
  const { instanciaId } = useParams()
  const id = Number(instanciaId)
  const [fecha, setFecha] = useState('')
  const [jurados, setJurados] = useState('')
  const [actaUrl, setActaUrl] = useState('')
  const [tutores, setTutores] = useState<UsuarioResponse[]>([])
  const [tutorEmpresarialId, setTutorEmpresarialId] = useState<number | null>(null)
  const [tituloEncuesta, setTituloEncuesta] = useState('Evaluacion Satisfaccion 2026-I')
  const [preguntasEncuesta, setPreguntasEncuesta] = useState('Como evalua el proceso?\nQue aspectos recomienda mejorar?')
  const [resultadoSust, setResultadoSust] = useState<ResultadoSustentacion>('APROBADO')
  const [checklist, setChecklist] = useState<ChecklistCierreResponse>()
  const [cierre, setCierre] = useState<CierreFormalResponse>()
  const [mensaje, setMensaje] = useState('')
  const [saving, setSaving] = useState(false)

  const cargarChecklist = () => {
    sprint4Service.checklist(id).then(setChecklist).catch(() => setMensaje('No se pudo cargar el checklist.'))
  }

  useEffect(() => {
    cargarChecklist()
    usuarioService.listarTutores().then(setTutores)
  }, [id])

  const programar = async () => {
    setSaving(true)
    setMensaje('')
    try {
      await sprint4Service.programarSustentacion(id, fecha, jurados.split(',').map(j => j.trim()).filter(Boolean))
      setMensaje('Sustentacion programada.')
      cargarChecklist()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo programar la sustentacion.')
    } finally {
      setSaving(false)
    }
  }

  const registrarResultado = async () => {
    setSaving(true)
    setMensaje('')
    try {
      await sprint4Service.registrarResultadoSustentacion(id, resultadoSust, actaUrl, true)
      setMensaje('Resultado de sustentacion registrado.')
      cargarChecklist()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo registrar el resultado.')
    } finally {
      setSaving(false)
    }
  }

  const enviarEncuestaTutor = async () => {
    setSaving(true)
    setMensaje('')
    try {
      await sprint4Service.enviarEncuestaTutor(id, {
        titulo: tituloEncuesta,
        tutorEmpresarialId: tutorEmpresarialId!,
        preguntas: preguntasEncuesta.split('\n').map(p => p.trim()).filter(Boolean),
      })
      setMensaje('Encuesta enviada al tutor.')
      cargarChecklist()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo enviar la encuesta al tutor.')
    } finally {
      setSaving(false)
    }
  }

  const enviarEncuestaEstudiante = async () => {
    setSaving(true)
    setMensaje('')
    try {
      await sprint4Service.enviarEncuestaEstudiante(id, {
        titulo: tituloEncuesta,
        preguntas: preguntasEncuesta.split('\n').map(p => p.trim()).filter(Boolean),
      })
      setMensaje('Encuesta enviada al estudiante.')
      cargarChecklist()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo enviar la encuesta al estudiante.')
    } finally {
      setSaving(false)
    }
  }

  const ejecutar = async () => {
    if (!window.confirm('El cierre formal es irreversible. Deseas continuar?')) return
    setSaving(true)
    try {
      const res = await sprint4Service.ejecutarCierre(id)
      setCierre(res)
      setMensaje('Cierre formal ejecutado.')
      cargarChecklist()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo ejecutar el cierre.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Cierre formal de practica</h1>
        <p className="text-sm text-gray-500 mt-1">Sustentacion, checklist y cierre irreversible.</p>
      </div>

      {mensaje && <div className="card text-sm">{mensaje}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-900">Sustentacion obligatoria</h2>
          <input className="input-field" type="date" value={fecha} onChange={e => setFecha(e.target.value)} />
          <input className="input-field" placeholder="Jurados separados por coma" value={jurados} onChange={e => setJurados(e.target.value)} />
          <button className="btn-secondary w-full" disabled={saving || !fecha || !jurados} onClick={programar}>Programar sustentacion</button>
          <div className="border-t border-gray-100 pt-4 space-y-3">
            <Select value={resultadoSust} onChange={e => setResultadoSust(e.target.value as ResultadoSustentacion)}>
              <option value="APROBADO">Aprobado</option>
              <option value="NO_APROBADO">No aprobado</option>
            </Select>
            <input className="input-field" placeholder="URL o ruta del acta firmada" value={actaUrl} onChange={e => setActaUrl(e.target.value)} />
            <button className="btn-secondary w-full" disabled={saving || !actaUrl} onClick={registrarResultado}>Registrar acta y resultado</button>
          </div>
        </div>

        <ChecklistCierrePanel checklist={checklist} onCerrar={ejecutar} cerrando={saving} />
      </div>

      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-900">Encuestas obligatorias</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <input className="input-field" placeholder="Titulo de encuesta" value={tituloEncuesta} onChange={e => setTituloEncuesta(e.target.value)} />
          <Select
            value={tutorEmpresarialId ?? ''}
            onChange={e => setTutorEmpresarialId(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">-- Selecciona el tutor empresarial --</option>
            {tutores.map(t => (
              <option key={t.id} value={t.id}>{t.nombre} — {t.correo}</option>
            ))}
          </Select>
        </div>
        <textarea className="input-field" rows={3} value={preguntasEncuesta} onChange={e => setPreguntasEncuesta(e.target.value)} />
        <div className="flex flex-col sm:flex-row gap-3">
          <button className="btn-secondary flex-1" disabled={saving || !tutorEmpresarialId} onClick={enviarEncuestaTutor}>Enviar a tutor</button>
          <button className="btn-secondary flex-1" disabled={saving} onClick={enviarEncuestaEstudiante}>Enviar a estudiante</button>
        </div>
      </div>

      {cierre && (
        <div className="card space-y-3">
          <h2 className="font-semibold text-gray-900">Resultado oficial</h2>
          <p className="text-sm text-gray-700">Resultado: <strong>{cierre.resultado}</strong> · Nota: <strong>{cierre.notaFinal}</strong></p>
          {cierre.codigoPazYSalvo && <p className="text-sm text-gray-700">Paz y Salvo: <strong>{cierre.codigoPazYSalvo}</strong></p>}
          {cierre.pazYSalvo && <pre className="bg-gray-50 rounded-lg p-4 text-xs whitespace-pre-wrap">{cierre.pazYSalvo}</pre>}
        </div>
      )}
    </div>
  )
}
