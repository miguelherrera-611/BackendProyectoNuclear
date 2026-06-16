import type { TipoEventoNotificacion } from '../types'

export const ETIQUETAS_EVENTO: Record<TipoEventoNotificacion, string> = {
  INICIO_PRACTICA:                  'Inicio de práctica (vinculación confirmada)',
  EVALUACION_DOCENTE_COMPLETADA:    'Evaluación del docente completada',
  EVALUACION_TUTOR_COMPLETADA:      'Evaluación del tutor completada',
  NOTA_FINAL_REGISTRADA:            'Nota final registrada',
  ENCUESTA_TUTOR_ENVIADA:           'Encuesta enviada al tutor',
  ENCUESTA_ESTUDIANTE_ENVIADA:      'Encuesta enviada al estudiante',
  ENCUESTA_COMPLETADA:              'Encuesta completada',
  CIERRE_FORMAL_EJECUTADO:          'Cierre formal ejecutado',
  COORDINACION_ACADEMICA_RESULTADO: 'Resultado enviado a coordinación académica',
}

export const EVENTOS_NOTIFICACION: TipoEventoNotificacion[] = [
  'INICIO_PRACTICA',
  'EVALUACION_DOCENTE_COMPLETADA',
  'EVALUACION_TUTOR_COMPLETADA',
  'NOTA_FINAL_REGISTRADA',
  'ENCUESTA_TUTOR_ENVIADA',
  'ENCUESTA_ESTUDIANTE_ENVIADA',
  'ENCUESTA_COMPLETADA',
  'CIERRE_FORMAL_EJECUTADO',
  'COORDINACION_ACADEMICA_RESULTADO',
]

export const REQUISITOS_CIERRE = [
  { valor: 'evaluacion_docente',  etiqueta: 'Evaluación del docente asesor' },
  { valor: 'evaluacion_tutor',    etiqueta: 'Evaluación del tutor empresarial' },
  { valor: 'nota_final',          etiqueta: 'Nota final registrada' },
  { valor: 'encuesta_tutor',      etiqueta: 'Encuesta de satisfacción (tutor)' },
  { valor: 'encuesta_estudiante', etiqueta: 'Encuesta de satisfacción (estudiante)' },
  { valor: 'documentos',          etiqueta: 'Documentos entregados' },
  { valor: 'sustentacion',        etiqueta: 'Sustentación realizada' },
]

export const ROLES_RECEPTORES_OPCIONES = [
  { valor: 'ESTUDIANTE',        etiqueta: 'Estudiante' },
  { valor: 'DOCENTE_ASESOR',    etiqueta: 'Docente asesor' },
  { valor: 'TUTOR_EMPRESARIAL', etiqueta: 'Tutor empresarial' },
]

export const VARIABLES_PLANTILLA = [
  { variable: '{{nombre_estudiante}}', descripcion: 'Nombre del estudiante' },
  { variable: '{{nombre_docente}}',    descripcion: 'Nombre del docente asesor' },
  { variable: '{{nombre_tutor}}',      descripcion: 'Nombre del tutor empresarial' },
  { variable: '{{programa}}',          descripcion: 'Nombre del programa académico' },
  { variable: '{{empresa}}',           descripcion: 'Nombre de la empresa' },
  { variable: '{{fecha_inicio}}',      descripcion: 'Fecha de inicio de la práctica' },
  { variable: '{{fecha_fin}}',         descripcion: 'Fecha de fin de la práctica' },
  { variable: '{{nota_final}}',        descripcion: 'Nota final de la práctica' },
  { variable: '{{resultado}}',         descripcion: 'Resultado: APROBADO / NO_APROBADO' },
  { variable: '{{fecha_cierre}}',      descripcion: 'Fecha del cierre formal' },
]

export const CONFIG_DEFAULTS = {
  numeroPracticas: 1,
  semanasSeguimiento: 12,
  notaMinimaAprobacion: 3.0,
  requisitosCierre: 'evaluacion_docente,evaluacion_tutor,nota_final,encuesta_tutor,encuesta_estudiante,documentos,sustentacion',
  umbralInactividadDias: 7,
}
