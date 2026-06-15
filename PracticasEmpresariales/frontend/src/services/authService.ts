import api from './api'
import { ApiResponse, AuthUser } from '../types'

interface LoginRequest {
  correo: string
  password: string
}

interface CambiarPasswordRequest {
  passwordActual: string
  passwordNueva: string
  passwordConfirmacion: string
}

export const authService = {
  async login(data: LoginRequest): Promise<AuthUser> {
    const res = await api.post<ApiResponse<AuthUser & { token: string; tipo: string }>>('/auth/login', data)
    return res.data.datos!
  },

  async cambiarPassword(data: CambiarPasswordRequest): Promise<void> {
    await api.post('/auth/cambiar-password', data)
  },

  async solicitarCambioCorreo(): Promise<void> {
    await api.post('/auth/correo/solicitar-cambio')
  },

  async confirmarCambioCorreo(data: { codigo: string; nuevoCorreo: string }): Promise<void> {
    await api.post('/auth/correo/confirmar-cambio', data)
  },
}
