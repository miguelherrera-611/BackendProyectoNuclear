import { useState } from 'react'
import { useConfiguracionPrograma } from '../../hooks/useConfiguracionPrograma'
import { usePlantillaNotificacion } from '../../hooks/usePlantillaNotificacion'
import { ParametrosProgramaPanel } from '../../components/configuracion/ParametrosProgramaPanel'
import { PlantillasNotificacionPanel } from '../../components/configuracion/PlantillasNotificacionPanel'

type Pestana = 'parametros' | 'plantillas'

const PESTANAS: { id: Pestana; titulo: string; descripcion: string }[] = [
  {
    id: 'parametros',
    titulo: 'Parámetros por programa',
    descripcion: 'Define las reglas operativas de cada programa: número de prácticas, notas mínimas y requisitos de cierre.',
  },
  {
    id: 'plantillas',
    titulo: 'Plantillas de correo',
    descripcion: 'Personaliza los correos automáticos que el sistema envía cuando ocurren eventos clave en las prácticas.',
  },
]

export default function ConfiguracionSprint4Page() {
  const [pestanaActiva, setPestanaActiva] = useState<Pestana>('parametros')

  const configuracionPrograma = useConfiguracionPrograma()
  const plantillaNotificacion = usePlantillaNotificacion()

  const pestana = PESTANAS.find(p => p.id === pestanaActiva)!

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Configuración del sistema</h1>
        <p className="text-sm text-gray-500 mt-1">
          Ajusta los parámetros operativos y las comunicaciones automáticas de la plataforma.
        </p>
      </div>

      <div className="border-b border-gray-200">
        <nav className="flex gap-1">
          {PESTANAS.map(p => (
            <button
              key={p.id}
              onClick={() => setPestanaActiva(p.id)}
              className={`px-4 py-2.5 text-sm font-medium rounded-t-lg transition-colors border-b-2 -mb-px ${
                pestanaActiva === p.id
                  ? 'border-cue-accent text-cue-accent bg-white'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {p.titulo}
            </button>
          ))}
        </nav>
      </div>

      <div className="card">
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-gray-900">{pestana.titulo}</h2>
          <p className="text-sm text-gray-500 mt-1">{pestana.descripcion}</p>
        </div>

        {pestanaActiva === 'parametros' && (
          <ParametrosProgramaPanel
            programas={configuracionPrograma.programas}
            programaId={configuracionPrograma.programaId}
            config={configuracionPrograma.config}
            cargando={configuracionPrograma.cargando}
            guardando={configuracionPrograma.guardando}
            onSeleccionarPrograma={configuracionPrograma.seleccionarPrograma}
            onCambiarConfig={configuracionPrograma.setConfig}
            onGuardar={configuracionPrograma.guardar}
          />
        )}

        {pestanaActiva === 'plantillas' && (
          <PlantillasNotificacionPanel
            plantilla={plantillaNotificacion.plantilla}
            guardando={plantillaNotificacion.guardando}
            onCambiarEvento={plantillaNotificacion.cambiarEvento}
            onCambiarPlantilla={plantillaNotificacion.setPlantilla}
            onGuardar={plantillaNotificacion.guardar}
          />
        )}
      </div>
    </div>
  )
}
