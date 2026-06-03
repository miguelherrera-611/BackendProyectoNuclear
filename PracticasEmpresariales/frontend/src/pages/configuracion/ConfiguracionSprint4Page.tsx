import { useEffect, useState } from 'react'
import api from '../../services/api'
import { sprint4Service, type PlantillaNotificacionRequest } from '../../services/sprint4Service'
import type { ApiResponse, Pageable, ProgramaResponse, TipoEventoNotificacion } from '../../types'

const requisitos = 'evaluacion_docente,evaluacion_tutor,nota_final,encuesta_tutor,encuesta_estudiante,documentos,sustentacion'
const eventos: TipoEventoNotificacion[] = [
  'EVALUACION_DOCENTE_COMPLETADA',
  'EVALUACION_TUTOR_COMPLETADA',
  'NOTA_FINAL_REGISTRADA',
  'ENCUESTA_TUTOR_ENVIADA',
  'ENCUESTA_ESTUDIANTE_ENVIADA',
  'ENCUESTA_COMPLETADA',
  'CIERRE_FORMAL_EJECUTADO',
  'COORDINACION_ACADEMICA_RESULTADO',
]

export default function ConfiguracionSprint4Page() {
  const [programas, setProgramas] = useState<ProgramaResponse[]>([])
  const [programaId, setProgramaId] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [config, setConfig] = useState({
    numeroPracticas: 1,
    semanasSeguimiento: 12,
    notaMinimaAprobacion: 3,
    requisitosCierre: requisitos,
    umbralInactividadDias: 7,
  })
  const [plantilla, setPlantilla] = useState<PlantillaNotificacionRequest>({
    tipoEvento: 'CIERRE_FORMAL_EJECUTADO',
    asunto: 'Resultado de practica {{nombre_estudiante}}',
    cuerpo: '<p>Resultado: {{resultado}}</p><p>Nota final: {{nota_final}}</p>',
    rolesReceptores: 'ESTUDIANTE,DOCENTE_ASESOR,TUTOR_EMPRESARIAL',
    frecuenciaRecordatorioDias: 1,
  })
  const [preview, setPreview] = useState('')

  useEffect(() => {
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas?size=100')
      .then(r => setProgramas(r.data.datos?.content ?? []))
  }, [])

  const guardarConfig = async () => {
    try {
      await sprint4Service.configurarPrograma(Number(programaId), config)
      setMensaje('Configuracion de programa guardada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo guardar la configuracion.')
    }
  }

  const guardarPlantilla = async () => {
    try {
      await sprint4Service.guardarPlantilla(plantilla)
      setMensaje('Plantilla guardada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setMensaje(msg ?? 'No se pudo guardar la plantilla.')
    }
  }

  const previsualizar = async () => {
    setPreview(await sprint4Service.previsualizarPlantilla(plantilla))
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Configuracion Sprint 4</h1>
        <p className="text-sm text-gray-500 mt-1">Parametros por programa y plantillas de correo.</p>
      </div>

      {mensaje && <div className="card text-sm">{mensaje}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-900">Parametros por programa</h2>
          <select className="input-field" value={programaId} onChange={e => setProgramaId(e.target.value)}>
            <option value="">Selecciona programa</option>
            {programas.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
          </select>
          <input className="input-field" type="number" min={1} value={config.numeroPracticas} onChange={e => setConfig({ ...config, numeroPracticas: Number(e.target.value) })} />
          <input className="input-field" type="number" min={1} value={config.semanasSeguimiento} onChange={e => setConfig({ ...config, semanasSeguimiento: Number(e.target.value) })} />
          <input className="input-field" type="number" step="0.1" min={0} max={5} value={config.notaMinimaAprobacion} onChange={e => setConfig({ ...config, notaMinimaAprobacion: Number(e.target.value) })} />
          <textarea className="input-field" rows={3} value={config.requisitosCierre} onChange={e => setConfig({ ...config, requisitosCierre: e.target.value })} />
          <input className="input-field" type="number" min={1} value={config.umbralInactividadDias} onChange={e => setConfig({ ...config, umbralInactividadDias: Number(e.target.value) })} />
          <button className="btn-primary w-full" disabled={!programaId} onClick={guardarConfig}>Guardar parametros</button>
        </div>

        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-900">Plantillas de notificacion</h2>
          <select className="input-field" value={plantilla.tipoEvento} onChange={e => setPlantilla({ ...plantilla, tipoEvento: e.target.value as TipoEventoNotificacion })}>
            {eventos.map(e => <option key={e} value={e}>{e}</option>)}
          </select>
          <input className="input-field" value={plantilla.asunto} onChange={e => setPlantilla({ ...plantilla, asunto: e.target.value })} />
          <textarea className="input-field" rows={5} value={plantilla.cuerpo} onChange={e => setPlantilla({ ...plantilla, cuerpo: e.target.value })} />
          <input className="input-field" value={plantilla.rolesReceptores ?? ''} onChange={e => setPlantilla({ ...plantilla, rolesReceptores: e.target.value })} />
          <input className="input-field" type="number" min={1} value={plantilla.frecuenciaRecordatorioDias} onChange={e => setPlantilla({ ...plantilla, frecuenciaRecordatorioDias: Number(e.target.value) })} />
          <div className="flex gap-3">
            <button className="btn-secondary flex-1" onClick={previsualizar}>Previsualizar</button>
            <button className="btn-primary flex-1" onClick={guardarPlantilla}>Guardar plantilla</button>
          </div>
          {preview && <div className="bg-gray-50 rounded-lg p-4 text-sm" dangerouslySetInnerHTML={{ __html: preview }} />}
        </div>
      </div>
    </div>
  )
}
