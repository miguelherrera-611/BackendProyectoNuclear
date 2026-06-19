import api from './api'
import type {
  ApiResponse,
  ChecklistCierreResponse,
  CierreFormalResponse,
  CriterioEvaluacion,
  EncuestaCoordinadorResumen,
  EncuestaResponse,
  EvaluacionFinalResponse,
  NotaFinalResponse,
  PlantillaNotificacionResponse,
  ProgramaConfiguracionResponse,
  ReporteEstadoProcesoResponse,
  ResultadoSustentacion,
  SustentacionResponse,
  TableroGerencialResponse,
  TipoEventoNotificacion,
  TipoExportacionReporte,
  Pageable,
} from '../types'

export interface RegistrarEvaluacionRequest {
  criterios: CriterioEvaluacion[]
  observaciones?: string
}

export interface EnviarEncuestaRequest {
  titulo: string
  preguntas: string[]
  tutorEmpresarialId?: number
}

export interface ConfigurarProgramaRequest {
  numeroPracticas: number
  semanasSeguimiento: number
  notaMinimaAprobacion: number
  requisitosCierre: string
  umbralInactividadDias: number
}

export interface PlantillaNotificacionRequest {
  tipoEvento: TipoEventoNotificacion
  asunto: string
  cuerpo: string
  rolesReceptores?: string
  frecuenciaRecordatorioDias: number
}

