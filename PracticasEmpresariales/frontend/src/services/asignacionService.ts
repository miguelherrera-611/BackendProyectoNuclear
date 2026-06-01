import api from './api'
import { ApiResponse, InstanciaPracticaResponse } from '../types'

interface CrearAsignacionRequest {
  estudianteId: number
  vacanteId: number
  catalogoPracticaId?: number
  docenteAsesorId?: number
  tutorEmpresarialId?: number
}

export const asignacionService = {
  async crear(data: CrearAsignacionRequest): Promise<InstanciaPracticaResponse> {
    const res = await api.post<ApiResponse<InstanciaPracticaResponse>>('/asignaciones', data)
    return res.data.datos!
  },

  async listar(estado?: string): Promise<InstanciaPracticaResponse[]> {
    const params = estado ? { estado } : {}
    const res = await api.get<ApiResponse<InstanciaPracticaResponse[]>>('/asignaciones', { params })
    return res.data.datos ?? []
  },

  async detalle(id: number): Promise<InstanciaPracticaResponse> {
    const res = await api.get<ApiResponse<InstanciaPracticaResponse>>(`/asignaciones/${id}`)
    return res.data.datos!
  },

  async cancelar(id: number, motivo: string): Promise<void> {
    await api.patch(`/asignaciones/${id}/cancelar`, { motivo })
  },
}
