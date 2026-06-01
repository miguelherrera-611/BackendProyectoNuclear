import { useEffect, useState } from 'react'
import api from '../../services/api'
import type { ApiResponse } from '../../types'
import { ConfirmModal } from '../../components/common/Modal/Modal'
import { useToast } from '../../components/common/Notifications/Toast'

interface DocumentoResponse {
  id: number
  nombre: string
  tipo: string
  urlArchivo?: string
  creadoEn: string
}

const TIPO_ICONO = (tipo: string) => {
  if (tipo?.includes('pdf'))  return '📄'
  if (tipo?.includes('image')) return '🖼️'
  if (tipo?.includes('word') || tipo?.includes('docx')) return '📝'
  return '📎'
}

export default function MisDocumentosPage() {
  const { showToast } = useToast()
  const [documentos, setDocumentos] = useState<DocumentoResponse[]>([])
  const [loading, setLoading]       = useState(true)
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; nombre: string }>({
    open: false, id: 0, nombre: '',
  })

  const cargar = () => {
    setLoading(true)
    api.get<ApiResponse<DocumentoResponse[]>>('/api/v1/documentos')
      .then(r => setDocumentos(r.data.datos ?? []))
      .catch(() => showToast('No se pudieron cargar los documentos.', 'error'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const handleEliminar = async () => {
    try {
      await api.delete(`/api/v1/documentos/${confirm.id}`)
      setConfirm({ open: false, id: 0, nombre: '' })
      showToast('Documento eliminado.')
      cargar()
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al eliminar.', 'error')
    }
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Mis Documentos</h1>
        <p className="text-sm text-gray-500 mt-1">Documentos asociados a tu práctica empresarial.</p>
      </div>

      {/* Info */}
      <div className="bg-cue-light border border-cue-accent/30 rounded-xl p-4 flex items-start gap-3">
        <span className="text-cue-primary text-xl mt-0.5">ℹ️</span>
        <div className="text-sm text-cue-secondary">
          <p className="font-medium mb-0.5">Gestión de documentos</p>
          <p>Para cargar nuevos documentos, contacta al coordinador de prácticas.</p>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
        </div>
      ) : documentos.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">📁</div>
          <h3 className="font-medium text-gray-600 mb-1">Sin documentos</h3>
          <p className="text-gray-400 text-sm">No tienes documentos cargados aún.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {documentos.map(doc => (
            <div key={doc.id} className="card flex items-center justify-between hover:shadow-md transition-shadow">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-cue-light rounded-xl flex items-center justify-center text-2xl shrink-0">
                  {TIPO_ICONO(doc.tipo)}
                </div>
                <div>
                  <p className="font-medium text-gray-900">{doc.nombre}</p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    {doc.tipo} · {new Date(doc.creadoEn).toLocaleDateString('es-CO')}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                {doc.urlArchivo && (
                  <a href={doc.urlArchivo} target="_blank" rel="noopener noreferrer"
                    className="btn-secondary text-sm">Ver</a>
                )}
                <button
                  onClick={() => setConfirm({ open: true, id: doc.id, nombre: doc.nombre })}
                  className="text-sm text-red-500 hover:text-red-700 px-3 py-2 rounded-lg hover:bg-red-50 transition-colors"
                >
                  Eliminar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmModal
        open={confirm.open}
        title="¿Eliminar documento?"
        message={`Se eliminará permanentemente "${confirm.nombre}".`}
        confirmLabel="Eliminar"
        variant="danger"
        onConfirm={handleEliminar}
        onCancel={() => setConfirm({ open: false, id: 0, nombre: '' })}
      />
    </div>
  )
}
