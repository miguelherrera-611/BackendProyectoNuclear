export type Rol =
  | 'ADMIN_DTI'
  | 'COORDINACION_ACADEMICA'
  | 'COORDINADOR_PRACTICAS'
  | 'DOCENTE_ASESOR'
  | 'TUTOR_EMPRESARIAL'
  | 'ESTUDIANTE'
  | 'DIRECCION'

export type EtiquetaCargo = 'COORDINACION_ACADEMICA' | 'SECRETARIA'

export type EstadoCuenta = 'PENDIENTE' | 'ACTIVO'

export type EstadoEstudiante = 'NO_APTO' | 'APTO'

export type TipoAccion =
  | 'LOGIN_EXITOSO' | 'LOGIN_FALLIDO' | 'LOGOUT'
  | 'CREAR' | 'EDITAR' | 'CONFIRMAR' | 'ASIGNAR'
  | 'DESACTIVAR' | 'ACTIVAR' | 'CAMBIO_ESTADO'
  | 'SUBIR_DOCUMENTO' | 'FIRMAR' | 'CERRAR' | 'CALIFICAR'
  | 'ACCESO_NO_AUTORIZADO' | 'CAMBIO_PASSWORD' | 'CAMBIO_CORREO' | 'RESET_PASSWORD'
  | 'EXPORTAR' | 'CONSULTAR'

export interface AuthUser {
  usuarioId: number
  nombre: string
  correo: string
  rol: Rol
  etiquetaCargo?: EtiquetaCargo
  primerIngreso: boolean
  facultadId?: number
  programaId?: number
  token: string
}

export interface UsuarioResponse {
  id: number
  nombre: string
  correo: string
  telefono?: string
  fotoPerfil?: string
  rol: Rol
  etiquetaCargo?: EtiquetaCargo
  activo: boolean
  primerIngreso: boolean
  ultimoAcceso?: string
  creadoEn: string
  facultadId?: number
  facultadNombre?: string
  programaId?: number
  programaNombre?: string
  empresaId?: number
  razonSocialEmpresa?: string
  estadoEstudiante?: EstadoEstudiante
  estadoCuenta?: EstadoCuenta
  identificacion?: string
  semestre?: number
  contactoEmergencia?: string
  enviadoAlProceso?: boolean
}

export interface FacultadResponse {
  id: number
  nombre: string
  descripcion?: string
  activa: boolean
  numeroProgramas: number
  tieneProgramasActivos: boolean
  programas: { id: number; nombre: string; activo: boolean; numeroTotalPracticas: number; promedioMinimoGeneral: number }[]
  creadaEn: string
}

export interface ProgramaResponse {
  id: number
  nombre: string
  descripcion?: string
  facultadId: number
  facultadNombre: string
  numeroTotalPracticas: number
  promedioMinimoGeneral: number
  activo: boolean
  creadoEn: string
}

export interface BitacoraResponse {
  id: number
  usuarioId?: number
  nombreUsuario: string
  rolUsuario: Rol
  etiquetaCargoUsuario?: EtiquetaCargo
  fechaHora: string
  modulo: string
  tipoAccion: TipoAccion
  registroAfectadoId?: number
  registroAfectadoTipo?: string
  valoresAnteriores?: string
  valoresNuevos?: string
  ipOrigen?: string
  exitoso: boolean
  motivoFallo?: string
}

export interface DashboardSeccion {
  id: string
  titulo: string
  ruta: string
  contador: number
}

export interface DashboardResponse {
  rol: Rol
  etiquetaCargo?: EtiquetaCargo
  nombreUsuario: string
  titulo: string
  soloLectura: boolean
  secciones: DashboardSeccion[]
}

export type EstadoEmpresa = 'ACTIVA' | 'INACTIVA'
export type EstadoVacante = 'PENDIENTE' | 'DISPONIBLE' | 'RECHAZADA' | 'CERRADA'

export interface EmpresaResponse {
  id: number
  razonSocial: string
  nit: string
  sector?: string
  direccion?: string
  municipio?: string
  telefono?: string
  nombreContacto: string
  correo?: string
  estado: EstadoEmpresa
  areasDisponibles: string[]
  creadoEn: string
}

