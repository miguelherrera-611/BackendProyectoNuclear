import { useRef, useState } from 'react'
import { Input } from '../common/Input/Input'
import { Button } from '../common/Button/Button'
import type { PlantillaNotificacionRequest } from '../../services/sprint4Service'
import type { PlantillaNotificacionResponse, TipoEventoNotificacion } from '../../types'
import {
  ETIQUETAS_EVENTO,
  EVENTOS_NOTIFICACION,
  ROLES_RECEPTORES_OPCIONES,
  VARIABLES_PLANTILLA,
} from '../../constants/configuracion'

// ── Conversión texto plano ↔ HTML ──────────────────────────────────────────────

function htmlATexto(html: string): string {
  return html
    .replace(/<\/p>\s*<p>/gi, '\n\n')
    .replace(/<p>/gi, '')
    .replace(/<\/p>/gi, '')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .trim()
}

function textoAHtml(texto: string): string {
  return texto
    .split('\n\n')
    .filter(p => p.trim())
    .map(p => `<p>${p.replace(/\n/g, '<br>')}</p>`)
    .join('')
}

// ── Datos de ejemplo para la vista previa ─────────────────────────────────────

const DATOS_EJEMPLO: Record<string, string> = {
  nombre_estudiante: 'Ana García López',
  nombre_docente:    'Prof. Carlos Rodríguez',
  nombre_tutor:      'Ing. Laura Martínez',
  programa:          'Ingeniería de Sistemas',
  empresa:           'TechCorp S.A.S.',
  nota_final:        '4.2',
  resultado:         'APROBADO',
  fecha_cierre:      '10 de junio de 2026',
}

function aplicarEjemplos(texto: string): string {
  return texto.replace(/\{\{(\w+)\}\}/g, (_, clave) => DATOS_EJEMPLO[clave] ?? `[${clave}]`)
}

// ── Tipos ─────────────────────────────────────────────────────────────────────

type CampoActivo = 'asunto' | 'cuerpo'

interface Props {
  plantilla: PlantillaNotificacionRequest
  guardando: boolean
  guardadas: Map<TipoEventoNotificacion, PlantillaNotificacionResponse>
  onCambiarEvento: (evento: TipoEventoNotificacion) => void
  onCambiarPlantilla: (plantilla: PlantillaNotificacionRequest) => void
  onGuardar: () => void
}

// ── Componente ────────────────────────────────────────────────────────────────

