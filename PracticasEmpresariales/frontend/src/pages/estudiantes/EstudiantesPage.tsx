import { useEffect, useMemo, useState } from 'react'
import { EstadoEstudiante, Pageable, UsuarioResponse } from '../../types'
import { estudianteService } from '../../services/estudianteService'
import { Modal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Select } from '../../components/common/Select/Select'
import { Table } from '../../components/common/Table/Table'
import { Pagination } from '../../components/common/Table/Pagination'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'
import { catalogoPracticaService } from '../../services/catalogoPracticaService'
import type { CatalogoPracticaResponse } from '../../types'

const ESTADOS: Array<{ label: string; value: '' | EstadoEstudiante }> = [
  { label: 'Todos',    value: '' },
  { label: 'No aptos', value: 'NO_APTO' },
  { label: 'Aptos',   value: 'APTO' },
]

export default function EstudiantesPage() {
  const { showToast } = useToast()
  const [estado, setEstado]         = useState<'' | EstadoEstudiante>('APTO')
  const [busqueda, setBusqueda]     = useState('')
  const [pageData, setPageData]     = useState<Pageable<UsuarioResponse> | null>(null)
  const [pagina, setPagina]         = useState(0)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [loading, setLoading]       = useState(true)
  const [processing, setProcessing] = useState(false)
  const [error, setError]           = useState('')

  const [modalApto, setModalApto] = useState<{ open: boolean; id: number; nombre: string }>({ open: false, id: 0, nombre: '' })
  const [catalogos, setCatalogos]           = useState<CatalogoPracticaResponse[]>([])
  const [catalogoSeleccionado, setCatalogo] = useState('')

  const [modalNoApto, setModalNoApto] = useState<{ open: boolean; id: number; nombre: string }>({ open: false, id: 0, nombre: '' })
  const [motivoNoApto, setMotivoNoApto] = useState('')

  const estudiantes = useMemo(() => pageData?.content ?? [], [pageData])
  const estudiantesFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return estudiantes.filter(e => !texto || [e.nombre, e.correo, e.identificacion, e.programaNombre, e.facultadNombre].some(valor => valor?.toLowerCase().includes(texto)))
  }, [busqueda, estudiantes])

  const cargar = async (estadoFiltro: '' | EstadoEstudiante = estado, page = pagina) => {
    setLoading(true)
    setError('')
    try {
      const data = await estudianteService.listar(estadoFiltro || undefined, page, 10)
      setPageData(data)
    } catch {
      setError('No se pudo cargar el listado de estudiantes.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { setPagina(0); setSelectedIds([]); cargar(estado, 0) }, [estado])
  useEffect(() => { cargar(estado, pagina) }, [pagina])

  const toggleSelection = (id: number) =>
    setSelectedIds(curr => curr.includes(id) ? curr.filter(x => x !== id) : [...curr, id])

  const limpiarFiltros = () => setBusqueda('')

  const abrirModalApto = async (e: UsuarioResponse) => {
    setModalApto({ open: true, id: e.id, nombre: e.nombre })
    setCatalogo('')
    const lista = await catalogoPracticaService.listar()
    setCatalogos(lista)
  }

  const handleMarcarApto = async (ev: React.FormEvent) => {
    ev.preventDefault()
    if (!catalogoSeleccionado) return
    setProcessing(true)
    try {
      await estudianteService.marcarApto(modalApto.id, Number(catalogoSeleccionado))
      setModalApto({ open: false, id: 0, nombre: '' })
      showToast(`${modalApto.nombre} marcado como APTO.`)
      await cargar(estado, pagina)
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al marcar APTO.', 'error')
    } finally {
      setProcessing(false)
    }
  }

  const handleMantenerNoApto = async (ev: React.FormEvent) => {
    ev.preventDefault()
    if (!motivoNoApto.trim()) return
    setProcessing(true)
    try {
      await estudianteService.mantenerNoApto(modalNoApto.id, motivoNoApto.trim())
      setModalNoApto({ open: false, id: 0, nombre: '' })
      setMotivoNoApto('')
      showToast('Motivo registrado correctamente.')
      await cargar(estado, pagina)
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error.', 'error')
    } finally {
      setProcessing(false)
    }
  }

  const handleEnviarAlProceso = async () => {
    if (selectedIds.length === 0) { setError('Selecciona al menos un estudiante APTO.'); return }
    setProcessing(true)
    setError('')
    try {
      await estudianteService.enviarAlProceso(selectedIds)
      setSelectedIds([])
      showToast(`${selectedIds.length} estudiante(s) enviado(s) al proceso.`)
      await cargar(estado, pagina)
    } catch {
      setError('No se pudieron enviar los estudiantes al proceso.')
    } finally {
      setProcessing(false)
    }
  }

  const HEADERS = ['', 'Estudiante', 'Contacto', 'Programa', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Estudiantes</h1>
          <p className="text-sm text-gray-500">Listado con estado académico y acciones del proceso.</p>
        </div>
        <Button onClick={handleEnviarAlProceso} disabled={processing || selectedIds.length === 0}>
          Enviar al proceso{selectedIds.length > 0 ? ` (${selectedIds.length})` : ''}
        </Button>
      </div>

      {error && <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">{error}</div>}

      <ListFilters
        search={{
          label: 'Buscar estudiante',
          placeholder: 'Nombre, correo, identificación o programa...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${estudiantesFiltrados.length} de ${estudiantes.length}`}
        onClear={limpiarFiltros}
      >
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
        <button className="btn-secondary self-end" onClick={() => cargar(estado, pagina)} disabled={loading}>Refrescar</button>
      </ListFilters>

      <Table headers={HEADERS} loading={loading} empty={estudiantesFiltrados.length === 0}
        emptyMessage={estudiantes.length === 0 ? 'No hay estudiantes para mostrar.' : 'No hay estudiantes que coincidan con la búsqueda.'} emptyIcon="👨‍🎓">
        {estudiantesFiltrados.map(e => (
          <tr key={e.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3">
              <input type="checkbox" checked={selectedIds.includes(e.id)} onChange={() => toggleSelection(e.id)}
                disabled={e.estadoEstudiante !== 'APTO'} className="rounded" />
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
              <div>{e.facultadNombre ?? '—'}</div>
              <div className="text-xs text-gray-400">{e.programaNombre ?? 'Sin programa'}</div>
            </td>
            <td className="px-4 py-3">
              <span className={e.estadoEstudiante === 'APTO' ? 'badge-apto' : 'badge-no-apto'}>
                {e.estadoEstudiante ?? 'N/D'}
              </span>
            </td>
            <td className="px-4 py-3">
              <div className="flex gap-2">
                <button className="text-xs bg-green-50 text-green-700 px-3 py-1 rounded-lg hover:bg-green-100 transition-colors font-medium"
                  onClick={() => abrirModalApto(e)} disabled={processing}>Marcar APTO</button>
                <button className="text-xs bg-red-50 text-red-700 px-3 py-1 rounded-lg hover:bg-red-100 transition-colors font-medium"
                  onClick={() => { setModalNoApto({ open: true, id: e.id, nombre: e.nombre }); setMotivoNoApto('') }}
                  disabled={processing}>NO_APTO</button>
              </div>
            </td>
          </tr>
        ))}
      </Table>

      <Pagination page={pagina} totalPages={pageData?.totalPages ?? 0} totalElements={pageData?.totalElements}
        onPageChange={setPagina} disabled={loading} />

      {modalApto.open && (
        <Modal title="Marcar como APTO" subtitle={`Asigna un catálogo de práctica a ${modalApto.nombre}.`}
          onClose={() => setModalApto({ open: false, id: 0, nombre: '' })}>
          <form onSubmit={handleMarcarApto} className="space-y-4">
            <Select label="Catálogo de práctica" required value={catalogoSeleccionado} onChange={e => setCatalogo(e.target.value)}>
              <option value="">— Selecciona un catálogo —</option>
              {catalogos.filter(c => c.activo).map(c => (
                <option key={c.id} value={c.id}>{c.programaNombre} · Práctica {c.numeroPractica} — {c.nombre}</option>
              ))}
            </Select>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalApto({ open: false, id: 0, nombre: '' })}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={processing}>Confirmar APTO</Button>
            </div>
          </form>
        </Modal>
      )}

      {modalNoApto.open && (
        <Modal title="Mantener como NO_APTO" subtitle={`Registra el motivo para ${modalNoApto.nombre}.`}
          onClose={() => setModalNoApto({ open: false, id: 0, nombre: '' })}>
          <form onSubmit={handleMantenerNoApto} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Motivo <span className="text-red-500">*</span></label>
              <textarea className="input-field" rows={3} required
                placeholder="Describe por qué el estudiante no cumple los requisitos..."
                value={motivoNoApto} onChange={e => setMotivoNoApto(e.target.value)} />
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalNoApto({ open: false, id: 0, nombre: '' })}>Cancelar</Button>
              <Button variant="danger" className="flex-1" type="submit" loading={processing}>Registrar Motivo</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
