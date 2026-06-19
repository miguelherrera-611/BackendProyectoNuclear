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

export const catalogoPracticaService = {
  async listar(): Promise<CatalogoPracticaResponse[]> {
    const r = await api.get<ApiResponse<CatalogoPracticaResponse[]>>('/api/v1/catalogo-practicas')
    return r.data.datos ?? []
  },

  async listarPaginado(page = 0, size = 20): Promise<Pageable<CatalogoPracticaResponse>> {
    const r = await api.get<ApiResponse<Pageable<CatalogoPracticaResponse>>>('/api/v1/catalogo-practicas/page', { params: { page, size } })
    return r.data.datos!
  },

  async crear(data: CrearCatalogoRequest): Promise<CatalogoPracticaResponse> {
    const r = await api.post<ApiResponse<CatalogoPracticaResponse>>('/api/v1/catalogo-practicas', data)
    return r.data.datos!
  },

  async editar(id: number, data: Partial<CrearCatalogoRequest>): Promise<CatalogoPracticaResponse> {
    const r = await api.put<ApiResponse<CatalogoPracticaResponse>>(`/api/v1/catalogo-practicas/${id}`, data)
    return r.data.datos!
  },

}
