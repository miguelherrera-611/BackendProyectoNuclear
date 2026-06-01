import { useState, useEffect } from 'react'
import { EmpresaResponse, EstadoEmpresa } from '../../types'
import { empresaService } from '../../services/empresaService'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Table } from '../../components/common/Table/Table'
import { useToast } from '../../components/common/Notifications/Toast'

const BADGE: Record<EstadoEmpresa, string> = {
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  APROBADA:  'bg-green-100 text-green-800',
  RECHAZADA: 'bg-red-100 text-red-800',
  INACTIVA:  'bg-gray-100 text-gray-600',
}

const FORM_INICIAL = {
  razonSocial: '', nit: '', sector: '', direccion: '',
  municipio: '', telefono: '', nombreContacto: '', correo: '', areas: '',
}
const CLONAR_INICIAL = { razonSocial: '', nit: '' }
const RECHAZAR_INICIAL = { id: 0, motivo: '' }

export default function EmpresasPage() {
  const { showToast } = useToast()
  const [empresas, setEmpresas]     = useState<EmpresaResponse[]>([])
  const [loading, setLoading]       = useState(true)
  const [saving, setSaving]         = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [modalClonar, setModalClonar] = useState<{ open: boolean; id: number }>({ open: false, id: 0 })
  const [modalRechazar, setModalRechazar] = useState(RECHAZAR_INICIAL)
  const [form, setForm]             = useState(FORM_INICIAL)
  const [clonarForm, setClonarForm] = useState(CLONAR_INICIAL)
  const [errorModal, setErrorModal] = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; razon: string; accion: 'aprobar' | 'inactivar' }>({
    open: false, id: 0, razon: '', accion: 'aprobar',
  })

  const cargar = () => {
    setLoading(true)
    empresaService.listar().then(setEmpresas).finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await empresaService.crear({
        ...form,
        areasDisponibles: form.areas.split(',').map(a => a.trim()).filter(Boolean),
      })
      setModalCrear(false)
      setForm(FORM_INICIAL)
      cargar()
      showToast('Empresa creada correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear la empresa.')
    } finally {
      setSaving(false)
    }
  }

  const handleClonar = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await empresaService.clonar(modalClonar.id, clonarForm.razonSocial, clonarForm.nit)
      setModalClonar({ open: false, id: 0 })
      setClonarForm(CLONAR_INICIAL)
      cargar()
      showToast('Empresa clonada correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al clonar.')
    } finally {
      setSaving(false)
    }
  }

  const handleConfirm = async () => {
    try {
      if (confirm.accion === 'aprobar') {
        await empresaService.aprobar(confirm.id)
        showToast('Empresa aprobada.')
      } else {
        await empresaService.inactivar(confirm.id)
        showToast('Empresa inactivada.')
      }
      setConfirm({ open: false, id: 0, razon: '', accion: 'aprobar' })
      cargar()
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al procesar.', 'error')
    }
  }

  const handleRechazar = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      await empresaService.rechazar(modalRechazar.id, modalRechazar.motivo)
      setModalRechazar(RECHAZAR_INICIAL)
      cargar()
      showToast('Empresa rechazada.')
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al rechazar.', 'error')
    } finally {
      setSaving(false)
    }
  }

  const HEADERS = ['Razón Social', 'NIT', 'Sector', 'Municipio', 'Contacto', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Empresas</h1>
        <Button onClick={() => { setErrorModal(''); setModalCrear(true) }}>+ Nueva Empresa</Button>
      </div>

      <Table headers={HEADERS} loading={loading} empty={empresas.length === 0}
        emptyMessage="No hay empresas registradas." emptyIcon="🏢">
        {empresas.map(e => (
          <tr key={e.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 font-medium text-gray-800">{e.razonSocial}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.nit}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.sector ?? '—'}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.municipio ?? '—'}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.nombreContacto}</td>
            <td className="px-4 py-3">
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${BADGE[e.estado]}`}>{e.estado}</span>
            </td>
            <td className="px-4 py-3">
              <div className="flex flex-wrap gap-2">
                {e.estado === 'PENDIENTE' && (
                  <>
                    <button onClick={() => setConfirm({ open: true, id: e.id, razon: e.razonSocial, accion: 'aprobar' })}
                      className="text-xs bg-green-100 text-green-700 px-3 py-1 rounded-lg hover:bg-green-200 transition-colors font-medium">Aprobar</button>
                    <button onClick={() => setModalRechazar({ id: e.id, motivo: '' })}
                      className="text-xs bg-red-100 text-red-700 px-3 py-1 rounded-lg hover:bg-red-200 transition-colors font-medium">Rechazar</button>
                  </>
                )}
                {e.estado === 'APROBADA' && (
                  <>
                    <button onClick={() => { setErrorModal(''); setModalClonar({ open: true, id: e.id }); setClonarForm(CLONAR_INICIAL) }}
                      className="text-xs bg-blue-100 text-blue-700 px-3 py-1 rounded-lg hover:bg-blue-200 transition-colors font-medium">Clonar</button>
                    <button onClick={() => setConfirm({ open: true, id: e.id, razon: e.razonSocial, accion: 'inactivar' })}
                      className="text-xs bg-gray-100 text-gray-600 px-3 py-1 rounded-lg hover:bg-gray-200 transition-colors font-medium">Inactivar</button>
                  </>
                )}
              </div>
            </td>
          </tr>
        ))}
      </Table>

      {modalCrear && (
        <Modal title="Nueva Empresa" size="lg" onClose={() => { setModalCrear(false); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleCrear} className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <Input label="Razón Social" required value={form.razonSocial} onChange={e => setForm({ ...form, razonSocial: e.target.value })} />
              <Input label="NIT" required value={form.nit} onChange={e => setForm({ ...form, nit: e.target.value })} />
              <Input label="Sector" value={form.sector} onChange={e => setForm({ ...form, sector: e.target.value })} />
              <Input label="Municipio" value={form.municipio} onChange={e => setForm({ ...form, municipio: e.target.value })} />
            </div>
            <Input label="Dirección" value={form.direccion} onChange={e => setForm({ ...form, direccion: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Nombre del Contacto" required value={form.nombreContacto} onChange={e => setForm({ ...form, nombreContacto: e.target.value })} />
              <Input label="Correo" type="email" value={form.correo} onChange={e => setForm({ ...form, correo: e.target.value })} />
            </div>
            <Input label="Teléfono" value={form.telefono} onChange={e => setForm({ ...form, telefono: e.target.value })} />
            <Input label="Áreas Disponibles" placeholder="Sistemas, Contabilidad, Mercadeo" hint="Separadas por coma"
              value={form.areas} onChange={e => setForm({ ...form, areas: e.target.value })} />
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => { setModalCrear(false); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Crear</Button>
            </div>
          </form>
        </Modal>
      )}

      {modalRechazar.id > 0 && (
        <Modal title="Rechazar Empresa" onClose={() => setModalRechazar(RECHAZAR_INICIAL)}>
          <form onSubmit={handleRechazar} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Motivo de Rechazo <span className="text-red-500">*</span></label>
              <textarea className="input-field" rows={4} required value={modalRechazar.motivo}
                onChange={e => setModalRechazar({ ...modalRechazar, motivo: e.target.value })} />
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalRechazar(RECHAZAR_INICIAL)}>Cancelar</Button>
              <Button variant="danger" className="flex-1" type="submit" loading={saving}>Rechazar</Button>
            </div>
          </form>
        </Modal>
      )}

      {modalClonar.open && (
        <Modal title="Clonar Empresa" subtitle="Se copiarán sector, dirección, municipio y áreas."
          onClose={() => { setModalClonar({ open: false, id: 0 }); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleClonar} className="space-y-4">
            <Input label="Nueva Razón Social" required value={clonarForm.razonSocial} onChange={e => setClonarForm({ ...clonarForm, razonSocial: e.target.value })} />
            <Input label="Nuevo NIT" required value={clonarForm.nit} onChange={e => setClonarForm({ ...clonarForm, nit: e.target.value })} />
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => { setModalClonar({ open: false, id: 0 }); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Clonar</Button>
            </div>
          </form>
        </Modal>
      )}

      <ConfirmModal open={confirm.open}
        title={confirm.accion === 'aprobar' ? '¿Aprobar empresa?' : '¿Inactivar empresa?'}
        message={confirm.accion === 'aprobar' ? `Se aprobará "${confirm.razon}" y podrá recibir vacantes.` : `Se inactivará "${confirm.razon}". Sus vacantes activas serán cerradas.`}
        confirmLabel={confirm.accion === 'aprobar' ? 'Aprobar' : 'Inactivar'}
        variant={confirm.accion === 'inactivar' ? 'danger' : 'primary'}
        onConfirm={handleConfirm}
        onCancel={() => setConfirm({ open: false, id: 0, razon: '', accion: 'aprobar' })}
      />
    </div>
  )
}
