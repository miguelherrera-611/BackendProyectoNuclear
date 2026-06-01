import { useEffect, useState } from 'react'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'

export default function SustentacionesPage() {
  const [practicas, setPracticas] = useState<InstanciaPracticaResponseV2[]>([])
  const [loading, setLoading]     = useState(true)

  useEffect(() => {
    seguimientoService.misPracticantes()
      .then(lista => setPracticas(lista.filter(p => p.estado === 'FINALIZADA' || p.estado === 'EN_CURSO')))
      .finally(() => setLoading(false))
  }, [])

  const finalizadas = practicas.filter(p => p.estado === 'FINALIZADA')
  const enCurso     = practicas.filter(p => p.estado === 'EN_CURSO')

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Sustentaciones</h1>
        <p className="text-sm text-gray-500 mt-1">Gestiona las sustentaciones finales de tus practicantes.</p>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3">
        <span className="text-amber-500 text-xl mt-0.5">🔔</span>
        <div className="text-sm text-amber-800">
          <p className="font-semibold mb-0.5">Módulo en desarrollo</p>
          <p>La programación y calificación de sustentaciones estará disponible próximamente.</p>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
        </div>
      ) : (
        <>
          {enCurso.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
                En curso — pendientes de finalizar ({enCurso.length})
              </h2>
              {enCurso.map(p => (
                <div key={p.id} className="card flex items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 bg-cue-light rounded-xl flex items-center justify-center text-xl">🎓</div>
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="font-medium text-gray-800">{p.nombre}</p>
                        <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-green-100 text-green-800">En curso</span>
                      </div>
                      <p className="text-xs text-gray-500 mt-0.5">{p.razonSocialEmpresa ?? '—'}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
          {finalizadas.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
                Finalizadas — programar sustentación ({finalizadas.length})
              </h2>
              {finalizadas.map(p => (
                <div key={p.id} className="card flex items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 bg-cue-light rounded-xl flex items-center justify-center text-xl">🎓</div>
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="font-medium text-gray-800">{p.nombre}</p>
                        <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">Finalizada</span>
                      </div>
                      <p className="text-xs text-gray-500 mt-0.5">{p.razonSocialEmpresa ?? '—'}</p>
                    </div>
                  </div>
                  <span className="text-xs bg-amber-100 text-amber-700 px-3 py-1 rounded-full font-medium whitespace-nowrap">Próximamente</span>
                </div>
              ))}
            </div>
          )}
          {practicas.length === 0 && (
            <div className="card text-center py-16">
              <div className="text-gray-300 text-5xl mb-3">🎓</div>
              <p className="text-gray-500 text-sm">No tienes practicantes para sustentar aún.</p>
            </div>
          )}
        </>
      )}
    </div>
  )
}
