import { useState, useEffect, useCallback } from 'react'
import { sprint4Service, type PlantillaNotificacionRequest } from '../services/sprint4Service'
import type { PlantillaNotificacionResponse, TipoEventoNotificacion } from '../types'
import { useToast } from '../components/common/Notifications/Toast'

const PLANTILLA_POR_DEFECTO: Omit<PlantillaNotificacionRequest, 'tipoEvento'> = {
  asunto: '',
  cuerpo: '',
  rolesReceptores: '',
  frecuenciaRecordatorioDias: 1,
}

function respuestaARequest(r: PlantillaNotificacionResponse): PlantillaNotificacionRequest {
  return {
    tipoEvento: r.tipoEvento,
    asunto: r.asunto,
    cuerpo: r.cuerpo,
    rolesReceptores: r.rolesReceptores ?? '',
    frecuenciaRecordatorioDias: r.frecuenciaRecordatorioDias,
  }
}

export function usePlantillaNotificacion() {
  const { showToast } = useToast()

  const [eventoActivo, setEventoActivo] = useState<TipoEventoNotificacion>('CIERRE_FORMAL_EJECUTADO')
  const [plantilla, setPlantilla] = useState<PlantillaNotificacionRequest>({
    ...PLANTILLA_POR_DEFECTO,
    tipoEvento: 'CIERRE_FORMAL_EJECUTADO',
  })
  // Índice de todas las plantillas guardadas: tipoEvento → datos
  const [guardadas, setGuardadas] = useState<Map<TipoEventoNotificacion, PlantillaNotificacionResponse>>(new Map())
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)

  // Al montar, carga todas las plantillas guardadas de una vez
  useEffect(() => {
    sprint4Service.listarPlantillas()
      .then(lista => {
        const mapa = new Map<TipoEventoNotificacion, PlantillaNotificacionResponse>()
        lista.forEach(p => mapa.set(p.tipoEvento, p))
        setGuardadas(mapa)
        // Aplica la del evento activo si existe
        const guardada = mapa.get('CIERRE_FORMAL_EJECUTADO')
        if (guardada) setPlantilla(respuestaARequest(guardada))
      })
      .finally(() => setCargando(false))
  }, [])

  const cambiarEvento = useCallback((evento: TipoEventoNotificacion) => {
    setEventoActivo(evento)
    const guardada = guardadas.get(evento)
    if (guardada) {
      setPlantilla(respuestaARequest(guardada))
    } else {
      // Evento sin plantilla guardada aún → campos en blanco listos para configurar
      setPlantilla({ ...PLANTILLA_POR_DEFECTO, tipoEvento: evento })
    }
  }, [guardadas])

  const guardar = async () => {
    setGuardando(true)
    try {
      const guardada = await sprint4Service.guardarPlantilla(plantilla)
      // Actualiza el índice local para que cambios futuros de evento vean el dato fresco
      setGuardadas(prev => new Map(prev).set(guardada.tipoEvento, guardada))
      showToast('Plantilla guardada correctamente.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? 'No se pudo guardar la plantilla.', 'error')
    } finally {
      setGuardando(false)
    }
  }

  const eliminar = async (evento: TipoEventoNotificacion) => {
    try {
      await sprint4Service.eliminarPlantilla(evento)
      setGuardadas(prev => {
        const siguiente = new Map(prev)
        siguiente.delete(evento)
        return siguiente
      })
      // Si se eliminó el evento que está abierto en el editor, limpia el formulario
      if (plantilla.tipoEvento === evento) {
        setPlantilla({ ...PLANTILLA_POR_DEFECTO, tipoEvento: evento })
      }
      showToast('Plantilla eliminada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? 'No se pudo eliminar la plantilla.', 'error')
    }
  }

  return { plantilla, setPlantilla, guardando, cargando, eventoActivo, guardar, cambiarEvento, guardadas, eliminar }
}
