import { useState, useEffect, useMemo } from 'react'
import { FacultadResponse, ProgramaResponse, ApiResponse, Pageable } from '../../types'
import api from '../../services/api'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Select } from '../../components/common/Select/Select'
import { Table } from '../../components/common/Table/Table'
import { Pagination } from '../../components/common/Table/Pagination'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'

export default function ProgramasPage() {
  const { showToast } = useToast()
  const [programas, setProgramas]   = useState<ProgramaResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<ProgramaResponse> | null>(null)
  const [facultades, setFacultades] = useState<FacultadResponse[]>([])
  const [busqueda, setBusqueda] = useState('')
  const [estadoFiltro, setEstadoFiltro] = useState<'todos' | 'activos' | 'inactivos'>('todos')
  const [loading, setLoading]       = useState(true)
  const [saving, setSaving]         = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [form, setForm] = useState({
    nombre: '', descripcion: '', facultadId: '',
    numeroTotalPracticas: 1, promedioMinimoGeneral: 3.0,
  })
  const [modalEditar, setModalEditar] = useState<ProgramaResponse | null>(null)
  const [formEditar, setFormEditar] = useState({
    nombre: '', descripcion: '', numeroTotalPracticas: 1, promedioMinimoGeneral: 3.0,
  })
  const [errorModal, setErrorModal] = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; nombre: string; accion: 'activar' | 'desactivar' }>({
    open: false, id: 0, nombre: '', accion: 'desactivar',
  })

  const cargar = () => {
    setLoading(true)
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas', { params: { incluirInactivos: true, page: pagina, size: 20 } })
      .then(r => {
        setProgramas(r.data.datos?.content ?? [])
        setPageData(r.data.datos ?? null)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    cargar()
    api.get<ApiResponse<Pageable<FacultadResponse>>>('/facultades?size=100')
      .then(r => setFacultades(r.data.datos?.content ?? []))
  }, [pagina])

  const programasFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return programas.filter(p => {
      const coincideTexto = !texto || [p.nombre, p.descripcion, p.facultadNombre].some(valor => valor?.toLowerCase().includes(texto))
      const coincideEstado = estadoFiltro === 'todos'
        ? true
        : estadoFiltro === 'activos'
          ? p.activo
          : !p.activo
      return coincideTexto && coincideEstado
    })
  }, [busqueda, estadoFiltro, programas])

  const limpiarFiltros = () => {
    setBusqueda('')
    setEstadoFiltro('todos')
  }

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

  const abrirEditar = (p: ProgramaResponse) => {
    setFormEditar({
      nombre: p.nombre,
      descripcion: p.descripcion ?? '',
      numeroTotalPracticas: p.numeroTotalPracticas,
      promedioMinimoGeneral: p.promedioMinimoGeneral,
    })
    setErrorModal('')
    setModalEditar(p)
  }

  const handleEditar = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!modalEditar) return
    setErrorModal('')
    setSaving(true)
    try {
      await api.put(`/programas/${modalEditar.id}`, formEditar)
      setModalEditar(null)
      cargar()
      showToast('Programa actualizado correctamente.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setErrorModal(msg ?? 'Error al editar el programa.')
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

      <ListFilters
        search={{
          label: 'Buscar programa',
          placeholder: 'Nombre, descripción o facultad...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${programasFiltrados.length} de ${programas.length}`}
        onClear={limpiarFiltros}
      >
        <div className="w-full sm:w-56">
          <Select label="Estado" value={estadoFiltro} onChange={e => setEstadoFiltro(e.target.value as typeof estadoFiltro)}>
            <option value="todos">Todos</option>
            <option value="activos">Activos</option>
            <option value="inactivos">Inactivos</option>
          </Select>
        </div>
      </ListFilters>

      <Table
        headers={HEADERS}
        loading={loading}
        empty={programasFiltrados.length === 0}
        emptyMessage={programas.length === 0 ? 'No hay programas registrados.' : 'No hay programas que coincidan con los filtros.'}
        emptyIcon="📚"
      >
        {programasFiltrados.map(p => (
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
              <div className="flex items-center justify-center gap-3">
                <button
                  onClick={() => abrirEditar(p)}
                  className="text-xs text-blue-600 hover:text-blue-800 transition-colors"
                >
                  Editar
                </button>
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
              </div>
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

      {/* Modal: Editar */}
      {modalEditar && (
        <Modal title="Editar Programa" size="lg" onClose={() => setModalEditar(null)}>
          {errorModal && (
            <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>
          )}
          <form onSubmit={handleEditar} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <Input label="Nombre" required value={formEditar.nombre}
                  onChange={e => setFormEditar({ ...formEditar, nombre: e.target.value })} />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
                <textarea className="input-field" rows={2} value={formEditar.descripcion}
                  onChange={e => setFormEditar({ ...formEditar, descripcion: e.target.value })} />
              </div>
              <Input
                label="N° de Prácticas"
                type="number"
                min={1}
                required
                value={formEditar.numeroTotalPracticas}
                onChange={e => setFormEditar({ ...formEditar, numeroTotalPracticas: Number(e.target.value) })}
              />
              <Input
                label="Promedio Mínimo"
                type="number"
                step="0.1"
                min={0}
                max={5}
                value={formEditar.promedioMinimoGeneral}
                onChange={e => setFormEditar({ ...formEditar, promedioMinimoGeneral: Number(e.target.value) })}
              />
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalEditar(null)}>
                Cancelar
              </Button>
              <Button className="flex-1" type="submit" loading={saving}>
                Guardar Cambios
              </Button>
            </div>
          </form>
        </Modal>
      )}

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
                <Select label="Facultad" required value={form.facultadId}
                  onChange={e => setForm({ ...form, facultadId: e.target.value })}>
                  <option value="">— Selecciona una facultad —</option>
                  {facultades.map(f => <option key={f.id} value={f.id}>{f.nombre}</option>)}
                </Select>
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
