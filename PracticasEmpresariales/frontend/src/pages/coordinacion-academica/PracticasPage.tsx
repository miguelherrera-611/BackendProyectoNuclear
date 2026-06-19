import { useState, useEffect, useMemo } from 'react'
import { CatalogoPracticaResponse, ProgramaResponse, ApiResponse, Pageable } from '../../types'
import { catalogoPracticaService } from '../../services/catalogoPracticaService'
import api from '../../services/api'
import { Modal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Select } from '../../components/common/Select/Select'
import { Table } from '../../components/common/Table/Table'
import { Pagination } from '../../components/common/Table/Pagination'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'

const FORM_INICIAL = {
  programaId: '',
  numeroPractica: 1,
  nombre: '',
  materiaNucleo: '',
  codigoMateria: '',
  numCortes: 3,
  duracionSemanas: 16,
  documentosRequeridos: '',
}

export default function PracticasPage() {
  const { showToast } = useToast()
  const [catalogos, setCatalogos]     = useState<CatalogoPracticaResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<CatalogoPracticaResponse> | null>(null)
  const [programas, setProgramas]     = useState<ProgramaResponse[]>([])
  const [loading, setLoading]         = useState(true)
  const [saving, setSaving]           = useState(false)
  const [modalCrear, setModalCrear]   = useState(false)
  const [modalEditar, setModalEditar] = useState<CatalogoPracticaResponse | null>(null)
  const [form, setForm]               = useState(FORM_INICIAL)
  const [formEditar, setFormEditar]   = useState<Partial<typeof FORM_INICIAL>>({})
  const [busqueda, setBusqueda] = useState('')
  const [filtroPrograma, setFiltroPrograma] = useState('')
  const [estadoFiltro, setEstadoFiltro] = useState<'todos' | 'activos' | 'inactivos'>('todos')
  const [errorModal, setErrorModal]   = useState('')

  const cargar = () => {
    setLoading(true)
    catalogoPracticaService.listarPaginado(pagina)
      .then(p => {
        setCatalogos(p.content)
        setPageData(p)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    cargar()
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas?size=100')
      .then(r => setProgramas(r.data.datos?.content ?? []))
  }, [pagina])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await catalogoPracticaService.crear({ ...form, programaId: Number(form.programaId) })
      setModalCrear(false)
      setForm(FORM_INICIAL)
      cargar()
      showToast('Catálogo creado correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear el catálogo.')
    } finally {
      setSaving(false)
    }
  }

  const handleEditar = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!modalEditar) return
    setErrorModal('')
    setSaving(true)
    try {
      await catalogoPracticaService.editar(modalEditar.id, {
        nombre:               formEditar.nombre,
        materiaNucleo:        formEditar.materiaNucleo,
        codigoMateria:        formEditar.codigoMateria,
        numCortes:            formEditar.numCortes,
        duracionSemanas:      formEditar.duracionSemanas,
        documentosRequeridos: formEditar.documentosRequeridos,
      })
      setModalEditar(null)
      cargar()
      showToast('Catálogo actualizado correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al editar.')
    } finally {
      setSaving(false)
    }
  }

  const abrirEditar = (cat: CatalogoPracticaResponse) => {
    setFormEditar({
      nombre:               cat.nombre,
      materiaNucleo:        cat.materiaNucleo,
      codigoMateria:        cat.codigoMateria,
      numCortes:            cat.numCortes,
      duracionSemanas:      cat.duracionSemanas,
      documentosRequeridos: cat.documentosRequeridos ?? '',
    })
    setErrorModal('')
    setModalEditar(cat)
  }

  const catalogosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return catalogos.filter(c => {
      const coincideTexto = !texto || [c.nombre, c.codigoMateria, c.materiaNucleo, c.programaNombre].some(valor => valor?.toLowerCase().includes(texto))
      const coincidePrograma = !filtroPrograma || String(c.programaId) === filtroPrograma
      const coincideEstado = estadoFiltro === 'todos'
        ? true
        : estadoFiltro === 'activos'
          ? c.activo
          : !c.activo
      return coincideTexto && coincidePrograma && coincideEstado
    })
  }, [busqueda, catalogos, estadoFiltro, filtroPrograma])

  const programasConCatalogos = programas.filter(p => catalogos.some(c => c.programaId === p.id))

  const limpiarFiltros = () => {
    setBusqueda('')
    setFiltroPrograma('')
    setEstadoFiltro('todos')
  }

  const HEADERS = ['#', 'Nombre', 'Programa', 'Materia Núcleo', 'Cortes', 'Semanas', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Catálogo de Prácticas</h1>
          <p className="text-sm text-gray-500 mt-1">Plantillas de prácticas por programa académico.</p>
        </div>
        <Button onClick={() => { setErrorModal(''); setForm(FORM_INICIAL); setModalCrear(true) }}>
          + Nuevo Catálogo
        </Button>
      </div>

      <div className="card py-3 flex gap-4 items-end flex-wrap">
        <ListFilters
          search={{
            label: 'Buscar catálogo',
            placeholder: 'Nombre, materia, código o programa...',
            value: busqueda,
            onChange: setBusqueda,
          }}
          summary={`${catalogosFiltrados.length} catálogo${catalogosFiltrados.length !== 1 ? 's' : ''}`}
          onClear={limpiarFiltros}
          className="w-full"
        >
          <div className="w-full sm:w-56">
            <Select label="Programa" value={filtroPrograma} onChange={e => setFiltroPrograma(e.target.value)}>
              <option value="">Todos los programas</option>
              {programasConCatalogos.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
            </Select>
          </div>
          <div className="w-full sm:w-48">
            <Select label="Estado" value={estadoFiltro} onChange={e => setEstadoFiltro(e.target.value as typeof estadoFiltro)}>
              <option value="todos">Todos</option>
              <option value="activos">Activos</option>
              <option value="inactivos">Inactivos</option>
            </Select>
          </div>
        </ListFilters>
      </div>

      <Table headers={HEADERS} loading={loading} empty={catalogosFiltrados.length === 0}
        emptyMessage="No hay catálogos registrados." emptyIcon="📄">
        {catalogosFiltrados.map(cat => (
          <tr key={cat.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 font-medium text-cue-primary">{cat.numeroPractica}</td>
            <td className="px-4 py-3">
              <div className="font-medium text-gray-900">{cat.nombre}</div>
              <div className="text-xs text-gray-400">{cat.codigoMateria}</div>
            </td>
            <td className="px-4 py-3 text-gray-600 text-sm">{cat.programaNombre}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{cat.materiaNucleo}</td>
            <td className="px-4 py-3 text-center text-gray-700">{cat.numCortes}</td>
            <td className="px-4 py-3 text-center text-gray-700">{cat.duracionSemanas}</td>
            <td className="px-4 py-3">
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${cat.activo ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-500'}`}>
                {cat.activo ? 'Activo' : 'Inactivo'}
              </span>
            </td>
            <td className="px-4 py-3">
              <button onClick={() => abrirEditar(cat)}
                className="text-xs bg-blue-50 text-blue-700 px-3 py-1 rounded-lg hover:bg-blue-100 transition-colors font-medium">
                Editar
              </button>
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

      {/* Modal: Crear */}
      {modalCrear && (
        <Modal title="Nuevo Catálogo de Práctica"
          subtitle="Define la plantilla de práctica para el programa."
          size="lg" onClose={() => { setModalCrear(false); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleCrear} className="space-y-6">

            {/* ── Sección 1: Datos del catálogo ── */}
            <div className="space-y-3">
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider border-b border-gray-200 pb-2">
                Datos del catálogo
              </h3>
              <Select label="Programa" required value={form.programaId}
                onChange={e => setForm({ ...form, programaId: e.target.value })}>
                <option value="">— Selecciona un programa —</option>
                {programas.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
              </Select>
              <div className="grid grid-cols-2 gap-3">
                <Input label="N° de práctica" type="number" min={1} required value={form.numeroPractica}
                  onChange={e => setForm({ ...form, numeroPractica: Number(e.target.value) })} />
                <Input label="Cortes" type="number" min={1} required value={form.numCortes}
                  onChange={e => setForm({ ...form, numCortes: Number(e.target.value) })} />
              </div>
              <Input label="Nombre" required value={form.nombre}
                onChange={e => setForm({ ...form, nombre: e.target.value })} />
              <div className="grid grid-cols-2 gap-3">
                <Input label="Materia núcleo" required value={form.materiaNucleo}
                  onChange={e => setForm({ ...form, materiaNucleo: e.target.value })} />
                <Input label="Código materia" required value={form.codigoMateria}
                  onChange={e => setForm({ ...form, codigoMateria: e.target.value })} />
              </div>
              <Input label="Duración (semanas)" type="number" min={1} required value={form.duracionSemanas}
                onChange={e => setForm({ ...form, duracionSemanas: Number(e.target.value) })} />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Documentos requeridos</label>
                <textarea className="input-field" rows={2} placeholder="Describe los documentos necesarios..."
                  value={form.documentosRequeridos}
                  onChange={e => setForm({ ...form, documentosRequeridos: e.target.value })} />
              </div>
            </div>

            <div className="flex gap-3 pt-2 border-t border-gray-100">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => { setModalCrear(false); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Crear catálogo</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Editar */}
      {modalEditar && (
        <Modal title="Editar Catálogo"
          subtitle={`${modalEditar.nombre} — ${modalEditar.programaNombre}`}
          size="lg" onClose={() => { setModalEditar(null); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleEditar} className="space-y-3">
            <Input label="Nombre" value={formEditar.nombre ?? ''}
              onChange={e => setFormEditar({ ...formEditar, nombre: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Materia núcleo" value={formEditar.materiaNucleo ?? ''}
                onChange={e => setFormEditar({ ...formEditar, materiaNucleo: e.target.value })} />
              <Input label="Código materia" value={formEditar.codigoMateria ?? ''}
                onChange={e => setFormEditar({ ...formEditar, codigoMateria: e.target.value })} />
              <Input label="Cortes" type="number" min={1} value={formEditar.numCortes ?? ''}
                onChange={e => setFormEditar({ ...formEditar, numCortes: Number(e.target.value) })} />
              <Input label="Semanas" type="number" min={1} value={formEditar.duracionSemanas ?? ''}
                onChange={e => setFormEditar({ ...formEditar, duracionSemanas: Number(e.target.value) })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Documentos requeridos</label>
              <textarea className="input-field" rows={2} value={formEditar.documentosRequeridos ?? ''}
                onChange={e => setFormEditar({ ...formEditar, documentosRequeridos: e.target.value })} />
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => { setModalEditar(null); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Guardar Cambios</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
