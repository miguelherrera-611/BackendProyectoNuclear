import { Input } from '../common/Input/Input'
import { Select } from '../common/Select/Select'
import { Button } from '../common/Button/Button'
import type { ProgramaResponse } from '../../types'
import type { ConfigurarProgramaRequest } from '../../services/sprint4Service'
import { REQUISITOS_CIERRE } from '../../constants/configuracion'

interface Props {
  programas: ProgramaResponse[]
  programaId: string
  config: ConfigurarProgramaRequest
  cargando: boolean
  guardando: boolean
  onSeleccionarPrograma: (id: string) => void
  onCambiarConfig: (config: ConfigurarProgramaRequest) => void
  onGuardar: () => void
}

export function ParametrosProgramaPanel({
  programas,
  programaId,
  config,
  cargando,
  guardando,
  onSeleccionarPrograma,
  onCambiarConfig,
  onGuardar,
}: Props) {
  const requisitosActivos = config.requisitosCierre
    ? config.requisitosCierre.split(',').map(r => r.trim()).filter(Boolean)
    : []

  const toggleRequisito = (valor: string) => {
    const siguiente = requisitosActivos.includes(valor)
      ? requisitosActivos.filter(r => r !== valor)
      : [...requisitosActivos, valor]
    onCambiarConfig({ ...config, requisitosCierre: siguiente.join(',') })
  }

  return (
    <div className="space-y-6">
      <Select
        label="Programa académico"
        required
        value={programaId}
        onChange={e => onSeleccionarPrograma(e.target.value)}
        hint="Cada programa puede tener parámetros distintos. Selecciónalo para ver y editar su configuración vigente."
      >
        <option value="">Selecciona un programa para cargar su configuración</option>
        {programas.map(p => (
          <option key={p.id} value={p.id}>{p.nombre}</option>
        ))}
      </Select>

      {cargando ? (
        <div className="flex items-center gap-3 py-8 justify-center text-gray-400">
          <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-cue-accent" />
          <span className="text-sm">Cargando configuración del programa...</span>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Número de prácticas"
              type="number"
              min={1}
              value={config.numeroPracticas}
              onChange={e => onCambiarConfig({ ...config, numeroPracticas: Number(e.target.value) })}
              hint="Cantidad total de prácticas que puede cursar un estudiante del programa."
              disabled={!programaId}
            />
            <Input
              label="Semanas de seguimiento"
              type="number"
              min={1}
              value={config.semanasSeguimiento}
              onChange={e => onCambiarConfig({ ...config, semanasSeguimiento: Number(e.target.value) })}
              hint="Duración en semanas del período de seguimiento de cada práctica."
              disabled={!programaId}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Nota mínima de aprobación (0.0 – 5.0)"
              type="number"
              step="0.1"
              min={0}
              max={5}
              value={config.notaMinimaAprobacion}
              onChange={e => onCambiarConfig({ ...config, notaMinimaAprobacion: Number(e.target.value) })}
              hint="El estudiante debe obtener esta nota o superior para aprobar la práctica."
              disabled={!programaId}
            />
            <Input
              label="Días de inactividad permitidos"
              type="number"
              min={1}
              value={config.umbralInactividadDias}
              onChange={e => onCambiarConfig({ ...config, umbralInactividadDias: Number(e.target.value) })}
              hint="Si no hay reportes de seguimiento en este plazo, la práctica se marcará como inactiva."
              disabled={!programaId}
            />
          </div>

          <div>
            <p className="text-sm font-medium text-gray-700 mb-1">Requisitos para el cierre formal</p>
            <p className="text-xs text-gray-400 mb-3">
              Selecciona qué condiciones deben cumplirse antes de poder cerrar una práctica oficialmente.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {REQUISITOS_CIERRE.map(req => (
                <label
                  key={req.valor}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg border cursor-pointer transition-colors ${
                    !programaId
                      ? 'opacity-50 cursor-not-allowed border-gray-200 bg-gray-50'
                      : requisitosActivos.includes(req.valor)
                      ? 'border-cue-accent bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300 bg-white'
                  }`}
                >
                  <input
                    type="checkbox"
                    className="accent-cue-accent h-4 w-4 shrink-0"
                    checked={requisitosActivos.includes(req.valor)}
                    onChange={() => toggleRequisito(req.valor)}
                    disabled={!programaId}
                  />
                  <span className="text-sm text-gray-700">{req.etiqueta}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="pt-2">
            <Button
              className="w-full sm:w-auto"
              disabled={!programaId}
              loading={guardando}
              onClick={onGuardar}
            >
              Guardar configuración
            </Button>
            {!programaId && (
              <p className="text-xs text-gray-400 mt-2">
                Selecciona un programa para poder guardar.
              </p>
            )}
          </div>
        </>
      )}
    </div>
  )
}
