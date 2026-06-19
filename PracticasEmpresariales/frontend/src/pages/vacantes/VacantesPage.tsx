import { useState, useEffect, useCallback, useMemo } from 'react'
import { VacanteResponse, EmpresaResponse, EstadoVacante } from '../../types'
import { vacanteService } from '../../services/vacanteService'
import { empresaService } from '../../services/empresaService'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Select } from '../../components/common/Select/Select'
import { Table } from '../../components/common/Table/Table'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'

type Tab = 'todas' | 'activas' | 'inactivas'

const BADGE: Record<EstadoVacante, string> = {
  DISPONIBLE: 'bg-green-100 text-green-800',
  PENDIENTE:  'bg-yellow-100 text-yellow-800',
  RECHAZADA:  'bg-gray-100 text-gray-600',
  CERRADA:    'bg-gray-100 text-gray-600',
}

const LABEL: Record<EstadoVacante, string> = {
  DISPONIBLE: 'Activa',
  PENDIENTE:  'Sin activar',
  RECHAZADA:  'Inactiva',
  CERRADA:    'Inactiva',
}

const esActiva = (v: VacanteResponse) => v.estado === 'DISPONIBLE'
const esInactiva = (v: VacanteResponse) => v.estado === 'CERRADA' || v.estado === 'RECHAZADA' || v.estado === 'PENDIENTE'

const FORM_INICIAL = { empresaId: 0, area: '', cuposTotales: 1 }

export default function VacantesPage() {
  const { showToast } = useToast()
  const [vacantes, setVacantes]     = useState<VacanteResponse[]>([])
  const [empresas, setEmpresas]     = useState<EmpresaResponse[]>([])
  const [busqueda, setBusqueda]    = useState('')
  const [tab, setTab]               = useState<Tab>('todas')
  const [loading, setLoading]       = useState(true)
  const [saving, setSaving]         = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [form, setForm]             = useState(FORM_INICIAL)
  const [errorModal, setErrorModal] = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; accion: 'activar' | 'desactivar' }>({
    open: false, id: 0, accion: 'activar',
  })

  const cargar = useCallback(() => {
    setLoading(true)
    vacanteService.listar()
      .then(setVacantes)
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => { empresaService.listarActivas().then(setEmpresas) }, [])

  const vacantesTab = vacantes.filter(v =>
    tab === 'activas' ? esActiva(v) : tab === 'inactivas' ? esInactiva(v) : true
  )

  const vacantesFiltradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return vacantesTab.filter(v => {
      const coincideTexto = !texto || [v.razonSocialEmpresa, v.area].some(valor => valor?.toLowerCase().includes(texto))
      return coincideTexto
    })
  }, [busqueda, vacantesTab])

  const limpiarFiltros = () => setBusqueda('')

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await vacanteService.crear(form)
      setModalCrear(false)
      setForm(FORM_INICIAL)
      cargar()
      showToast('Vacante creada y activada correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear la vacante.')
    } finally {
      setSaving(false)
    }
  }

  const handleConfirm = async () => {
    try {
      if (confirm.accion === 'activar') {
        await vacanteService.activar(confirm.id)
        showToast('Vacante activada.')
      } else {
        await vacanteService.desactivar(confirm.id)
        showToast('Vacante desactivada.')
      }
      setConfirm({ open: false, id: 0, accion: 'activar' })
      cargar()
    } catch (err: unknown) {
      showToast(
        (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al cambiar el estado.',
        'error'
      )
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
        <button className={tabClass('todas')}    onClick={() => setTab('todas')}>Todas</button>
        <button className={tabClass('activas')}  onClick={() => setTab('activas')}>Activas</button>
        <button className={tabClass('inactivas')} onClick={() => setTab('inactivas')}>Inactivas</button>
      </div>

      <ListFilters
        search={{
          label: 'Buscar vacante',
          placeholder: 'Empresa o área...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${vacantesFiltradas.length} de ${vacantesTab.length}`}
        onClear={limpiarFiltros}
      />

      <Table headers={HEADERS} loading={loading} empty={vacantesFiltradas.length === 0}
        emptyMessage={vacantesTab.length === 0 ? 'No hay vacantes en esta categoría.' : 'No hay vacantes que coincidan con los filtros.'} emptyIcon="💼">
        {vacantesFiltradas.map(v => (
          <tr key={v.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 font-medium text-gray-800">{v.razonSocialEmpresa}</td>
            <td className="px-4 py-3 text-gray-600">{v.area}</td>
            <td className="px-4 py-3 text-gray-600 text-center">{v.cuposOcupados} / {v.cuposTotales}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{v.fechaPublicacion}</td>
            <td className="px-4 py-3">
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${BADGE[v.estado]}`}>
                {LABEL[v.estado]}
              </span>
            </td>
            <td className="px-4 py-3">
              {esActiva(v) ? (
                <button
                  onClick={() => setConfirm({ open: true, id: v.id, accion: 'desactivar' })}
                  className="text-xs bg-gray-100 text-gray-700 px-3 py-1 rounded-lg hover:bg-gray-200 transition-colors font-medium"
                >
                  Desactivar
                </button>
              ) : (
                <button
                  onClick={() => setConfirm({ open: true, id: v.id, accion: 'activar' })}
                  className="text-xs bg-green-100 text-green-700 px-3 py-1 rounded-lg hover:bg-green-200 transition-colors font-medium"
                  disabled={v.cuposOcupados >= v.cuposTotales}
                  title={v.cuposOcupados >= v.cuposTotales ? 'Todos los cupos están ocupados' : 'Activar vacante'}
                >
                  Activar
                </button>
              )}
            </td>
          </tr>
        ))}
      </Table>

      {modalCrear && (
        <Modal title="Nueva Vacante" onClose={() => { setModalCrear(false); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleCrear} className="space-y-4">
            <Select
              label="Empresa"
              required
              value={form.empresaId || ''}
              onChange={e => setForm({ ...form, empresaId: Number(e.target.value) })}
            >
              <option value="">Selecciona una empresa activa</option>
              {empresas.map(em => <option key={em.id} value={em.id}>{em.razonSocial}</option>)}
            </Select>
            <Input
              label="Área"
              required
              placeholder="Ej. Sistemas, Contabilidad"
              value={form.area}
              onChange={e => setForm({ ...form, area: e.target.value })}
            />
            <Input
              label="Cupos Totales"
              type="number"
              min={1}
              required
              value={form.cuposTotales}
              onChange={e => setForm({ ...form, cuposTotales: Number(e.target.value) })}
            />
            <p className="text-xs text-gray-400">La vacante quedará activa inmediatamente.</p>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => { setModalCrear(false); setErrorModal('') }}>
                Cancelar
              </Button>
              <Button className="flex-1" type="submit" loading={saving}>Crear</Button>
            </div>
          </form>
        </Modal>
      )}

      <ConfirmModal
        open={confirm.open}
        title={confirm.accion === 'activar' ? '¿Activar vacante?' : '¿Desactivar vacante?'}
        message={
          confirm.accion === 'activar'
            ? 'La vacante quedará disponible para nuevas asignaciones.'
            : 'La vacante no podrá recibir nuevas asignaciones mientras esté inactiva.'
        }
        confirmLabel={confirm.accion === 'activar' ? 'Activar' : 'Desactivar'}
        variant={confirm.accion === 'desactivar' ? 'danger' : 'primary'}
        onConfirm={handleConfirm}
        onCancel={() => setConfirm({ open: false, id: 0, accion: 'activar' })}
      />
    </div>
  )
}
