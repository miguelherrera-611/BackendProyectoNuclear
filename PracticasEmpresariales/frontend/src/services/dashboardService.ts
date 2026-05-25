import api from './api'
import { ApiResponse, DashboardResponse } from '../types'

export const dashboardService = {
  async obtener(): Promise<DashboardResponse> {
    const res = await api.get<ApiResponse<DashboardResponse>>('/dashboard')
    return res.data.datos!
  },
}
