import api from './api'
import { ApiResponse, TutorEmpresarialResponse } from '../types'

export const tutorService = {
  async listarTodos(): Promise<TutorEmpresarialResponse[]> {
    const res = await api.get<ApiResponse<TutorEmpresarialResponse[]>>('/api/v1/tutores')
    return res.data.datos ?? []
  },

  async listarPorEmpresa(empresaId: number): Promise<TutorEmpresarialResponse[]> {
    const res = await api.get<ApiResponse<TutorEmpresarialResponse[]>>(`/api/v1/tutores/empresa/${empresaId}`)
    return res.data.datos ?? []
  },

  async actualizarTelefono(id: number, telefono: string): Promise<TutorEmpresarialResponse> {
    const res = await api.patch<ApiResponse<TutorEmpresarialResponse>>(`/api/v1/tutores/${id}/telefono`, { telefono })
    return res.data.datos!
  },
}
