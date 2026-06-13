import { useState, useEffect } from 'react'
import { FacultadResponse, ProgramaResponse, ApiResponse, Pageable } from '../../types'
import api from '../../services/api'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Table } from '../../components/common/Table/Table'
import { useToast } from '../../components/common/Notifications/Toast'

export default function ProgramasPage() {
  const { showToast } = useToast()
  const [programas, setProgramas]   = useState<ProgramaResponse[]>([])
  const [facultades, setFacultades] = useState<FacultadResponse[]>([])
  const [loading, setLoading]       = useState(true)
  const [saving, setSaving]         = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [form, setForm] = useState({
    nombre: '', descripcion: '', facultadId: '',
    numeroTotalPracticas: 1, promedioMinimoGeneral: 3.0,
  })
  const [errorModal, setErrorModal] = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; nombre: string; accion: 'activar' | 'desactivar' }>({
    open: false, id: 0, nombre: '', accion: 'desactivar',
  })

  const cargar = () => {
    setLoading(true)
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas?incluirInactivos=true')
      .then(r => setProgramas(r.data.datos?.content ?? []))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    cargar()
    api.get<ApiResponse<Pageable<FacultadResponse>>>('/facultades?size=100')
      .then(r => setFacultades(r.data.datos?.content ?? []))
  }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await api.post('/programas', { ...form, facultadId: Number(form.facultadId) })
      setModalCrear(false)
      setForm({ nombre: '', descripcion: '', facultadId: '', numeroTotalPracticas: 1, promedioMinimoGeneral: 3.0 })
      cargar()
      showToast('Programa creado correctamente.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setErrorModal(msg ?? 'Error al crear el programa.')
    } finally {
      setSaving(false)
    }
  }

  const handleConfirmar = async () => {
    try {
      await api.patch(`/programas/${confirm.id}/${confirm.accion}`)
      setConfirm({ open: false, id: 0, nombre: '', accion: 'desactivar' })
      cargar()
      showToast(confirm.accion === 'activar' ? 'Programa activado.' : 'Programa desactivado.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? `No se puede ${confirm.accion} el programa.`, 'error')
    }
  }

  const HEADERS = ['Programa', 'Facultad', 'N° Prácticas', 'Promedio Mín.', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Programas Académicos</h1>
        <Button onClick={() => { setErrorModal(''); setModalCrear(true) }}>
          + Nuevo Programa
        </Button>
      </div>

      <Table
        headers={HEADERS}
        loading={loading}
        empty={programas.length === 0}
        emptyMessage="No hay programas registrados."
        emptyIcon="📚"
      >
        {programas.map(p => (
          <tr key={p.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3">
              <div className="font-medium text-gray-900">{p.nombre}</div>
              {p.descripcion && <div className="text-xs text-gray-400 mt-0.5">{p.descripcion}</div>}
            </td>
            <td className="px-4 py-3 text-gray-600">{p.facultadNombre}</td>
            <td className="px-4 py-3 text-center text-gray-700">{p.numeroTotalPracticas}</td>
            <td className="px-4 py-3 text-center text-gray-700">{p.promedioMinimoGeneral.toFixed(1)}</td>
            <td className="px-4 py-3">
              <span className={p.activo ? 'badge-apto' : 'badge-no-apto'}>
                {p.activo ? 'Activo' : 'Inactivo'}
              </span>
            </td>
            <td className="px-4 py-3 text-center">
              {p.activo ? (
                <button
                  onClick={() => setConfirm({ open: true, id: p.id, nombre: p.nombre, accion: 'desactivar' })}
                  className="text-xs text-red-500 hover:text-red-700 transition-colors"
                >
                  Desactivar
                </button>
              ) : (
                <button
                  onClick={() => setConfirm({ open: true, id: p.id, nombre: p.nombre, accion: 'activar' })}
                  className="text-xs text-green-600 hover:text-green-800 transition-colors"
                >
                  Activar
                </button>
              )}
            </td>
          </tr>
        ))}
      </Table>

      <ConfirmModal
        open={confirm.open}
        title={confirm.accion === 'activar' ? 'Activar programa' : 'Desactivar programa'}
        message={confirm.accion === 'activar'
          ? `¿Activar "${confirm.nombre}"?`
          : `¿Desactivar "${confirm.nombre}"? Los estudiantes activos en este programa podrían verse afectados.`
        }
        confirmLabel={confirm.accion === 'activar' ? 'Activar' : 'Desactivar'}
        variant={confirm.accion === 'activar' ? 'primary' : 'danger'}
        onConfirm={handleConfirmar}
        onCancel={() => setConfirm({ open: false, id: 0, nombre: '', accion: 'desactivar' })}
      />

      {/* Modal: Crear */}
      {modalCrear && (
        <Modal title="Nuevo Programa" size="lg" onClose={() => setModalCrear(false)}>
          {errorModal && (
            <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>
          )}
          <form onSubmit={handleCrear} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <Input label="Nombre" required value={form.nombre}
                  onChange={e => setForm({ ...form, nombre: e.target.value })} />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
                <textarea className="input-field" rows={2} value={form.descripcion}
                  onChange={e => setForm({ ...form, descripcion: e.target.value })} />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Facultad <span className="text-red-500">*</span>
                </label>
                <select className="input-field" required value={form.facultadId}
                  onChange={e => setForm({ ...form, facultadId: e.target.value })}>
                  <option value="">— Selecciona una facultad —</option>
                  {facultades.map(f => <option key={f.id} value={f.id}>{f.nombre}</option>)}
                </select>
              </div>
              <Input
                label="N° de Prácticas"
                type="number"
                min={1}
                required
                value={form.numeroTotalPracticas}
                onChange={e => setForm({ ...form, numeroTotalPracticas: Number(e.target.value) })}
              />
              <Input
                label="Promedio Mínimo"
                type="number"
                step="0.1"
                min={0}
                max={5}
                value={form.promedioMinimoGeneral}
                onChange={e => setForm({ ...form, promedioMinimoGeneral: Number(e.target.value) })}
              />
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalCrear(false)}>
                Cancelar
              </Button>
              <Button className="flex-1" type="submit" loading={saving}>
                Crear Programa
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
