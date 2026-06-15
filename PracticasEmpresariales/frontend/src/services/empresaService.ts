import api from './api'
import { ApiResponse, EmpresaResponse } from '../types'

interface CrearEmpresaRequest {
  razonSocial: string
  nit: string
  sector?: string
  direccion?: string
  municipio?: string
  telefono?: string
  nombreContacto: string
  correo?: string
  areasDisponibles: string[]
}

export const empresaService = {
  async listar(): Promise<EmpresaResponse[]> {
    const res = await api.get<ApiResponse<EmpresaResponse[]>>('/api/v1/empresas')
    return res.data.datos ?? []
  },

  async listarActivas(): Promise<EmpresaResponse[]> {
    const res = await api.get<ApiResponse<EmpresaResponse[]>>('/api/v1/empresas/activas')
    return res.data.datos ?? []
  },

  async crear(data: CrearEmpresaRequest): Promise<EmpresaResponse> {
    const res = await api.post<ApiResponse<EmpresaResponse>>('/api/v1/empresas', data)
    return res.data.datos!
  },

  async clonar(id: number, razonSocial: string, nit: string): Promise<EmpresaResponse> {
    const res = await api.post<ApiResponse<EmpresaResponse>>(
      `/api/v1/empresas/${id}/clonar?razonSocial=${encodeURIComponent(razonSocial)}&nit=${encodeURIComponent(nit)}`
    )
    return res.data.datos!
  },

  async activar(id: number): Promise<EmpresaResponse> {
    const res = await api.patch<ApiResponse<EmpresaResponse>>(`/api/v1/empresas/${id}/activar`)
    return res.data.datos!
  },

  async inactivar(id: number): Promise<EmpresaResponse> {
    const res = await api.patch<ApiResponse<EmpresaResponse>>(`/api/v1/empresas/${id}/inactivar`)
    return res.data.datos!
  },
}
