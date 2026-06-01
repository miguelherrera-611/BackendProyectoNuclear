import { useState, useEffect } from 'react'
import { FacultadResponse, ApiResponse, Pageable } from '../../types'
import api from '../../services/api'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { useToast } from '../../components/common/Notifications/Toast'

export default function FacultadesPage() {
  const { showToast } = useToast()
  const [facultades, setFacultades]   = useState<FacultadResponse[]>([])
  const [loading, setLoading]         = useState(true)
  const [saving, setSaving]           = useState(false)
  const [modalCrear, setModalCrear]   = useState(false)
  const [form, setForm]               = useState({ nombre: '', descripcion: '' })
  const [errorModal, setErrorModal]   = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; nombre: string }>({
    open: false, id: 0, nombre: '',
  })

  const cargar = () => {
    setLoading(true)
    api.get<ApiResponse<Pageable<FacultadResponse>>>('/facultades')
      .then(r => setFacultades(r.data.datos?.content ?? []))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await api.post('/facultades', form)
      setModalCrear(false)
      setForm({ nombre: '', descripcion: '' })
      cargar()
      showToast('Facultad creada correctamente.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setErrorModal(msg ?? 'Error al crear la facultad.')
    } finally {
      setSaving(false)
    }
  }

  const handleDesactivar = async () => {
    try {
      await api.patch(`/facultades/${confirm.id}/desactivar`)
      setConfirm({ open: false, id: 0, nombre: '' })
      cargar()
      showToast('Facultad desactivada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? 'No se puede desactivar.', 'error')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Facultades</h1>
        <Button onClick={() => { setErrorModal(''); setModalCrear(true) }}>
          + Nueva Facultad
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {loading ? (
          <div className="col-span-3 flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
          </div>
        ) : facultades.length === 0 ? (
          <div className="col-span-3 card text-center py-16">
            <div className="text-gray-300 text-5xl mb-3">🏛️</div>
            <p className="text-gray-400 text-sm">No hay facultades registradas.</p>
            <button className="mt-3 text-cue-primary text-sm font-medium hover:underline"
              onClick={() => setModalCrear(true)}>
              Crear la primera
            </button>
          </div>
        ) : facultades.map(f => (
          <div key={f.id} className="card hover:shadow-md transition-shadow">
            <div className="flex items-start justify-between">
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-gray-800 truncate">{f.nombre}</h3>
                {f.descripcion && <p className="text-xs text-gray-500 mt-1">{f.descripcion}</p>}
              </div>
              <span className={f.activa ? 'badge-apto ml-2 shrink-0' : 'badge-no-apto ml-2 shrink-0'}>
                {f.activa ? 'Activa' : 'Inactiva'}
              </span>
            </div>
            <div className="mt-3 flex items-center justify-between">
              <p className="text-sm text-gray-600">
                <span className="font-semibold text-cue-primary">{f.numeroProgramas}</span> programas
              </p>
              {f.activa && (
                <button
                  onClick={() => setConfirm({ open: true, id: f.id, nombre: f.nombre })}
                  className="text-xs text-red-500 hover:text-red-700 transition-colors"
                >
                  Desactivar
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Modal: Crear */}
      {modalCrear && (
        <Modal title="Nueva Facultad" onClose={() => setModalCrear(false)}>
          {errorModal && (
            <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>
          )}
          <form onSubmit={handleCrear} className="space-y-4">
            <Input
              label="Nombre"
              required
              value={form.nombre}
              onChange={e => setForm({ ...form, nombre: e.target.value })}
            />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
              <textarea
                className="input-field"
                rows={3}
                value={form.descripcion}
                onChange={e => setForm({ ...form, descripcion: e.target.value })}
              />
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalCrear(false)}>
                Cancelar
              </Button>
              <Button className="flex-1" type="submit" loading={saving}>
                Crear
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Confirmación desactivar */}
      <ConfirmModal
        open={confirm.open}
        title="Desactivar facultad"
        message={`¿Desactivar "${confirm.nombre}"? Sus programas asociados quedarán inactivos.`}
        confirmLabel="Desactivar"
        variant="danger"
        onConfirm={handleDesactivar}
        onCancel={() => setConfirm({ open: false, id: 0, nombre: '' })}
      />
    </div>
  )
}
