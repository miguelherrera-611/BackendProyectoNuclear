import { useState, useEffect, useCallback } from 'react'
import { VacanteResponse, EmpresaResponse, EstadoVacante } from '../../types'
import { vacanteService } from '../../services/vacanteService'
import { empresaService } from '../../services/empresaService'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Table } from '../../components/common/Table/Table'
import { useToast } from '../../components/common/Notifications/Toast'

type Tab = 'todas' | 'pendientes' | 'disponibles'

const BADGE: Record<EstadoVacante, string> = {
  PENDIENTE:  'bg-yellow-100 text-yellow-800',
  DISPONIBLE: 'bg-green-100 text-green-800',
  RECHAZADA:  'bg-red-100 text-red-800',
  CERRADA:    'bg-gray-100 text-gray-600',
}

const FORM_INICIAL = { empresaId: 0, area: '', cuposTotales: 1 }
const RECHAZAR_INICIAL = { id: 0, motivo: '' }

export default function VacantesPage() {
  const { showToast } = useToast()
  const [vacantes, setVacantes]     = useState<VacanteResponse[]>([])
  const [empresas, setEmpresas]     = useState<EmpresaResponse[]>([])
  const [tab, setTab]               = useState<Tab>('todas')
  const [loading, setLoading]       = useState(true)
  const [saving, setSaving]         = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [modalRechazar, setModalRechazar] = useState(RECHAZAR_INICIAL)
  const [form, setForm]             = useState(FORM_INICIAL)
  const [errorModal, setErrorModal] = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; accion: 'aprobar' | 'cerrar' }>({
    open: false, id: 0, accion: 'aprobar',
  })

  const cargar = useCallback(() => {
    setLoading(true)
    const fetch = tab === 'pendientes' ? vacanteService.listarPendientes()
      : tab === 'disponibles' ? vacanteService.listarDisponibles()
      : vacanteService.listar()
    fetch.then(setVacantes).finally(() => setLoading(false))
  }, [tab])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => { empresaService.listarActivas().then(setEmpresas) }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await vacanteService.crear(form)
      setModalCrear(false)
      setForm(FORM_INICIAL)
      cargar()
      showToast('Vacante creada correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear la vacante.')
    } finally {
      setSaving(false)
    }
  }

  const handleConfirm = async () => {
    try {
      if (confirm.accion === 'aprobar') { await vacanteService.aprobar(confirm.id); showToast('Vacante aprobada.') }
      else { await vacanteService.cerrar(confirm.id); showToast('Vacante cerrada.') }
      setConfirm({ open: false, id: 0, accion: 'aprobar' })
      cargar()
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error.', 'error')
    }
  }

  const handleRechazar = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      await vacanteService.rechazar(modalRechazar.id, modalRechazar.motivo)
      setModalRechazar(RECHAZAR_INICIAL)
      cargar()
      showToast('Vacante rechazada.')
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error.', 'error')
    } finally {
      setSaving(false)
    }
  }

  const tabClass = (t: Tab) =>
    `px-4 py-2 text-sm font-medium rounded-lg transition-colors ${tab === t ? 'bg-cue-primary text-white' : 'text-gray-600 hover:bg-gray-100'}`

  const HEADERS = ['Empresa', 'Área', 'Cupos', 'Publicación', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Vacantes</h1>
        <Button onClick={() => { setErrorModal(''); setModalCrear(true) }}>+ Nueva Vacante</Button>
      </div>

      <div className="flex gap-2">
        <button className={tabClass('todas')} onClick={() => setTab('todas')}>Todas</button>
        <button className={tabClass('pendientes')} onClick={() => setTab('pendientes')}>Pendientes</button>
        <button className={tabClass('disponibles')} onClick={() => setTab('disponibles')}>Disponibles</button>
      </div>

      <Table headers={HEADERS} loading={loading} empty={vacantes.length === 0}
        emptyMessage="No hay vacantes en esta categoría." emptyIcon="💼">
        {vacantes.map(v => (
          <tr key={v.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 font-medium text-gray-800">{v.razonSocialEmpresa}</td>
            <td className="px-4 py-3 text-gray-600">{v.area}</td>
            <td className="px-4 py-3 text-gray-600 text-center">{v.cuposOcupados} / {v.cuposTotales}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{v.fechaPublicacion}</td>
            <td className="px-4 py-3">
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${BADGE[v.estado]}`}>{v.estado}</span>
            </td>
            <td className="px-4 py-3">
              <div className="flex gap-2">
                {v.estado === 'PENDIENTE' && (
                  <>
                    <button onClick={() => setConfirm({ open: true, id: v.id, accion: 'aprobar' })}
                      className="text-xs bg-green-100 text-green-700 px-3 py-1 rounded-lg hover:bg-green-200 transition-colors font-medium">Aprobar</button>
                    <button onClick={() => setModalRechazar({ id: v.id, motivo: '' })}
                      className="text-xs bg-red-100 text-red-700 px-3 py-1 rounded-lg hover:bg-red-200 transition-colors font-medium">Rechazar</button>
                  </>
                )}
                {v.estado === 'DISPONIBLE' && (
                  <button onClick={() => setConfirm({ open: true, id: v.id, accion: 'cerrar' })}
                    className="text-xs bg-gray-100 text-gray-600 px-3 py-1 rounded-lg hover:bg-gray-200 transition-colors font-medium">Cerrar</button>
                )}
              </div>
            </td>
          </tr>
        ))}
      </Table>

      {modalCrear && (
        <Modal title="Nueva Vacante" onClose={() => { setModalCrear(false); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleCrear} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Empresa <span className="text-red-500">*</span></label>
              <select className="input-field" required value={form.empresaId || ''} onChange={e => setForm({ ...form, empresaId: Number(e.target.value) })}>
                <option value="">Selecciona una empresa aprobada</option>
                {empresas.map(em => <option key={em.id} value={em.id}>{em.razonSocial}</option>)}
              </select>
            </div>
            <Input label="Área" required placeholder="Ej. Sistemas, Contabilidad" value={form.area} onChange={e => setForm({ ...form, area: e.target.value })} />
            <Input label="Cupos Totales" type="number" min={1} required value={form.cuposTotales} onChange={e => setForm({ ...form, cuposTotales: Number(e.target.value) })} />
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => { setModalCrear(false); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Crear</Button>
            </div>
          </form>
        </Modal>
      )}

      {modalRechazar.id > 0 && (
        <Modal title="Rechazar Vacante" onClose={() => setModalRechazar(RECHAZAR_INICIAL)}>
          <form onSubmit={handleRechazar} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Motivo <span className="text-red-500">*</span></label>
              <textarea className="input-field" rows={4} required value={modalRechazar.motivo} onChange={e => setModalRechazar({ ...modalRechazar, motivo: e.target.value })} />
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalRechazar(RECHAZAR_INICIAL)}>Cancelar</Button>
              <Button variant="danger" className="flex-1" type="submit" loading={saving}>Rechazar</Button>
            </div>
          </form>
        </Modal>
      )}

      <ConfirmModal open={confirm.open}
        title={confirm.accion === 'aprobar' ? '¿Aprobar vacante?' : '¿Cerrar vacante?'}
        message={confirm.accion === 'aprobar' ? 'La vacante quedará disponible para asignaciones.' : 'La vacante se cerrará y no recibirá más asignaciones.'}
        confirmLabel={confirm.accion === 'aprobar' ? 'Aprobar' : 'Cerrar'}
        variant={confirm.accion === 'cerrar' ? 'danger' : 'primary'}
        onConfirm={handleConfirm}
        onCancel={() => setConfirm({ open: false, id: 0, accion: 'aprobar' })}
      />
    </div>
  )
}
