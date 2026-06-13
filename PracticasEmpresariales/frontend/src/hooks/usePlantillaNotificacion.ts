import { useState } from 'react'
import { sprint4Service, type PlantillaNotificacionRequest } from '../services/sprint4Service'
import type { TipoEventoNotificacion } from '../types'
import { useToast } from '../components/common/Notifications/Toast'

const PLANTILLA_INICIAL: PlantillaNotificacionRequest = {
  tipoEvento: 'CIERRE_FORMAL_EJECUTADO',
  asunto: 'Resultado de práctica - {{nombre_estudiante}}',
  cuerpo: '<p>Estimado/a {{nombre_estudiante}},</p><p>Le informamos que su práctica ha concluido con resultado: {{resultado}}. Nota final: {{nota_final}}.</p>',
  rolesReceptores: 'ESTUDIANTE,DOCENTE_ASESOR,TUTOR_EMPRESARIAL',
  frecuenciaRecordatorioDias: 1,
}

export function usePlantillaNotificacion() {
  const { showToast } = useToast()
  const [plantilla, setPlantilla] = useState<PlantillaNotificacionRequest>(PLANTILLA_INICIAL)
  const [guardando, setGuardando] = useState(false)

  const guardar = async () => {
    setGuardando(true)
    try {
      await sprint4Service.guardarPlantilla(plantilla)
      showToast('Plantilla de notificación guardada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? 'No se pudo guardar la plantilla.', 'error')
    } finally {
      setGuardando(false)
    }
  }

  const cambiarEvento = (evento: TipoEventoNotificacion) => {
    setPlantilla(prev => ({ ...prev, tipoEvento: evento }))
  }

  return { plantilla, setPlantilla, guardando, guardar, cambiarEvento }
}