// SPRINT 4 - Facade frontend: concentra el acceso HTTP a evaluaciones, encuestas, cierre, reportes y configuracion.
export const sprint4Service = {
  async registrarEvaluacionDocente(instanciaId: number, data: RegistrarEvaluacionRequest) {
    const res = await api.post<ApiResponse<EvaluacionFinalResponse>>(`/api/v1/evaluaciones-finales/${instanciaId}/docente`, data)
    return res.data.datos!
  },

  async registrarEvaluacionTutor(instanciaId: number, data: RegistrarEvaluacionRequest) {
    const res = await api.post<ApiResponse<EvaluacionFinalResponse>>(`/api/v1/evaluaciones-finales/${instanciaId}/tutor`, data)
    return res.data.datos!
  },

  async registrarNotaFinal(instanciaId: number, notaFinal: number, observaciones?: string) {
    const res = await api.post<ApiResponse<NotaFinalResponse>>(`/api/v1/evaluaciones-finales/${instanciaId}/coordinador`, { notaFinal, observaciones })
    return res.data.datos!
  },

  async referencias(instanciaId: number) {
    const res = await api.get<ApiResponse<Record<string, EvaluacionFinalResponse | null>>>(`/api/v1/evaluaciones-finales/${instanciaId}/referencias`)
    return res.data.datos ?? {}
  },

  async enviarEncuestaTutor(instanciaId: number, data: EnviarEncuestaRequest) {
    const res = await api.post<ApiResponse<EncuestaResponse>>(`/api/v1/encuestas-satisfaccion/${instanciaId}/tutor/enviar`, data)
    return res.data.datos!
  },

  async enviarEncuestaEstudiante(instanciaId: number, data: EnviarEncuestaRequest) {
    const res = await api.post<ApiResponse<EncuestaResponse>>(`/api/v1/encuestas-satisfaccion/${instanciaId}/estudiante/enviar`, data)
    return res.data.datos!
  },

  async guardarBorradorEncuesta(encuestaId: number, respuestas: string[]) {
    const res = await api.patch<ApiResponse<EncuestaResponse>>(`/api/v1/encuestas-satisfaccion/${encuestaId}/borrador`, { respuestas })
    return res.data.datos!
  },

  async completarEncuesta(encuestaId: number, respuestas: string[]) {
    const res = await api.patch<ApiResponse<EncuestaResponse>>(`/api/v1/encuestas-satisfaccion/${encuestaId}/completar`, { respuestas })
    return res.data.datos!
  },

  async completarEncuestaPublica(token: string, respuestas: string[]) {
    const res = await api.patch<ApiResponse<EncuestaResponse>>(`/api/v1/encuestas-satisfaccion/publica/${token}/completar`, { respuestas })
    return res.data.datos!
  },

  async consultarEncuestaPublica(token: string) {
    const res = await api.get<ApiResponse<EncuestaResponse>>(`/api/v1/encuestas-satisfaccion/publica/${token}`)
    return res.data.datos!
  },

  async misEncuestas() {
    const res = await api.get<ApiResponse<EncuestaResponse[]>>('/api/v1/encuestas-satisfaccion/mis-encuestas')
    return res.data.datos ?? []
  },

  async listarEncuestasCoordinador() {
    const res = await api.get<ApiResponse<EncuestaCoordinadorResumen[]>>('/api/v1/encuestas-satisfaccion/coordinador/practicas')
    return res.data.datos ?? []
  },

  async listarEncuestasCoordinadorPaginado(page = 0, size = 20) {
    const res = await api.get<ApiResponse<Pageable<EncuestaCoordinadorResumen>>>('/api/v1/encuestas-satisfaccion/coordinador/practicas/page', { params: { page, size } })
    return res.data.datos!
  },

  async programarSustentacion(instanciaId: number, fecha: string, jurados: string[]) {
    const res = await api.post<ApiResponse<SustentacionResponse>>(`/api/v1/cierre-practicas/${instanciaId}/sustentacion`, { fecha, jurados })
    return res.data.datos!
  },

  async registrarResultadoSustentacion(instanciaId: number, resultado: ResultadoSustentacion, actaUrl: string, actaFirmada: boolean) {
    const res = await api.patch<ApiResponse<SustentacionResponse>>(`/api/v1/cierre-practicas/${instanciaId}/sustentacion/resultado`, { resultado, actaUrl, actaFirmada })
    return res.data.datos!
  },

  async checklist(instanciaId: number) {
    const res = await api.get<ApiResponse<ChecklistCierreResponse>>(`/api/v1/cierre-practicas/${instanciaId}/checklist`)
    return res.data.datos!
  },

  async ejecutarCierre(instanciaId: number) {
    const res = await api.post<ApiResponse<CierreFormalResponse>>(`/api/v1/cierre-practicas/${instanciaId}/ejecutar`, { confirmarCierreIrreversible: true })
    return res.data.datos!
  },

  async reporteEstadoProceso(params: { facultadId?: number; programaId?: number; semestreAcademico?: string; formato?: TipoExportacionReporte }) {
    const res = await api.get<ApiResponse<ReporteEstadoProcesoResponse>>('/api/v1/reportes-sprint4/estado-proceso', { params })
    return res.data.datos!
  },

  async tableroDireccion() {
    const res = await api.get<ApiResponse<TableroGerencialResponse>>('/api/v1/reportes-sprint4/tablero-direccion')
    return res.data.datos!
  },

  async configurarPrograma(programaId: number, data: ConfigurarProgramaRequest) {
    const res = await api.post<ApiResponse<ProgramaConfiguracionResponse>>(`/api/v1/configuracion-sprint4/programas/${programaId}`, data)
    return res.data.datos!
  },

  async obtenerConfiguracionPrograma(programaId: number) {
    const res = await api.get<ApiResponse<ProgramaConfiguracionResponse>>(`/api/v1/configuracion-sprint4/programas/${programaId}`)
    return res.data.datos!
  },

  async listarPlantillas() {
    const res = await api.get<ApiResponse<PlantillaNotificacionResponse[]>>('/api/v1/configuracion-sprint4/notificaciones')
    return res.data.datos ?? []
  },

  async obtenerPlantilla(tipoEvento: TipoEventoNotificacion) {
    const res = await api.get<ApiResponse<PlantillaNotificacionResponse | null>>(`/api/v1/configuracion-sprint4/notificaciones/${tipoEvento}`)
    return res.data.datos ?? null
  },

  async eliminarPlantilla(tipoEvento: TipoEventoNotificacion) {
    await api.delete(`/api/v1/configuracion-sprint4/notificaciones/${tipoEvento}`)
  },

  async guardarPlantilla(data: PlantillaNotificacionRequest) {
    const res = await api.post<ApiResponse<PlantillaNotificacionResponse>>('/api/v1/configuracion-sprint4/notificaciones', data)
    return res.data.datos!
  },

  async previsualizarPlantilla(data: PlantillaNotificacionRequest) {
    const res = await api.post<ApiResponse<string>>('/api/v1/configuracion-sprint4/notificaciones/previsualizar', data)
    return res.data.datos ?? ''
  },
}
