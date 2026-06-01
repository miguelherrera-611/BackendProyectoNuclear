import api from './api'
import { ApiResponse, SeguimientoSemanalResponse, InstanciaPracticaResponseV2 } from '../types'

interface CrearSeguimientoRequest {
  semana: number
  actividades: string
  logros: string
  dificultades?: string
  evidencias?: string
}

export const seguimientoService = {
  async crear(instanciaId: number, data: CrearSeguimientoRequest): Promise<SeguimientoSemanalResponse> {
    const res = await api.post<ApiResponse<SeguimientoSemanalResponse>>(`/api/v1/seguimientos/${instanciaId}`, data)
    return res.data.datos!
  },

  async editar(seguimientoId: number, data: CrearSeguimientoRequest): Promise<SeguimientoSemanalResponse> {
    const res = await api.put<ApiResponse<SeguimientoSemanalResponse>>(`/api/v1/seguimientos/${seguimientoId}/editar`, data)
    return res.data.datos!
  },

  async listar(instanciaId: number): Promise<SeguimientoSemanalResponse[]> {
    const res = await api.get<ApiResponse<SeguimientoSemanalResponse[]>>(`/api/v1/seguimientos/${instanciaId}`)
    return res.data.datos ?? []
  },

  async aprobar(seguimientoId: number): Promise<SeguimientoSemanalResponse> {
    const res = await api.patch<ApiResponse<SeguimientoSemanalResponse>>(`/api/v1/seguimientos/${seguimientoId}/aprobar`)
    return res.data.datos!
  },

  async rechazar(seguimientoId: number, observacion: string): Promise<SeguimientoSemanalResponse> {
    const res = await api.patch<ApiResponse<SeguimientoSemanalResponse>>(`/api/v1/seguimientos/${seguimientoId}/rechazar`, { observacion })
    return res.data.datos!
  },

  // Tablero y práctica del estudiante (vinculaciones)
  async tableroGeneral(): Promise<InstanciaPracticaResponseV2[]> {
    const res = await api.get<ApiResponse<InstanciaPracticaResponseV2[]>>('/api/v1/vinculaciones/tablero')
    return res.data.datos ?? []
  },

  async misPracticantes(): Promise<InstanciaPracticaResponseV2[]> {
    const res = await api.get<ApiResponse<InstanciaPracticaResponseV2[]>>('/api/v1/vinculaciones/mis-practicantes')
    return res.data.datos ?? []
  },

  async miPractica(): Promise<InstanciaPracticaResponseV2> {
    const res = await api.get<ApiResponse<InstanciaPracticaResponseV2>>('/api/v1/vinculaciones/mi-practica')
    return res.data.datos!
  },
}