export interface VacanteResponse {
  id: number
  empresaId: number
  razonSocialEmpresa: string
  area: string
  cuposTotales: number
  cuposOcupados: number
  estado: EstadoVacante
  fechaPublicacion: string
  motivoRechazo?: string
  creadoEn: string
}

export type EstadoPractica = 'ASIGNADA_PENDIENTE_INICIO' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA'
export type EstadoHojaDeVida = 'PENDIENTE' | 'VALIDA' | 'RECHAZADA'

export interface CatalogoPracticaResponse {
  id: number
  programaId: number
  programaNombre: string
  numeroPractica: number
  nombre: string
  materiaNucleo: string
  codigoMateria: string
  numCortes: number
  duracionSemanas: number
  documentosRequeridos?: string
  activo: boolean
  creadoEn: string
}

export interface InstanciaPracticaResponse {
  id: number
  numeroPractica: number
  nombre: string
  materiaNucleo: string
  codigoMateria: string
  numCortes: number
  duracionSemanas: number
  documentosRequeridos?: string
  estado: EstadoPractica
  evaluacionDocenteRegistrada?: boolean
  estudianteId?: number
  nombreEstudiante?: string
  empresaId?: number
  razonSocialEmpresa?: string
  docenteAsesorId?: number
  nombreDocenteAsesor?: string
  tutorEmpresarialId?: number
  nombreTutorEmpresarial?: string
  creadoEn: string
}

export interface HojaDeVidaResponse {
  id: number
  estudianteId: number
  version: number
  fechaCarga: string
  urlArchivo: string
  estado: EstadoHojaDeVida
  validadoPor?: number
  fechaValidacion?: string
  motivoRechazo?: string
  creadoEn: string
}

export interface ExpedienteResponse {
  expedienteId: number
  estudianteId: number
  nombreEstudiante: string
  identificacion?: string
  programa?: string
  semestre?: number
  estadoEstudiante: EstadoEstudiante
  hvActual?: HojaDeVidaResponse
  historialHv: HojaDeVidaResponse[]
  practicas: InstanciaPracticaResponse[]
  creadoEn: string
}

export interface ApiResponse<T> {
  exitoso: boolean
  mensaje?: string
  datos?: T
}

export interface Pageable<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// ── Sprint 3: Plan de práctica ────────────────────────────────────────────────
export type EstadoPlan = 'BORRADOR' | 'APROBADO_TUTOR' | 'APROBADO_DOCENTE' | 'RECHAZADO'

export interface PlanPracticaResponse {
  id: number
  instanciaPracticaId: number
  objetivos?: string
  cronograma?: string
  documentoNombre?: string
  estado: EstadoPlan
  cargadoPorId?: number
  aprobadoPorTutorEn?: string
  aprobadoPorDocenteEn?: string
  motivoRechazo?: string
  rechazadoPorId?: number
  creadoEn: string
  actualizadoEn: string
}

// ── Sprint 3: Seguimiento semanal ─────────────────────────────────────────────
export type EstadoSeguimiento = 'ENVIADO' | 'REVISADO' | 'RECHAZADO' | 'PENDIENTE' | 'APROBADO'

export interface SeguimientoSemanalResponse {
  id: number
  instanciaPracticaId: number
  semana: number
  actividades: string
  logros: string
  dificultades?: string
  evidencias?: string
  observacionesDocente?: string
  estado: EstadoSeguimiento
  creadoPorId?: number
  revisadoPorId?: number
  revisadoEn?: string
  creadoEn: string
  actualizadoEn: string
}

// ── Sprint 3: InstanciaPractica extendida ─────────────────────────────────────
export interface InstanciaPracticaResponseV2 extends InstanciaPracticaResponse {
  vacanteId?: number
  fechaInicio?: string
  fechaFin?: string
  fechaSustentacion?: string
  firmaTutor: boolean
  firmaDocente: boolean
  firmaEstudiante: boolean
  vinculacionConfirmadaEn?: string
  actualizadoEn?: string
  resultadoCierre?: ResultadoPractica
  fechaCierre?: string
  semestreAcademico?: string
}

