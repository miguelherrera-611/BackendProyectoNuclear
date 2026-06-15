import { useEffect, useMemo, useState } from 'react'
import { EstadoEstudiante, Pageable, UsuarioResponse, ApiResponse, CatalogoPracticaResponse } from '../../types'
import { estudianteService } from '../../services/estudianteService'
import api from '../../services/api'
import { Modal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Table } from '../../components/common/Table/Table'
import { Pagination } from '../../components/common/Table/Pagination'
import { useToast } from '../../components/common/Notifications/Toast'

const ESTADOS: Array<{ label: string; value: '' | EstadoEstudiante }> = [
  { label: 'Pendientes (NO_APTO)', value: 'NO_APTO' },
  { label: 'Validados (APTO)',     value: 'APTO' },
  { label: 'Todos',               value: '' },
]

export default function EstudiantesValidacionPage() {
  const { showToast } = useToast()
  const [estado, setEstado]         = useState<'' | EstadoEstudiante>('NO_APTO')
  const [pageData, setPageData]     = useState<Pageable<UsuarioResponse> | null>(null)
  const [pagina, setPagina]         = useState(0)
  const [loading, setLoading]       = useState(true)
  const [processing, setProcessing] = useState(false)
  const [busqueda, setBusqueda]     = useState('')

  const [modalApto, setModalApto] = useState<{ open: boolean; estudianteId: number; nombre: string }>({
    open: false, estudianteId: 0, nombre: '',
  })
  const [modalEnviar, setModalEnviar] = useState<{ open: boolean; estudianteId: number; nombre: string }>({
    open: false, estudianteId: 0, nombre: '',
  })
  const [modalBulkEnviar, setModalBulkEnviar] = useState(false)
  const [seleccionados, setSeleccionados]     = useState<Set<number>>(new Set())

  const [catalogos, setCatalogos]           = useState<CatalogoPracticaResponse[]>([])
  const [catalogoSeleccionado, setCatalogo] = useState('')

  const estudiantes = useMemo(() => pageData?.content ?? [], [pageData])

  const cargar = async (estadoFiltro: '' | EstadoEstudiante = estado, page = pagina) => {
    setLoading(true)
    try {
      const data = await estudianteService.listar(estadoFiltro || undefined, page, 10)
      setPageData(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { setPagina(0); cargar(estado, 0) }, [estado])
  useEffect(() => { cargar(estado, pagina) }, [pagina])
  useEffect(() => { setSeleccionados(new Set()) }, [estado, pagina])

  const abrirModalApto = async (e: UsuarioResponse) => {
    setModalApto({ open: true, estudianteId: e.id, nombre: e.nombre })
    setCatalogo('')
    const r = await api.get<ApiResponse<CatalogoPracticaResponse[]>>('/api/v1/catalogo-practicas')
    setCatalogos(r.data.datos ?? [])
  }

  const handleMarcarApto = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!catalogoSeleccionado) return
    setProcessing(true)
    try {
      await estudianteService.marcarApto(modalApto.estudianteId, Number(catalogoSeleccionado))
      setModalApto({ open: false, estudianteId: 0, nombre: '' })
      showToast(`${modalApto.nombre} marcado como APTO.`)
      await cargar(estado, pagina)
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al marcar APTO.', 'error')
    } finally {
      setProcessing(false)
    }
  }

  const handleEnviarIndividual = async () => {
    setProcessing(true)
    try {
      await estudianteService.enviarAlProceso([modalEnviar.estudianteId])
      setModalEnviar({ open: false, estudianteId: 0, nombre: '' })
      showToast(`${modalEnviar.nombre} enviado a Coordinación de Prácticas.`)
      await cargar(estado, pagina)
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al enviar al proceso.', 'error')
    } finally {
      setProcessing(false)
    }
  }

  const handleEnviarBulk = async () => {
    setProcessing(true)
    try {
      await estudianteService.enviarAlProceso([...seleccionados])
      const count = seleccionados.size
      setSeleccionados(new Set())
      setModalBulkEnviar(false)
      showToast(`${count} estudiante${count !== 1 ? 's' : ''} enviado${count !== 1 ? 's' : ''} a Coordinación de Prácticas.`)
      await cargar(estado, pagina)
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al enviar al proceso.', 'error')
    } finally {
      setProcessing(false)
    }
  }

  const estudiantesFiltrados = busqueda
    ? estudiantes.filter(e =>
        e.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
        (e.correo ?? '').toLowerCase().includes(busqueda.toLowerCase()))
    : estudiantes

  const seleccionables = useMemo(
    () => estudiantesFiltrados.filter(e => e.estadoEstudiante === 'APTO' && !e.enviadoAlProceso),
    [estudiantesFiltrados]
  )
  const todosSeleccionados = seleccionables.length > 0 && seleccionables.every(e => seleccionados.has(e.id))
  const algunoSeleccionado = seleccionables.some(e => seleccionados.has(e.id))

  const toggleSeleccion = (id: number) => {
    setSeleccionados(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  const toggleTodos = () => {
    if (todosSeleccionados) {
      setSeleccionados(new Set())
    } else {
      setSeleccionados(new Set(seleccionables.map(e => e.id)))
    }
  }

  const checkboxHeader = (
    <input
      type="checkbox"
      className="accent-cue-primary h-4 w-4 cursor-pointer"
      checked={todosSeleccionados}
      ref={el => { if (el) el.indeterminate = algunoSeleccionado && !todosSeleccionados }}
      onChange={toggleTodos}
      disabled={seleccionables.length === 0}
      title={todosSeleccionados ? 'Deseleccionar todos' : 'Seleccionar todos los aptos pendientes'}
    />
  )

  const HEADERS = [checkboxHeader, 'Estudiante', 'Contacto', 'Programa', 'Semestre', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Validación de Estudiantes</h1>
        <p className="text-sm text-gray-500 mt-1">
          Revisa el estado académico y autoriza el ingreso al proceso de prácticas.
        </p>
      </div>

      <div className="card py-3 flex gap-4 items-end flex-wrap">
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">Estado</label>
          <div className="flex gap-2">
            {ESTADOS.map(s => (
              <button key={s.label} type="button" onClick={() => setEstado(s.value)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  estado === s.value ? 'bg-cue-primary text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}>
                {s.label}
              </button>
            ))}
          </div>
        </div>
        <div className="flex-1 min-w-48">
          <label className="block text-xs font-medium text-gray-600 mb-1">Buscar</label>
          <Input placeholder="Nombre o correo..." value={busqueda} onChange={e => setBusqueda(e.target.value)} />
        </div>
        <button className="btn-secondary self-end" onClick={() => cargar(estado, pagina)} disabled={loading}>
          Refrescar
        </button>
      </div>

      {/* Barra de acción masiva */}
      {seleccionados.size > 0 && (
        <div className="flex items-center justify-between bg-cue-primary/5 border border-cue-primary/20 rounded-xl px-4 py-3">
          <p className="text-sm font-medium text-cue-primary">
            {seleccionados.size} estudiante{seleccionados.size !== 1 ? 's' : ''} seleccionado{seleccionados.size !== 1 ? 's' : ''}
          </p>
          <div className="flex gap-3">
            <button
              onClick={() => setSeleccionados(new Set())}
              className="text-xs text-gray-500 hover:text-gray-700 transition-colors"
            >
              Limpiar selección
            </button>
            <Button onClick={() => setModalBulkEnviar(true)} disabled={processing}>
              Enviar {seleccionados.size} a Coord. Prácticas
            </Button>
          </div>
        </div>
      )}

      <Table headers={HEADERS} loading={loading} empty={estudiantesFiltrados.length === 0}
        emptyMessage="No hay estudiantes para mostrar." emptyIcon="👨‍🎓">
        {estudiantesFiltrados.map(e => {
          const esSeleccionable = e.estadoEstudiante === 'APTO' && !e.enviadoAlProceso
          const estaSeleccionado = seleccionados.has(e.id)
          return (
            <tr key={e.id}
              className={`border-b border-gray-100 hover:bg-gray-50 transition-colors ${estaSeleccionado ? 'bg-blue-50/60' : ''}`}>
              <td className="px-4 py-3 w-8">
                {esSeleccionable ? (
                  <input
                    type="checkbox"
                    className="accent-cue-primary h-4 w-4 cursor-pointer"
                    checked={estaSeleccionado}
                    onChange={() => toggleSeleccion(e.id)}
                  />
                ) : (
                  <span className="block w-4" />
                )}
              </td>
              <td className="px-4 py-3">
                <div className="font-medium text-gray-900">{e.nombre}</div>
                <div className="text-xs text-gray-400">{e.identificacion ?? 'Sin identificación'}</div>
              </td>
              <td className="px-4 py-3 text-gray-600 text-sm">
                <div>{e.correo}</div>
                <div className="text-xs text-gray-400">{e.telefono ?? '—'}</div>
              </td>
              <td className="px-4 py-3 text-gray-600 text-sm">
                <div>{e.programaNombre ?? '—'}</div>
                <div className="text-xs text-gray-400">{e.facultadNombre ?? '—'}</div>
              </td>
              <td className="px-4 py-3 text-center text-gray-700">{e.semestre ?? '—'}</td>
              <td className="px-4 py-3">
                <span className={e.estadoEstudiante === 'APTO' ? 'badge-apto' : 'badge-no-apto'}>
                  {e.estadoEstudiante ?? 'N/D'}
                </span>
              </td>
              <td className="px-4 py-3">
                {e.estadoEstudiante !== 'APTO' ? (
                  <button onClick={() => abrirModalApto(e)} disabled={processing}
                    className="text-xs bg-green-50 text-green-700 px-3 py-1 rounded-lg hover:bg-green-100 transition-colors font-medium">
                    Marcar APTO
                  </button>
                ) : e.enviadoAlProceso ? (
                  <span className="text-xs font-medium bg-blue-50 text-blue-700 px-2.5 py-1 rounded-full">
                    Enviado
                  </span>
                ) : (
                  <button
                    onClick={() => setModalEnviar({ open: true, estudianteId: e.id, nombre: e.nombre })}
                    disabled={processing}
                    className="text-xs bg-cue-primary text-white px-3 py-1.5 rounded-lg hover:opacity-90 transition-opacity font-medium whitespace-nowrap"
                  >
                    Enviar a Coord. Prácticas
                  </button>
                )}
              </td>
            </tr>
          )
        })}
      </Table>

      <Pagination
        page={pagina}
        totalPages={pageData?.totalPages ?? 0}
        totalElements={pageData?.totalElements}
        onPageChange={setPagina}
        disabled={loading}
      />

      {/* Modal: envío individual */}
      {modalEnviar.open && (
        <Modal
          title="Enviar a Coordinación de Prácticas"
          subtitle={`¿Deseas enviar a ${modalEnviar.nombre} al proceso de asignación de prácticas?`}
          onClose={() => setModalEnviar({ open: false, estudianteId: 0, nombre: '' })}
        >
          <p className="text-sm text-gray-600 mb-6">
            Una vez enviado, el estudiante quedará disponible para que el Coordinador de Prácticas
            lo asigne a una empresa. Esta acción no se puede deshacer desde este panel.
          </p>
          <div className="flex gap-3">
            <Button variant="secondary" className="flex-1" type="button"
              onClick={() => setModalEnviar({ open: false, estudianteId: 0, nombre: '' })}>
              Cancelar
            </Button>
            <Button className="flex-1" loading={processing} onClick={handleEnviarIndividual}>
              Confirmar envío
            </Button>
          </div>
        </Modal>
      )}

      {/* Modal: envío masivo */}
      {modalBulkEnviar && (
        <Modal
          title="Envío masivo a Coordinación de Prácticas"
          subtitle={`Se enviarán ${seleccionados.size} estudiante${seleccionados.size !== 1 ? 's' : ''} al proceso de asignación.`}
          onClose={() => setModalBulkEnviar(false)}
        >
          <p className="text-sm text-gray-600 mb-6">
            Todos los estudiantes seleccionados quedarán disponibles para que el Coordinador de Prácticas
            los asigne a una empresa. Esta acción no se puede deshacer desde este panel.
          </p>
          <div className="flex gap-3">
            <Button variant="secondary" className="flex-1" type="button"
              onClick={() => setModalBulkEnviar(false)}>
              Cancelar
            </Button>
            <Button className="flex-1" loading={processing} onClick={handleEnviarBulk}>
              Confirmar envío de {seleccionados.size}
            </Button>
          </div>
        </Modal>
      )}

      {/* Modal: marcar APTO */}
      {modalApto.open && (
        <Modal title="Marcar como APTO" subtitle={`Selecciona el catálogo para ${modalApto.nombre}.`}
          onClose={() => setModalApto({ open: false, estudianteId: 0, nombre: '' })}>
          <form onSubmit={handleMarcarApto} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Catálogo de práctica <span className="text-red-500">*</span>
              </label>
              <select className="input-field" required value={catalogoSeleccionado}
                onChange={e => setCatalogo(e.target.value)}>
                <option value="">— Selecciona un catálogo —</option>
                {catalogos.filter(c => c.activo).map(c => (
                  <option key={c.id} value={c.id}>
                    {c.programaNombre} · Práctica {c.numeroPractica} — {c.nombre}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => setModalApto({ open: false, estudianteId: 0, nombre: '' })}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={processing}>Confirmar APTO</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
