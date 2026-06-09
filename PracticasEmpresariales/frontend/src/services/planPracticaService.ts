import api from './api'
import { ApiResponse, PlanPracticaResponse } from '../types'

export const planPracticaService = {
  async crearOActualizar(instanciaId: number, data: { objetivos: string; cronograma: string }): Promise<PlanPracticaResponse> {
    const res = await api.put<ApiResponse<PlanPracticaResponse>>(`/api/v1/planes-practica/${instanciaId}`, data)
    return res.data.datos!
  },

  async obtenerActual(instanciaId: number): Promise<PlanPracticaResponse | null> {
    const res = await api.get<ApiResponse<PlanPracticaResponse>>(`/api/v1/planes-practica/${instanciaId}/actual`)
    return res.data.datos ?? null
  },

  async historial(instanciaId: number): Promise<PlanPracticaResponse[]> {
    const res = await api.get<ApiResponse<PlanPracticaResponse[]>>(`/api/v1/planes-practica/${instanciaId}/historial`)
    return res.data.datos ?? []
  },

  async aprobarTutor(planId: number): Promise<PlanPracticaResponse> {
    const res = await api.patch<ApiResponse<PlanPracticaResponse>>(`/api/v1/planes-practica/${planId}/aprobar-tutor`)
    return res.data.datos!
  },

  async aprobarDocente(planId: number): Promise<PlanPracticaResponse> {
    const res = await api.patch<ApiResponse<PlanPracticaResponse>>(`/api/v1/planes-practica/${planId}/aprobar-docente`)
    return res.data.datos!
  },

  async rechazar(planId: number, motivo: string): Promise<PlanPracticaResponse> {
    const res = await api.patch<ApiResponse<PlanPracticaResponse>>(`/api/v1/planes-practica/${planId}/rechazar`, { motivo })
    return res.data.datos!
  },
}
