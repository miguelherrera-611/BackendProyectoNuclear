import api from './api'
import type { ApiResponse, ExpedienteResponse } from '../types'

// Backend: @RequestMapping("/api/v1/expedientes")
// Con context-path=/api el frontend debe llamar /api/v1/expedientes
export const expedienteService = {
  async obtener(estudianteId: number): Promise<ExpedienteResponse> {
    const r = await api.get<ApiResponse<ExpedienteResponse>>(`/api/v1/expedientes/${estudianteId}`)
    return r.data.datos!
  },
}
