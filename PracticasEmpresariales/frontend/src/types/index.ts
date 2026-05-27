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
