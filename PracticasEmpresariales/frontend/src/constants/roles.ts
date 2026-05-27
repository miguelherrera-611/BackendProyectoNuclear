import { Rol } from '../types'

export const ROL_LABELS: Record<Rol, string> = {
  ADMIN_DTI: 'Administrador DTI',
  COORDINACION_ACADEMICA: 'Coordinación Académica',
  COORDINADOR_PRACTICAS: 'Coordinador de Prácticas',
  DOCENTE_ASESOR: 'Docente Asesor',
  TUTOR_EMPRESARIAL: 'Tutor Empresarial',
  ESTUDIANTE: 'Estudiante Practicante',
  DIRECCION: 'Dirección',
}

/** Roles que pueden escribir (no solo lectura) */
export const ROLES_ESCRITURA: Rol[] = [
  'ADMIN_DTI',
  'COORDINACION_ACADEMICA',
  'COORDINADOR_PRACTICAS',
  'DOCENTE_ASESOR',
  'TUTOR_EMPRESARIAL',
  'ESTUDIANTE',
]

export const esSoloLectura = (rol: Rol): boolean => rol === 'DIRECCION'
