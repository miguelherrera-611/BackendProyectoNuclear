import api from './api'
import { ApiResponse, InstanciaPracticaResponse, Pageable } from '../types'

interface CrearAsignacionRequest {
  estudianteId: number
  vacanteId: number
  catalogoPracticaId?: number
  docenteAsesorId?: number
  tutorEmpresarialId?: number
}

export const asignacionService = {
  async crear(data: CrearAsignacionRequest): Promise<InstanciaPracticaResponse> {
    const res = await api.post<ApiResponse<InstanciaPracticaResponse>>('/api/asignaciones', data)
    return res.data.datos!
  },

  async listar(estado?: string): Promise<InstanciaPracticaResponse[]> {
    const params = estado ? { estado } : {}
    const res = await api.get<ApiResponse<InstanciaPracticaResponse[]>>('/api/asignaciones', { params })
    return res.data.datos ?? []
  },

  async listarPaginado(estado?: string, page = 0, size = 20): Promise<Pageable<InstanciaPracticaResponse>> {
    const params = estado ? { estado, page, size } : { page, size }
    const res = await api.get<ApiResponse<Pageable<InstanciaPracticaResponse>>>('/api/asignaciones/page', { params })
    return res.data.datos!
  },

  async detalle(id: number): Promise<InstanciaPracticaResponse> {
    const res = await api.get<ApiResponse<InstanciaPracticaResponse>>(`/api/asignaciones/${id}`)
    return res.data.datos!
  },

  async cancelar(id: number, motivo: string): Promise<void> {
    await api.patch(`/api/asignaciones/${id}/cancelar`, { motivo })
  },
}
