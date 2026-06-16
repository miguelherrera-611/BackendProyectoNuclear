import api from './api'
import { ApiResponse, AuthUser } from '../types'

interface LoginRequest {
  correo: string
  password: string
}

interface LoginPendienteResponse {
  correo: string
  mensaje: string
  expiresInSeconds: number
}

interface CambiarPasswordRequest {
  passwordActual: string
  passwordNueva: string
  passwordConfirmacion: string
}

export const authService = {
  async iniciarLogin(data: LoginRequest): Promise<LoginPendienteResponse> {
    const res = await api.post<ApiResponse<LoginPendienteResponse>>('/auth/login', data)
    return res.data.datos!
  },

  async verificarCodigoLogin(data: { correo: string; codigo: string }): Promise<AuthUser> {
    const res = await api.post<ApiResponse<AuthUser & { token: string; tipo: string }>>('/auth/login/verificar', data)
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
