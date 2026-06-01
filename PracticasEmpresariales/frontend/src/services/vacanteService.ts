import api from './api'
import { ApiResponse, VacanteResponse } from '../types'

interface CrearVacanteRequest {
  empresaId: number
  area: string
  cuposTotales: number
}

export const vacanteService = {
  async listar(): Promise<VacanteResponse[]> {
    const res = await api.get<ApiResponse<VacanteResponse[]>>('/api/v1/vacantes')
    return res.data.datos ?? []
  },

  async listarPendientes(): Promise<VacanteResponse[]> {
    const res = await api.get<ApiResponse<VacanteResponse[]>>('/api/v1/vacantes/pendientes')
    return res.data.datos ?? []
  },

  async listarDisponibles(): Promise<VacanteResponse[]> {
    const res = await api.get<ApiResponse<VacanteResponse[]>>('/api/v1/vacantes/disponibles')
    return res.data.datos ?? []
  },

  async listarPorEmpresa(empresaId: number): Promise<VacanteResponse[]> {
    const res = await api.get<ApiResponse<VacanteResponse[]>>(`/api/v1/vacantes/empresa/${empresaId}`)
    return res.data.datos ?? []
  },

  async crear(data: CrearVacanteRequest): Promise<VacanteResponse> {
    const res = await api.post<ApiResponse<VacanteResponse>>('/api/v1/vacantes', data)
    return res.data.datos!
  },

  async aprobar(id: number): Promise<VacanteResponse> {
    const res = await api.patch<ApiResponse<VacanteResponse>>(`/api/v1/vacantes/${id}/aprobar`)
    return res.data.datos!
  },

  async rechazar(id: number, motivo: string): Promise<VacanteResponse> {
    const res = await api.patch<ApiResponse<VacanteResponse>>(`/api/v1/vacantes/${id}/rechazar`, { motivo })
    return res.data.datos!
  },

  async cerrar(id: number): Promise<VacanteResponse> {
    const res = await api.patch<ApiResponse<VacanteResponse>>(`/api/v1/vacantes/${id}/cerrar`)
    return res.data.datos!
  },
}
