import { useState, useEffect, useMemo } from 'react'
import { EmpresaResponse, EstadoEmpresa, Pageable } from '../../types'
import { empresaService } from '../../services/empresaService'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Select } from '../../components/common/Select/Select'
import { Table } from '../../components/common/Table/Table'
import { Pagination } from '../../components/common/Table/Pagination'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'

const BADGE: Record<EstadoEmpresa, string> = {
  ACTIVA:   'bg-green-100 text-green-800',
  INACTIVA: 'bg-gray-100 text-gray-600',
}

const FORM_INICIAL = {
  razonSocial: '', nit: '', sector: '', direccion: '',
  municipio: '', telefono: '', nombreContacto: '', correo: '', areas: '',
}

export default function EmpresasPage() {
  const { showToast } = useToast()
  const [empresas, setEmpresas]     = useState<EmpresaResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<EmpresaResponse> | null>(null)
  const [busqueda, setBusqueda] = useState('')
  const [estadoFiltro, setEstadoFiltro] = useState<'todas' | EstadoEmpresa>('todas')
  const [loading, setLoading]       = useState(true)
  const [saving, setSaving]         = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [form, setForm]             = useState(FORM_INICIAL)
  const [errorModal, setErrorModal] = useState('')
  const [confirm, setConfirm] = useState<{
    open: boolean; id: number; razon: string; accion: 'activar' | 'inactivar'
  }>({ open: false, id: 0, razon: '', accion: 'activar' })

  const cargar = () => {
    setLoading(true)
    empresaService.listarPaginado(pagina)
      .then(p => {
        setEmpresas(p.content)
        setPageData(p)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [pagina])

  const empresasFiltradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return empresas.filter(e => {
      const coincideTexto = !texto || [e.razonSocial, e.nit, e.sector, e.municipio, e.nombreContacto].some(valor => valor?.toLowerCase().includes(texto))
      const coincideEstado = estadoFiltro === 'todas' || e.estado === estadoFiltro
      return coincideTexto && coincideEstado
    })
  }, [busqueda, empresas, estadoFiltro])

  const limpiarFiltros = () => {
    setBusqueda('')
    setEstadoFiltro('todas')
  }

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
      showToast('Empresa creada correctamente. Estado inicial: Inactiva.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear la empresa.')
    } finally {
      setSaving(false)
    }
  }

  const handleConfirm = async () => {
    try {
      if (confirm.accion === 'activar') {
        await empresaService.activar(confirm.id)
        showToast(`"${confirm.razon}" activada correctamente.`)
      } else {
        await empresaService.inactivar(confirm.id)
        showToast(`"${confirm.razon}" inactivada. Sus tutores han sido inactivados.`)
      }
      setConfirm({ open: false, id: 0, razon: '', accion: 'activar' })
      cargar()
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al procesar.', 'error')
    }
  }

  const HEADERS = ['Razón Social', 'NIT', 'Sector', 'Municipio', 'Contacto', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Empresas</h1>
          <p className="text-sm text-gray-500 mt-0.5">Las empresas nuevas quedan en estado Inactiva hasta que las actives.</p>
        </div>
        <Button onClick={() => { setErrorModal(''); setModalCrear(true) }}>+ Nueva Empresa</Button>
      </div>

      <ListFilters
        search={{
          label: 'Buscar empresa',
          placeholder: 'Razón social, NIT, sector, municipio...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${empresasFiltradas.length} de ${empresas.length}`}
        onClear={limpiarFiltros}
      >
        <div className="w-full sm:w-56">
          <Select label="Estado" value={estadoFiltro} onChange={e => setEstadoFiltro(e.target.value as typeof estadoFiltro)}>
            <option value="todas">Todas</option>
            <option value="ACTIVA">Activas</option>
            <option value="INACTIVA">Inactivas</option>
          </Select>
        </div>
      </ListFilters>

      <Table headers={HEADERS} loading={loading} empty={empresasFiltradas.length === 0}
        emptyMessage={empresas.length === 0 ? 'No hay empresas registradas.' : 'No hay empresas que coincidan con los filtros.'} emptyIcon="🏢">
        {empresasFiltradas.map(e => (
          <tr key={e.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 font-medium text-gray-800">{e.razonSocial}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.nit}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.sector ?? '—'}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.municipio ?? '—'}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{e.nombreContacto}</td>
            <td className="px-4 py-3">
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${BADGE[e.estado]}`}>
                {e.estado === 'ACTIVA' ? 'Activa' : 'Inactiva'}
              </span>
            </td>
            <td className="px-4 py-3">
              {e.estado === 'INACTIVA' ? (
                <button
                  onClick={() => setConfirm({ open: true, id: e.id, razon: e.razonSocial, accion: 'activar' })}
                  className="text-xs bg-green-100 text-green-700 px-3 py-1 rounded-lg hover:bg-green-200 transition-colors font-medium"
                >
                  Activar
                </button>
              ) : (
                <button
                  onClick={() => setConfirm({ open: true, id: e.id, razon: e.razonSocial, accion: 'inactivar' })}
                  className="text-xs bg-gray-100 text-gray-600 px-3 py-1 rounded-lg hover:bg-gray-200 transition-colors font-medium"
                >
                  Inactivar
                </button>
              )}
            </td>
          </tr>
        ))}
      </Table>

      <Pagination
        page={pagina}
        totalPages={pageData?.totalPages ?? 0}
        totalElements={pageData?.totalElements}
        onPageChange={setPagina}
        disabled={loading}
      />

      {modalCrear && (
        <Modal title="Nueva Empresa" size="lg" onClose={() => { setModalCrear(false); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <p className="text-sm text-gray-500 mb-4">La empresa se creará en estado <strong>Inactiva</strong>. Podrás activarla desde el listado.</p>
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

      <ConfirmModal
        open={confirm.open}
        title={confirm.accion === 'activar' ? '¿Activar empresa?' : '¿Inactivar empresa?'}
        message={
          confirm.accion === 'activar'
            ? `"${confirm.razon}" quedará activa y podrá tener vacantes y tutores.`
            : `"${confirm.razon}" se inactivará. Sus vacantes activas deben estar cerradas y sus tutores quedarán inactivos.`
        }
        confirmLabel={confirm.accion === 'activar' ? 'Activar' : 'Inactivar'}
        variant={confirm.accion === 'inactivar' ? 'danger' : 'primary'}
        onConfirm={handleConfirm}
        onCancel={() => setConfirm({ open: false, id: 0, razon: '', accion: 'activar' })}
      />
    </div>
  )
}
