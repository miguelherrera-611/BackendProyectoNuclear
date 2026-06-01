import api from './api'
import { ApiResponse, TutorEmpresarialResponse } from '../types'

interface CrearTutorRequest {
  nombre: string
  cargo?: string
  correo: string
  telefono?: string
  empresaId: number
}

export const tutorService = {
  async listarPorEmpresa(empresaId: number): Promise<TutorEmpresarialResponse[]> {
    const res = await api.get<ApiResponse<TutorEmpresarialResponse[]>>(`/api/v1/tutores/empresa/${empresaId}`)
    return res.data.datos ?? []
  },

  async crear(data: CrearTutorRequest): Promise<TutorEmpresarialResponse> {
    const res = await api.post<ApiResponse<TutorEmpresarialResponse>>('/api/v1/tutores', data)
    return res.data.datos!
  },

  async desactivar(id: number): Promise<TutorEmpresarialResponse> {
    const res = await api.patch<ApiResponse<TutorEmpresarialResponse>>(`/api/v1/tutores/${id}/desactivar`)
    return res.data.datos!
  },
}
