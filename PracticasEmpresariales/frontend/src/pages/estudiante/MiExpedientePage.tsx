import { useEffect, useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { expedienteService } from '../../services/expedienteService'
import type { ExpedienteResponse, EstadoPractica } from '../../types'

const ESTADO_PRACTICA_BADGE: Record<EstadoPractica, string> = {
  ASIGNADA_PENDIENTE_INICIO: 'bg-yellow-100 text-yellow-800',
  EN_CURSO:                  'bg-green-100 text-green-800',
  FINALIZADA:                'bg-blue-100 text-blue-800',
  CANCELADA:                 'bg-red-100 text-red-800',
}

const ESTADO_HV_BADGE: Record<string, string> = {
  PENDIENTE: 'bg-amber-100 text-amber-800',
  VALIDA:    'bg-green-100 text-green-800',
  RECHAZADA: 'bg-red-100 text-red-800',
}

export default function MiExpedientePage() {
  const { user } = useAuth()
  const [expediente, setExpediente] = useState<ExpedienteResponse | null>(null)
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState('')
  const [tabActiva, setTabActiva]   = useState<'general' | 'hv' | 'practicas'>('general')

  useEffect(() => {
    if (!user?.usuarioId) return
    expedienteService.obtener(user.usuarioId)
      .then(setExpediente)
      .catch(() => setError('No se pudo cargar tu expediente. Contacta al coordinador.'))
      .finally(() => setLoading(false))
  }, [user])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-xl mx-auto mt-10">
        <div className="card border-amber-200 bg-amber-50">
          <div className="text-amber-600 text-3xl mb-2">📋</div>
          <h2 className="font-semibold text-amber-900 mb-1">Expediente no disponible</h2>
          <p className="text-amber-700 text-sm">{error}</p>
        </div>
      </div>
    )
  }

  if (!expediente) return null

  const hvActual = expediente.hvActual

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Mi Expediente</h1>
        <p className="text-sm text-gray-500 mt-1">Historial académico y registro de tus prácticas.</p>
      </div>

      {/* Resumen general */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="card bg-cue-light border-cue-accent/30 text-center">
          <div className="text-3xl font-bold text-cue-primary">{expediente.practicas.length}</div>
          <div className="text-sm text-cue-secondary font-medium mt-1">Prácticas registradas</div>
        </div>
        <div className="card text-center">
          <div className="text-3xl font-bold text-gray-800">{expediente.semestre ?? '—'}</div>
          <div className="text-sm text-gray-500 font-medium mt-1">Semestre actual</div>
        </div>
        <div className="card text-center">
          <span className={`text-xs font-bold px-3 py-1 rounded-full ${
            expediente.estadoEstudiante === 'APTO' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
          }`}>
            {expediente.estadoEstudiante}
          </span>
          <div className="text-sm text-gray-500 font-medium mt-2">Estado académico</div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-1">
          {[
            { id: 'general',   label: 'Información General' },
            { id: 'hv',        label: `Hoja de Vida (${expediente.historialHv.length})` },
            { id: 'practicas', label: `Prácticas (${expediente.practicas.length})` },
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setTabActiva(tab.id as typeof tabActiva)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                tabActiva === tab.id
                  ? 'border-cue-primary text-cue-primary'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab: General */}
      {tabActiva === 'general' && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800 text-lg">Datos Personales</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
            <InfoField label="Nombre completo"   value={expediente.nombreEstudiante} />
            <InfoField label="Identificación"    value={expediente.identificacion ?? '—'} />
            <InfoField label="Programa"          value={expediente.programa ?? '—'} />
            <InfoField label="Semestre"          value={expediente.semestre ? String(expediente.semestre) : '—'} />
            <InfoField label="Estado académico"  value={expediente.estadoEstudiante} />
            <InfoField label="Registro desde"    value={new Date(expediente.creadoEn).toLocaleDateString('es-CO')} />
          </div>

          {/* HV actual */}
          {hvActual ? (
            <div className="border-t pt-4 mt-2">
              <h3 className="font-medium text-gray-700 mb-3">Hoja de Vida Actual</h3>
              <div className="bg-gray-50 rounded-lg p-4 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-2xl">📄</span>
                    <span className="font-medium text-gray-800">Versión {hvActual.version}</span>
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${ESTADO_HV_BADGE[hvActual.estado]}`}>
                      {hvActual.estado}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500">
                    Cargada el {new Date(hvActual.fechaCarga).toLocaleDateString('es-CO')}
                    {hvActual.motivoRechazo && ` · Motivo: ${hvActual.motivoRechazo}`}
                  </p>
                </div>
                {hvActual.urlArchivo && (
                  <a
                    href={hvActual.urlArchivo}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn-secondary text-sm"
                  >
                    Ver archivo
                  </a>
                )}
              </div>
            </div>
          ) : (
            <div className="border-t pt-4 mt-2">
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 text-amber-800 text-sm">
                No tienes una hoja de vida cargada. Contacta al coordinador para cargar tu CV.
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab: Hoja de Vida */}
      {tabActiva === 'hv' && (
        <div className="space-y-3">
          {expediente.historialHv.length === 0 ? (
            <div className="card text-center py-12">
              <div className="text-gray-300 text-4xl mb-3">📄</div>
              <p className="text-gray-400 text-sm">No hay historial de hojas de vida.</p>
            </div>
          ) : expediente.historialHv.map((hv, idx) => (
            <div key={hv.id} className="card flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold ${
                  idx === 0 ? 'bg-cue-light text-cue-primary' : 'bg-gray-100 text-gray-500'
                }`}>
                  v{hv.version}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-800">Versión {hv.version}</span>
                    {idx === 0 && <span className="text-xs bg-cue-light text-cue-primary px-2 py-0.5 rounded-full font-medium">Actual</span>}
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${ESTADO_HV_BADGE[hv.estado]}`}>
                      {hv.estado}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Cargada el {new Date(hv.fechaCarga).toLocaleDateString('es-CO')}
                    {hv.fechaValidacion && ` · Validada el ${new Date(hv.fechaValidacion).toLocaleDateString('es-CO')}`}
                  </p>
                  {hv.motivoRechazo && (
                    <p className="text-xs text-red-600 mt-0.5">Motivo: {hv.motivoRechazo}</p>
                  )}
                </div>
              </div>
              {hv.urlArchivo && (
                <a href={hv.urlArchivo} target="_blank" rel="noopener noreferrer"
                  className="text-sm text-cue-primary font-medium hover:underline">
                  Ver →
                </a>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Tab: Prácticas */}
      {tabActiva === 'practicas' && (
        <div className="space-y-3">
          {expediente.practicas.length === 0 ? (
            <div className="card text-center py-12">
              <div className="text-gray-300 text-4xl mb-3">💼</div>
              <p className="text-gray-400 text-sm">No tienes prácticas registradas aún.</p>
            </div>
          ) : expediente.practicas.map(p => (
            <div key={p.id} className="card">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-gray-900">{p.nombre}</span>
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${ESTADO_PRACTICA_BADGE[p.estado]}`}>
                      {p.estado.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <p className="text-xs text-gray-400 mt-0.5">{p.codigoMateria} · Práctica #{p.numeroPractica}</p>
                </div>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
                <InfoField label="Empresa"   value={p.razonSocialEmpresa ?? '—'} />
                <InfoField label="Docente"   value={p.nombreDocenteAsesor ?? '—'} />
                <InfoField label="Tutor"     value={p.nombreTutorEmpresarial ?? '—'} />
                <InfoField label="Duración"  value={`${p.duracionSemanas} semanas`} />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function InfoField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-gray-500 font-medium">{label}</dt>
      <dd className="text-gray-800 mt-0.5">{value}</dd>
    </div>
  )
}