export function PlantillasNotificacionPanel({
  plantilla,
  guardando,
  guardadas,
  onCambiarEvento,
  onCambiarPlantilla,
  onGuardar,
}: Props) {
  const asuntoRef = useRef<HTMLInputElement>(null)
  const cuerpoRef = useRef<HTMLTextAreaElement>(null)
  const campoActivo = useRef<CampoActivo>('cuerpo')

  // El cuerpo se edita como texto plano; el hook guarda HTML para la API
  const [cuerpoTexto, setCuerpoTexto] = useState(() => htmlATexto(plantilla.cuerpo))

  const rolesActivos = plantilla.rolesReceptores
    ? plantilla.rolesReceptores.split(',').map(r => r.trim()).filter(Boolean)
    : []

  const actualizarCuerpo = (texto: string) => {
    setCuerpoTexto(texto)
    onCambiarPlantilla({ ...plantilla, cuerpo: textoAHtml(texto) })
  }

  const toggleRol = (valor: string) => {
    const siguiente = rolesActivos.includes(valor)
      ? rolesActivos.filter(r => r !== valor)
      : [...rolesActivos, valor]
    onCambiarPlantilla({ ...plantilla, rolesReceptores: siguiente.join(',') })
  }

  const insertarVariable = (variable: string) => {
    const esAsunto = campoActivo.current === 'asunto'
    const el = esAsunto
      ? (asuntoRef.current as HTMLInputElement | null)
      : (cuerpoRef.current as HTMLTextAreaElement | null)
    if (!el) return

    const inicio = el.selectionStart ?? el.value.length
    const fin = el.selectionEnd ?? el.value.length
    const nuevo = el.value.slice(0, inicio) + variable + el.value.slice(fin)
    const nuevaPosicion = inicio + variable.length

    if (esAsunto) {
      onCambiarPlantilla({ ...plantilla, asunto: nuevo })
    } else {
      actualizarCuerpo(nuevo)
    }

    setTimeout(() => {
      el.focus()
      el.setSelectionRange(nuevaPosicion, nuevaPosicion)
    }, 0)
  }

  // Vista previa en tiempo real con datos de ejemplo
  const asuntoPreview = aplicarEjemplos(plantilla.asunto)
  const cuerpoPreview = aplicarEjemplos(textoAHtml(cuerpoTexto))
  const rolesEtiquetas = ROLES_RECEPTORES_OPCIONES
    .filter(r => rolesActivos.includes(r.valor))
    .map(r => r.etiqueta)
    .join(', ') || 'Nadie seleccionado'

  const eventoGuardado = guardadas.has(plantilla.tipoEvento)
  const totalGuardadas = guardadas.size

  return (
    <div className="space-y-6">

      {/* Resumen de estado */}
      <div className="flex items-center justify-between bg-gray-50 border border-gray-200 rounded-lg px-4 py-2.5">
        <span className="text-sm text-gray-600">
          <span className="font-semibold text-gray-800">{totalGuardadas}</span> de {EVENTOS_NOTIFICACION.length} eventos configurados
        </span>
        {eventoGuardado ? (
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-green-100 text-green-700">
            ✓ Plantilla guardada
          </span>
        ) : (
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-amber-100 text-amber-700">
            Sin configurar
          </span>
        )}
      </div>

      {/* Evento */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          ¿Cuándo se envía este correo? <span className="text-red-500">*</span>
        </label>
        <select
          className="input-field"
          value={plantilla.tipoEvento}
          onChange={e => onCambiarEvento(e.target.value as TipoEventoNotificacion)}
        >
          {EVENTOS_NOTIFICACION.map(ev => (
            <option key={ev} value={ev}>
              {guardadas.has(ev) ? '✓ ' : '○ '}{ETIQUETAS_EVENTO[ev]}
            </option>
          ))}
        </select>
        <p className="text-xs text-gray-400 mt-1">
          El sistema enviará este correo automáticamente cada vez que ocurra esta situación.
        </p>
      </div>

      {/* Editor + Vista previa */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* — Editor — */}
        <div className="space-y-4">
          <p className="text-sm font-semibold text-gray-700">Redacción del correo</p>

          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">Asunto</label>
            <input
              ref={asuntoRef}
              className="input-field"
              value={plantilla.asunto}
              onFocus={() => { campoActivo.current = 'asunto' }}
              onChange={e => onCambiarPlantilla({ ...plantilla, asunto: e.target.value })}
              placeholder="Ej: Resultado de su práctica empresarial"
            />
          </div>

          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">Mensaje</label>
            <textarea
              ref={cuerpoRef}
              className="input-field resize-none font-sans"
              rows={8}
              value={cuerpoTexto}
              onFocus={() => { campoActivo.current = 'cuerpo' }}
              onChange={e => actualizarCuerpo(e.target.value)}
              placeholder="Escribe aquí el contenido del correo..."
            />
            <p className="text-xs text-gray-400">
              Haz clic en una variable para insertarla en el campo donde estés escribiendo.
            </p>
          </div>

          {/* Chips de variables */}
          <div className="space-y-2">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
              Variables disponibles — haz clic para insertar
            </p>
            <div className="flex flex-wrap gap-2">
              {VARIABLES_PLANTILLA.map(v => (
                <button
                  key={v.variable}
                  type="button"
                  title={v.descripcion}
                  onClick={() => insertarVariable(v.variable)}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-mono bg-blue-50 text-cue-accent border border-blue-200 hover:bg-blue-100 hover:border-cue-accent transition-colors"
                >
                  <span className="text-gray-400 font-sans font-normal">{v.descripcion}</span>
                  <span>{v.variable}</span>
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* — Vista previa — */}
        <div className="space-y-2">
          <p className="text-sm font-semibold text-gray-700">
            Vista previa
            <span className="ml-2 text-xs font-normal text-gray-400 normal-case">
              (con datos de ejemplo)
            </span>
          </p>
          <div className="border border-gray-200 rounded-xl overflow-hidden shadow-sm">
            {/* Cabecera del correo */}
            <div className="bg-gray-50 border-b border-gray-200 px-4 py-3 space-y-1">
              <div className="flex items-start gap-2 text-xs text-gray-500">
                <span className="font-medium w-14 shrink-0">Para:</span>
                <span>{rolesEtiquetas}</span>
              </div>
              <div className="flex items-start gap-2 text-xs">
                <span className="font-medium text-gray-500 w-14 shrink-0">Asunto:</span>
                <span className="text-gray-800 font-medium">{asuntoPreview || <em className="text-gray-300">Sin asunto</em>}</span>
              </div>
            </div>
            {/* Cuerpo del correo */}
            <div
              className="px-5 py-4 text-sm text-gray-700 leading-relaxed min-h-[140px] bg-white prose max-w-none"
              dangerouslySetInnerHTML={{
                __html: cuerpoPreview || '<p class="text-gray-300 italic">El mensaje aparecerá aquí...</p>',
              }}
            />
            <div className="bg-gray-50 border-t border-gray-100 px-4 py-2 flex items-center gap-1.5">
              <span className="text-gray-300 text-xs">ℹ</span>
              <span className="text-xs text-gray-400">
                Datos de ejemplo para ilustrar cómo quedará el correo real.
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Destinatarios */}
      <div>
        <p className="text-sm font-medium text-gray-700 mb-1">¿Quién recibe este correo?</p>
        <p className="text-xs text-gray-400 mb-3">
          Selecciona los roles que recibirán el correo cuando ocurra el evento.
        </p>
        <div className="flex flex-wrap gap-3">
          {ROLES_RECEPTORES_OPCIONES.map(rol => (
            <label
              key={rol.valor}
              className={`flex items-center gap-2 px-3 py-2 rounded-lg border cursor-pointer transition-colors ${
                rolesActivos.includes(rol.valor)
                  ? 'border-cue-accent bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300 bg-white'
              }`}
            >
              <input
                type="checkbox"
                className="accent-cue-accent h-4 w-4"
                checked={rolesActivos.includes(rol.valor)}
                onChange={() => toggleRol(rol.valor)}
              />
              <span className="text-sm text-gray-700">{rol.etiqueta}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Frecuencia de recordatorio */}
      <Input
        label="Días entre recordatorios"
        type="number"
        min={1}
        value={plantilla.frecuenciaRecordatorioDias}
        onChange={e => onCambiarPlantilla({ ...plantilla, frecuenciaRecordatorioDias: Number(e.target.value) })}
        hint="Si el destinatario no ha respondido, el sistema enviará el correo de nuevo cada estos días."
      />

      <div className="pt-2">
        <Button loading={guardando} onClick={onGuardar}>
          Guardar plantilla
        </Button>
      </div>
    </div>
  )
}
