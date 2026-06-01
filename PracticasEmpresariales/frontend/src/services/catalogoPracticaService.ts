import api from './api'
import type { ApiResponse, CatalogoPracticaResponse, Pageable } from '../types'

interface CrearCatalogoRequest {
  programaId: number
  numeroPractica: number
  nombre: string
  materiaNucleo: string
  codigoMateria: string
  numCortes: number
  duracionSemanas: number
  documentosRequeridos?: string
}

// Backend: @RequestMapping("/api/v1/catalogo-practicas")
// Con context-path=/api el frontend debe llamar /api/v1/catalogo-practicas
export const catalogoPracticaService = {
  async listar(): Promise<CatalogoPracticaResponse[]> {
    const r = await api.get<ApiResponse<Pageable<CatalogoPracticaResponse>>>('/api/v1/catalogo-practicas')
    return r.data.datos?.content ?? []
  },

  async crear(data: CrearCatalogoRequest): Promise<CatalogoPracticaResponse> {
    const r = await api.post<ApiResponse<CatalogoPracticaResponse>>('/api/v1/catalogo-practicas', data)
    return r.data.datos!
  },

  async editar(id: number, data: Partial<CrearCatalogoRequest>): Promise<CatalogoPracticaResponse> {
    const r = await api.put<ApiResponse<CatalogoPracticaResponse>>(`/api/v1/catalogo-practicas/${id}`, data)
    return r.data.datos!
  },

  async cambiarVersion(id: number): Promise<CatalogoPracticaResponse> {
    const r = await api.patch<ApiResponse<CatalogoPracticaResponse>>(`/api/v1/catalogo-practicas/${id}/cambio-version`)
    return r.data.datos!
  },
}
