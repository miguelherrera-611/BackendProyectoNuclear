import api from './api'
import { ApiResponse, Pageable, Rol, EtiquetaCargo, UsuarioResponse } from '../types'

interface CrearUsuarioRequest {
  nombre: string
  correo: string
  rol: Rol
  etiquetaCargo?: EtiquetaCargo
  telefono?: string
  facultadId?: number
  programaId?: number
  // Campos exclusivos de ESTUDIANTE
  identificacion?: string
  semestre?: number
  contactoEmergencia?: string
}

export const usuarioService = {
  async crear(data: CrearUsuarioRequest): Promise<UsuarioResponse> {
    const res = await api.post<ApiResponse<UsuarioResponse>>('/usuarios', data)
    return res.data.datos!
  },

  async listar(page = 0, size = 20): Promise<Pageable<UsuarioResponse>> {
    const res = await api.get<ApiResponse<Pageable<UsuarioResponse>>>('/usuarios', { params: { page, size } })
    return res.data.datos!
  },

  async obtener(id: number): Promise<UsuarioResponse> {
    const res = await api.get<ApiResponse<UsuarioResponse>>(`/usuarios/${id}`)
    return res.data.datos!
  },

  async editar(id: number, data: Partial<UsuarioResponse>): Promise<UsuarioResponse> {
    const res = await api.put<ApiResponse<UsuarioResponse>>(`/usuarios/${id}`, data)
    return res.data.datos!
  },

  async desactivar(id: number): Promise<void> {
    await api.patch(`/usuarios/${id}/desactivar`)
  },

  async activar(id: number): Promise<void> {
    await api.patch(`/usuarios/${id}/activar`)
  },
}
