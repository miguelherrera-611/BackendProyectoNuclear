import { useState } from 'react'
import { usePlantillaNotificacion } from '../../hooks/usePlantillaNotificacion'
import { PlantillasNotificacionPanel } from '../../components/configuracion/PlantillasNotificacionPanel'
import { ConfirmModal } from '../../components/common/Modal/Modal'
import { ETIQUETAS_EVENTO, EVENTOS_NOTIFICACION } from '../../constants/configuracion'
import type { TipoEventoNotificacion } from '../../types'

export default function ConfiguracionSprint4Page() {
  const { plantilla, setPlantilla, guardando, cargando, guardar, cambiarEvento, guardadas, eliminar } =
    usePlantillaNotificacion()

  const [confirmEliminar, setConfirmEliminar] = useState<TipoEventoNotificacion | null>(null)

  const editarPlantilla = (evento: TipoEventoNotificacion) => {
    cambiarEvento(evento)
    setTimeout(() => {
      document.getElementById('editor-plantillas')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 50)
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <ConfirmModal
        open={confirmEliminar !== null}
        title="¿Eliminar plantilla?"
        message={confirmEliminar ? `Se eliminará la plantilla para el evento "${ETIQUETAS_EVENTO[confirmEliminar]}". El sistema usará el correo genérico por defecto hasta que configures una nueva.` : ''}
        confirmLabel="Eliminar"
        variant="danger"
        onConfirm={async () => { if (confirmEliminar) { await eliminar(confirmEliminar); setConfirmEliminar(null) } }}
        onCancel={() => setConfirmEliminar(null)}
      />
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Plantillas de Correo</h1>
        <p className="text-sm text-gray-500 mt-1">
          Personaliza los correos automáticos que el sistema envía cuando ocurren eventos clave en las prácticas.
        </p>
      </div>

      {/* ── Tabla resumen de todas las plantillas ─────────────────────── */}
      <div className="card">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-gray-900">Estado de las plantillas</h2>
            <p className="text-xs text-gray-400 mt-0.5">
              {guardadas.size} de {EVENTOS_NOTIFICACION.length} eventos configurados
            </p>
          </div>
        </div>

        {cargando ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-7 w-7 border-b-2 border-cue-primary" />
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {EVENTOS_NOTIFICACION.map(ev => {
              const guardada = guardadas.get(ev)
              return (
                <div key={ev} className="flex items-center justify-between py-3 gap-4">
                  <div className="flex items-center gap-3 min-w-0">
                    {guardada ? (
                      <span className="shrink-0 w-5 h-5 rounded-full bg-green-100 text-green-600 flex items-center justify-center text-xs font-bold">✓</span>
                    ) : (
                      <span className="shrink-0 w-5 h-5 rounded-full bg-gray-100 text-gray-400 flex items-center justify-center text-xs">○</span>
                    )}
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-800 truncate">{ETIQUETAS_EVENTO[ev]}</p>
                      {guardada ? (
                        <p className="text-xs text-gray-400 truncate">{guardada.asunto || <em>Sin asunto</em>}</p>
                      ) : (
                        <p className="text-xs text-amber-500">Sin configurar</p>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    <button
                      onClick={() => editarPlantilla(ev)}
                      className="text-xs text-cue-primary hover:underline font-medium"
                    >
                      {guardada ? 'Editar' : 'Configurar'}
                    </button>
                    {guardada && (
                      <button
                        onClick={() => setConfirmEliminar(ev)}
                        className="text-xs text-red-500 hover:underline font-medium"
                      >
                        Eliminar
                      </button>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* ── Editor de plantilla ───────────────────────────────────────── */}
      <div className="card" id="editor-plantillas">
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Editor de plantilla</h2>
          <p className="text-sm text-gray-500 mt-1">
            Selecciona el evento y redacta el correo que se enviará automáticamente.
          </p>
        </div>

        {cargando ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
          </div>
        ) : (
          <PlantillasNotificacionPanel
            plantilla={plantilla}
            guardando={guardando}
            guardadas={guardadas}
            onCambiarEvento={cambiarEvento}
            onCambiarPlantilla={setPlantilla}
            onGuardar={guardar}
          />
        )}
      </div>
    </div>
  )
}
