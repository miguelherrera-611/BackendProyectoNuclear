import api from './api'
import { ApiResponse, InstanciaPracticaResponseV2 } from '../types'

export const sustentacionDocenteService = {
  async agendar(instanciaId: number, fecha: string): Promise<InstanciaPracticaResponseV2> {
    const res = await api.put<ApiResponse<InstanciaPracticaResponseV2>>(
      `/api/v1/sustentaciones-docente/${instanciaId}`,
      { fecha }
    )
    return res.data.datos!
  },
}
