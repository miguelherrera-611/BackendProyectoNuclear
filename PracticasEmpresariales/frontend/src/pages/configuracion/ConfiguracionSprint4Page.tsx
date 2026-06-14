import { usePlantillaNotificacion } from '../../hooks/usePlantillaNotificacion'
import { PlantillasNotificacionPanel } from '../../components/configuracion/PlantillasNotificacionPanel'

export default function ConfiguracionSprint4Page() {
  const plantillaNotificacion = usePlantillaNotificacion()

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Configuración del sistema</h1>
        <p className="text-sm text-gray-500 mt-1">
          Personaliza los correos automáticos que el sistema envía cuando ocurren eventos clave en las prácticas.
        </p>
      </div>

      <div className="card">
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Plantillas de correo</h2>
          <p className="text-sm text-gray-500 mt-1">
            Configura el contenido de cada notificación automática según el evento del ciclo de práctica.
          </p>
        </div>
        <PlantillasNotificacionPanel
          plantilla={plantillaNotificacion.plantilla}
          guardando={plantillaNotificacion.guardando}
          onCambiarEvento={plantillaNotificacion.cambiarEvento}
          onCambiarPlantilla={plantillaNotificacion.setPlantilla}
          onGuardar={plantillaNotificacion.guardar}
        />
      </div>
    </div>
  )
}
