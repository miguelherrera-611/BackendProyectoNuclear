import { Rol } from '../types'

export interface MenuItem {
  id: string
  label: string
  ruta: string
  icono: string
}

export const MENUS_POR_ROL: Record<Rol, MenuItem[]> = {
  ADMIN_DTI: [
    { id: 'dashboard', label: 'Panel Principal', ruta: '/dashboard', icono: 'Home' },
    { id: 'usuarios', label: 'Gestion de Usuarios', ruta: '/usuarios', icono: 'Users' },
    { id: 'facultades', label: 'Facultades', ruta: '/facultades', icono: 'Building' },
    { id: 'programas', label: 'Programas', ruta: '/programas', icono: 'Book' },
    { id: 'auditoria', label: 'Bitacora de Auditoria', ruta: '/auditoria', icono: 'Clipboard' },
  ],
  COORDINACION_ACADEMICA: [
    { id: 'dashboard', label: 'Panel Principal', ruta: '/dashboard', icono: 'Home' },
    { id: 'validacion', label: 'Validar Estudiantes', ruta: '/validacion-estudiantes', icono: 'Check' },
    { id: 'practicas', label: 'Catalogo de Practicas', ruta: '/practicas', icono: 'File' },
    { id: 'reportes', label: 'Reportes Proceso', ruta: '/reportes-proceso', icono: 'Chart' },
  ],
  COORDINADOR_PRACTICAS: [
    { id: 'dashboard', label: 'Panel Principal', ruta: '/dashboard', icono: 'Home' },
    { id: 'empresas', label: 'Empresas', ruta: '/empresas', icono: 'Building' },
    { id: 'vacantes', label: 'Vacantes', ruta: '/vacantes', icono: 'Briefcase' },
    { id: 'tutores', label: 'Tutores Empresariales', ruta: '/tutores', icono: 'UserTie' },
    { id: 'asignaciones', label: 'Asignaciones', ruta: '/asignaciones', icono: 'Link' },
    { id: 'reportes', label: 'Reportes Proceso', ruta: '/reportes-proceso', icono: 'Report' },
    { id: 'plantillas-correo', label: 'Plantillas de Correo', ruta: '/plantillas-correo', icono: 'Mail' },
  ],
  DOCENTE_ASESOR: [
    { id: 'dashboard', label: 'Panel Principal', ruta: '/dashboard', icono: 'Home' },
    { id: 'practicantes', label: 'Mis Practicantes', ruta: '/mis-practicantes', icono: 'Users' },
    { id: 'sustentaciones', label: 'Sustentaciones', ruta: '/sustentaciones', icono: 'Graduation' },
  ],
  TUTOR_EMPRESARIAL: [
    { id: 'dashboard', label: 'Panel Principal', ruta: '/dashboard', icono: 'Home' },
    { id: 'practicantes', label: 'Mis Practicantes', ruta: '/mis-practicantes-empresa', icono: 'Users' },
    { id: 'planes', label: 'Aprobar Planes', ruta: '/planes', icono: 'Clipboard' },
    { id: 'encuestas', label: 'Encuestas', ruta: '/encuestas', icono: 'Chart' },
  ],
  ESTUDIANTE: [
    { id: 'dashboard', label: 'Mi Panel', ruta: '/dashboard', icono: 'Home' },
    { id: 'practica', label: 'Mi Practica', ruta: '/mi-practica', icono: 'Briefcase' },
    { id: 'plan', label: 'Mi Plan', ruta: '/mi-practica/plan', icono: 'Clipboard' },
    { id: 'seguimiento', label: 'Seguimiento Semanal', ruta: '/mi-practica/seguimiento', icono: 'Edit' },
    { id: 'encuestas', label: 'Mis Encuestas', ruta: '/mis-encuestas', icono: 'Chart' },
    { id: 'documentos', label: 'Mis Documentos', ruta: '/mis-documentos', icono: 'Folder' },
    { id: 'expediente', label: 'Mi Expediente', ruta: '/mi-expediente', icono: 'Archive' },
  ],
  DIRECCION: [
    { id: 'dashboard', label: 'Panel Gerencial', ruta: '/dashboard', icono: 'Home' },
    { id: 'indicadores', label: 'Indicadores', ruta: '/indicadores', icono: 'Chart' },
    { id: 'reportes', label: 'Reportes', ruta: '/reportes', icono: 'Report' },
  ],
}
