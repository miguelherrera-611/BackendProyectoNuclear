import { useEffect, useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { expedienteService } from '../../services/expedienteService'
import api from '../../services/api'
import { Modal } from '../../components/common/Modal/Modal'
import { useToast } from '../../components/common/Notifications/Toast'
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

function abrirArchivoHv(hvId: number, urlArchivo: string) {
  if (urlArchivo.startsWith('http://') || urlArchivo.startsWith('https://')) {
    window.open(urlArchivo, '_blank', 'noopener,noreferrer')
    return
  }
  api.get<Blob>(`/api/v1/expedientes/hoja-de-vida/${hvId}/archivo`, { responseType: 'blob' })
    .then(res => {
      const blobUrl = URL.createObjectURL(res.data)
      window.open(blobUrl, '_blank')
      setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000)
    })
    .catch(() => alert('No se pudo abrir el archivo. El archivo puede no estar disponible en el servidor.'))
}

export default function MiExpedientePage() {
  const { user } = useAuth()
  const { showToast } = useToast()
  const [expediente, setExpediente] = useState<ExpedienteResponse | null>(null)
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState('')
  const [tabActiva, setTabActiva]   = useState<'general' | 'hv' | 'practicas'>('general')

  const [modalHv, setModalHv]       = useState(false)
  const [urlHv, setUrlHv]           = useState('')
  const [subiendoHv, setSubiendoHv] = useState(false)
  const [errorHv, setErrorHv]       = useState('')

  const cargar = () => {
    if (!user?.usuarioId) return
    expedienteService.obtener(user.usuarioId)
      .then(setExpediente)
      .catch(() => setError('No se pudo cargar tu expediente. Contacta al coordinador.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [user])

  const abrirModalHv = () => {
    setUrlHv('')
    setErrorHv('')
    setModalHv(true)
  }

  const handleSubirHv = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!urlHv.trim() || !user?.usuarioId) return
    setSubiendoHv(true)
    setErrorHv('')
    try {
      await expedienteService.subirHojaDeVida(user.usuarioId, urlHv.trim())
      setModalHv(false)
      showToast('Hoja de vida enviada correctamente. Queda pendiente de validación.')
      setLoading(true)
      cargar()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setErrorHv(msg ?? 'No se pudo enviar la hoja de vida.')
    } finally {
      setSubiendoHv(false)
    }
  }

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
  const tieneEnCurso = expediente.practicas.some(p => p.estado === 'EN_CURSO')

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Modal subir HV */}
      {modalHv && (
        <Modal
          title="Actualizar Hoja de Vida"
          subtitle="Pega el enlace público de tu CV (Google Drive, OneDrive, Dropbox, etc.)."
          onClose={() => setModalHv(false)}
        >
          <form onSubmit={handleSubirHv} className="space-y-4">
            {tieneEnCurso && (
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-amber-800 text-sm">
                Tienes una práctica EN_CURSO. El sistema no permite reemplazar la hoja de vida durante una práctica activa.
              </div>
            )}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Enlace del archivo <span className="text-red-500">*</span>
              </label>
              <input
                type="url"
                className="input-field"
                placeholder="https://drive.google.com/file/d/..."
                value={urlHv}
                onChange={e => setUrlHv(e.target.value)}
                required
                disabled={tieneEnCurso || subiendoHv}
              />
              <p className="text-xs text-gray-400 mt-1">
                El enlace debe ser público o accesible con el enlace para que el coordinador pueda revisarlo.
              </p>
            </div>

            {errorHv && (
              <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {errorHv}
              </p>
            )}

            <div className="flex gap-3 pt-1">
              <button
                type="button"
                className="btn-secondary flex-1"
                onClick={() => setModalHv(false)}
                disabled={subiendoHv}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="btn-primary flex-1"
                disabled={subiendoHv || tieneEnCurso || !urlHv.trim()}
              >
                {subiendoHv ? 'Enviando…' : 'Enviar'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Mi Expediente</h1>
          <p className="text-sm text-gray-500 mt-1">Historial académico y registro de tus prácticas.</p>
        </div>
        <button
          type="button"
          onClick={abrirModalHv}
          className="btn-primary text-sm"
        >
          Actualizar Hoja de Vida
        </button>
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
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-medium text-gray-700">Hoja de Vida Actual</h3>
                <button
                  type="button"
                  onClick={abrirModalHv}
                  className="text-xs text-cue-primary font-medium hover:underline"
                >
                  Actualizar
                </button>
              </div>
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
                  <button
                    type="button"
                    onClick={() => abrirArchivoHv(hvActual.id, hvActual.urlArchivo)}
                    className="btn-secondary text-sm"
                  >
                    Ver archivo
                  </button>
                )}
              </div>
            </div>
          ) : (
            <div className="border-t pt-4 mt-2">
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 text-amber-800 text-sm flex items-center justify-between gap-4">
                <p>No tienes una hoja de vida registrada.</p>
                <button
                  type="button"
                  onClick={abrirModalHv}
                  className="shrink-0 text-xs font-semibold bg-amber-200 hover:bg-amber-300 text-amber-900 px-3 py-1.5 rounded-lg transition-colors"
                >
                  Subir ahora
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab: Hoja de Vida */}
      {tabActiva === 'hv' && (
        <div className="space-y-3">
          <p className="text-sm text-gray-500">Historial de versiones de tu hoja de vida.</p>

          {expediente.historialHv.length === 0 ? (
            <div className="card text-center py-12">
              <div className="text-gray-300 text-4xl mb-3">📄</div>
              <p className="text-gray-500 text-sm font-medium mb-3">No has subido ninguna hoja de vida aún.</p>
              <button
                type="button"
                onClick={abrirModalHv}
                className="btn-primary text-sm"
              >
                Subir hoja de vida
              </button>
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
                <button
                  type="button"
                  onClick={() => abrirArchivoHv(hv.id, hv.urlArchivo)}
                  className="text-sm text-cue-primary font-medium hover:underline"
                >
                  Ver →
                </button>
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