// Sprint 4: evaluacion y cierre
export type TipoEvaluacionFinal = 'DOCENTE_ASESOR' | 'TUTOR_EMPRESARIAL'
export type EstadoEvaluacionFinal = 'PENDIENTE' | 'COMPLETADA' | 'FUERA_DE_PLAZO'
export type ResultadoPractica = 'APROBADO' | 'NO_APROBADO'
export type TipoEncuesta = 'PARA_TUTOR' | 'PARA_ESTUDIANTE'
export type EstadoEncuesta = 'PENDIENTE' | 'EN_BORRADOR' | 'COMPLETADA'
export type ResultadoSustentacion = 'APROBADO' | 'NO_APROBADO'
export type TipoExportacionReporte = 'EXCEL' | 'PDF'
export type TipoEventoNotificacion =
  | 'INICIO_PRACTICA'
  | 'EVALUACION_DOCENTE_COMPLETADA'
  | 'EVALUACION_TUTOR_COMPLETADA'
  | 'NOTA_FINAL_REGISTRADA'
  | 'ENCUESTA_TUTOR_ENVIADA'
  | 'ENCUESTA_ESTUDIANTE_ENVIADA'
  | 'ENCUESTA_COMPLETADA'
  | 'CIERRE_FORMAL_EJECUTADO'
  | 'COORDINACION_ACADEMICA_RESULTADO'

export interface CriterioEvaluacion {
  nombre: string
  peso: number
  puntaje: number
}

export interface EvaluacionFinalResponse {
  id: number
  instanciaPracticaId: number
  tipo: TipoEvaluacionFinal
  evaluadorId: number
  evaluadorNombre: string
  criterios: CriterioEvaluacion[]
  promedioFinal: number
  observaciones?: string
  estado: EstadoEvaluacionFinal
  fecha?: string
}

export interface NotaFinalResponse {
  id: number
  instanciaPracticaId: number
  notaFinal: number
  notaMinimaAplicada: number
  resultado: ResultadoPractica
  observaciones?: string
  fecha: string
}

export interface EncuestaResponse {
  id: number
  instanciaPracticaId: number
  titulo: string
  tipo: TipoEncuesta
  actorAsignadoId: number
  actorAsignadoCorreo: string
  enlaceDirecto?: string
  preguntas: string[]
  respuestas: string[]
  enviada: boolean
  completada: boolean
  estado: EstadoEncuesta
  fechaEnvio?: string
  fechaCompletada?: string
}

export interface EncuestaCoordinadorResumen {
  instanciaId: number
  nombrePractica: string
  nombreEstudiante: string
  programaNombre?: string
  nombreEmpresa?: string
  nombreDocenteAsesor?: string
  tutorEmpresarialId?: number
  nombreTutor?: string
  evaluacionDocenteCompleta: boolean
  encuestaTutorEnviada: boolean
  encuestaTutorCompletada: boolean
  encuestaEstudianteEnviada: boolean
  encuestaEstudianteCompletada: boolean
}

export interface SustentacionResponse {
  id: number
  instanciaPracticaId: number
  fecha: string
  jurados: string[]
  actaUrl?: string
  actaFirmada: boolean
  resultado?: ResultadoSustentacion
  completa: boolean
}

export interface ChecklistItemResponse {
  codigo: string
  nombre: string
  completo: boolean
  estadoVisual: string
  accionRequerida?: string
}

export interface ChecklistCierreResponse {
  instanciaPracticaId: number
  puedeEjecutarCierre: boolean
  items: ChecklistItemResponse[]
}

export interface CierreFormalResponse {
  instanciaPracticaId: number
  estado: EstadoPractica
  resultado: ResultadoPractica
  notaFinal: number
  codigoPazYSalvo?: string
  pazYSalvo?: string
}

export interface ReporteEstadoProcesoResponse {
  estados: Record<string, number>
  total: number
  exportacion?: string
  nombreArchivo?: string
  contentType?: string
}

export interface TableroGerencialResponse {
  practicantesEnCursoPorPrograma: Record<string, number>
  tasaAprobacionGlobal: number
  empresasActivas: number
}

export interface ProgramaConfiguracionResponse {
  id?: number
  programaId: number
  numeroPracticas: number
  semanasSeguimiento: number
  notaMinimaAprobacion: number
  requisitosCierre?: string
  umbralInactividadDias: number
  vigente: boolean
}

export interface PlantillaNotificacionResponse {
  id: number
  tipoEvento: TipoEventoNotificacion
  asunto: string
  cuerpo: string
  rolesReceptores?: string
  frecuenciaRecordatorioDias: number
}
