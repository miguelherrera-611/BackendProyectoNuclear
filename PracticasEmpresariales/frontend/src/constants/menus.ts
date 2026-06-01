import { Rol } from '../types'

export interface MenuItem {
  id: string
  label: string
  ruta: string
  icono: string
}

export const MENUS_POR_ROL: Record<Rol, MenuItem[]> = {
  ADMIN_DTI: [
    { id: 'dashboard',  label: 'Panel Principal',    ruta: '/dashboard',  icono: '🏠' },
    { id: 'usuarios',   label: 'Gestión de Usuarios', ruta: '/usuarios',   icono: '👥' },
    { id: 'facultades', label: 'Facultades',           ruta: '/facultades', icono: '🏛️' },
    { id: 'programas',  label: 'Programas',            ruta: '/programas',  icono: '📚' },
    { id: 'auditoria',  label: 'Bitácora de Auditoría', ruta: '/auditoria', icono: '📋' },
  ],
  COORDINACION_ACADEMICA: [
    { id: 'dashboard',    label: 'Panel Principal',        ruta: '/dashboard',              icono: '🏠' },
    { id: 'validacion',   label: 'Validar Estudiantes',    ruta: '/validacion-estudiantes', icono: '✅' },
    { id: 'practicas',    label: 'Catálogo de Prácticas',  ruta: '/practicas',              icono: '📄' },
  ],
  COORDINADOR_PRACTICAS: [
    { id: 'dashboard',    label: 'Panel Principal',       ruta: '/dashboard',          icono: '🏠' },
    { id: 'empresas',     label: 'Empresas',              ruta: '/empresas',           icono: '🏢' },
    { id: 'vacantes',     label: 'Vacantes',              ruta: '/vacantes',           icono: '💼' },
    { id: 'tutores',      label: 'Tutores Empresariales', ruta: '/tutores',            icono: '👨‍💼' },
    { id: 'estudiantes',  label: 'Estudiantes APTOS',     ruta: '/estudiantes',        icono: '✅' },
    { id: 'asignaciones', label: 'Asignaciones',          ruta: '/asignaciones',       icono: '🔗' },
    { id: 'tablero',      label: 'Tablero Seguimiento',   ruta: '/tablero-seguimiento',icono: '📊' },
  ],
  DOCENTE_ASESOR: [
    { id: 'dashboard',      label: 'Panel Principal',    ruta: '/dashboard',           icono: '🏠' },
    { id: 'practicantes',   label: 'Mis Practicantes',   ruta: '/mis-practicantes',    icono: '👨‍🎓' },
    { id: 'seguimientos',   label: 'Revisar Seguimientos', ruta: '/seguimientos',      icono: '📝' },
    { id: 'tablero',        label: 'Tablero General',    ruta: '/tablero-seguimiento', icono: '📊' },
    { id: 'sustentaciones', label: 'Sustentaciones',     ruta: '/sustentaciones',      icono: '🎓' },
  ],
  TUTOR_EMPRESARIAL: [
    { id: 'dashboard',    label: 'Panel Principal',      ruta: '/dashboard',                  icono: '🏠' },
    { id: 'practicantes', label: 'Mis Practicantes',     ruta: '/mis-practicantes-empresa',   icono: '👨‍💼' },
    { id: 'planes',       label: 'Aprobar Planes',       ruta: '/planes',                     icono: '📋' },
    { id: 'encuestas',    label: 'Encuestas',            ruta: '/encuestas',                  icono: '📊' },
  ],
  ESTUDIANTE: [
    { id: 'dashboard',   label: 'Mi Panel',            ruta: '/dashboard',               icono: '🏠' },
    { id: 'practica',    label: 'Mi Práctica',         ruta: '/mi-practica',             icono: '💼' },
    { id: 'plan',        label: 'Mi Plan',             ruta: '/mi-practica/plan',        icono: '📋' },
    { id: 'seguimiento', label: 'Seguimiento Semanal', ruta: '/mi-practica/seguimiento', icono: '📝' },
    { id: 'documentos',  label: 'Mis Documentos',      ruta: '/mis-documentos',          icono: '📁' },
    { id: 'expediente',  label: 'Mi Expediente',       ruta: '/mi-expediente',           icono: '🗂️' },
  ],
  DIRECCION: [
    { id: 'dashboard',   label: 'Panel Gerencial', ruta: '/dashboard',   icono: '🏠' },
    { id: 'indicadores', label: 'Indicadores',     ruta: '/indicadores', icono: '📈' },
    { id: 'reportes',    label: 'Reportes',        ruta: '/reportes',    icono: '📊' },
  ],
}
