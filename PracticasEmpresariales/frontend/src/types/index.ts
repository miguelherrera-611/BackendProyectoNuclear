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
  | 'CREAR' | 'EDITAR' | 'DESACTIVAR' | 'ACTIVAR'
  | 'CAMBIO_ESTADO' | 'ACCESO_NO_AUTORIZADO'
  | 'RESET_PASSWORD' | 'CAMBIO_PASSWORD' | 'EXPORTAR' | 'CONSULTAR'

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

export type EstadoEmpresa = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'INACTIVA'
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

export interface TutorEmpresarialResponse {
  id: number
  nombre: string
  cargo?: string
  correo: string
  telefono?: string
  empresaId: number
  razonSocialEmpresa: string
  disponible: boolean
  activo: boolean
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
