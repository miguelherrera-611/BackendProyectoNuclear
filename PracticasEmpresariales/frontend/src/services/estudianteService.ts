import api from './api'
import { ApiResponse, Pageable, UsuarioResponse, EstadoEstudiante } from '../types'

export const estudianteService = {
  async listar(estado?: EstadoEstudiante, page = 0, size = 20): Promise<Pageable<UsuarioResponse>> {
    const params: Record<string, string | number> = { page, size }
    if (estado) params.estado = estado
    const res = await api.get<ApiResponse<Pageable<UsuarioResponse>>>('/v1/estudiantes', { params })
    return res.data.datos!
  },

  async obtener(id: number): Promise<UsuarioResponse> {
    const res = await api.get<ApiResponse<UsuarioResponse>>(`/v1/estudiantes/${id}`)
    return res.data.datos!
  },

  async marcarApto(id: number, catalogoPracticaId: number): Promise<UsuarioResponse> {
    const res = await api.patch<ApiResponse<UsuarioResponse>>(`/v1/estudiantes/${id}/marcar-apto`, { catalogoPracticaId })
    return res.data.datos!
  },

  async mantenerNoApto(id: number, motivo: string): Promise<UsuarioResponse> {
    const res = await api.patch<ApiResponse<UsuarioResponse>>(`/v1/estudiantes/${id}/mantener-no-apto`, { motivo })
    return res.data.datos!
  },

  async enviarAlProceso(estudiantesIds: number[]): Promise<UsuarioResponse[]> {
    const res = await api.post<ApiResponse<UsuarioResponse[]>>('/v1/estudiantes/enviar-al-proceso', { estudiantesIds })
    return res.data.datos ?? []
  },
}
