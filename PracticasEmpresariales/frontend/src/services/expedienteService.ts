import api from './api'
import type { ApiResponse, ExpedienteResponse, HojaDeVidaResponse } from '../types'

export const expedienteService = {
  async obtener(estudianteId: number): Promise<ExpedienteResponse> {
    const r = await api.get<ApiResponse<ExpedienteResponse>>(`/api/v1/expedientes/${estudianteId}`)
    return r.data.datos!
  },

  async subirHojaDeVida(estudianteId: number, urlArchivo: string): Promise<HojaDeVidaResponse> {
    const r = await api.post<ApiResponse<HojaDeVidaResponse>>(
      `/api/v1/expedientes/${estudianteId}/hoja-de-vida`,
      { urlArchivo }
    )
    return r.data.datos!
  },
}
