import { useEffect, useState } from 'react'
import api from '../services/api'
import { sprint4Service, type ConfigurarProgramaRequest } from '../services/sprint4Service'
import type { ApiResponse, Pageable, ProgramaResponse } from '../types'
import { useToast } from '../components/common/Notifications/Toast'
import { CONFIG_DEFAULTS } from '../constants/configuracion'

export function useConfiguracionPrograma() {
  const { showToast } = useToast()
  const [programas, setProgramas] = useState<ProgramaResponse[]>([])
  const [programaId, setProgramaId] = useState('')
  const [cargando, setCargando] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [config, setConfig] = useState<ConfigurarProgramaRequest>(CONFIG_DEFAULTS)

  useEffect(() => {
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas?size=100')
      .then(r => setProgramas(r.data.datos?.content ?? []))
  }, [])

  const seleccionarPrograma = async (id: string) => {
    setProgramaId(id)
    if (!id) return
    setCargando(true)
    try {
      const datos = await sprint4Service.obtenerConfiguracionPrograma(Number(id))
      setConfig({
        numeroPracticas: datos.numeroPracticas,
        semanasSeguimiento: datos.semanasSeguimiento,
        notaMinimaAprobacion: datos.notaMinimaAprobacion,
        requisitosCierre: datos.requisitosCierre ?? CONFIG_DEFAULTS.requisitosCierre,
        umbralInactividadDias: datos.umbralInactividadDias,
      })
    } catch {
      setConfig(CONFIG_DEFAULTS)
    } finally {
      setCargando(false)
    }
  }

  const guardar = async () => {
    setGuardando(true)
    try {
      await sprint4Service.configurarPrograma(Number(programaId), config)
      showToast('Configuración guardada correctamente.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? 'No se pudo guardar la configuración.', 'error')
    } finally {
      setGuardando(false)
    }
  }

  return { programas, programaId, config, cargando, guardando, seleccionarPrograma, setConfig, guardar }
}
