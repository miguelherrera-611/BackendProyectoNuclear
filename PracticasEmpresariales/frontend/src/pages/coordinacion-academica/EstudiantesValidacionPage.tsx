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
  const [catalogos, setCatalogos]           = useState<CatalogoPracticaResponse[]>([])
  const [catalogoSeleccionado, setCatalogo] = useState('')

  const [modalNoApto, setModalNoApto] = useState<{ open: boolean; estudianteId: number; nombre: string }>({
    open: false, estudianteId: 0, nombre: '',
  })
  const [motivoNoApto, setMotivoNoApto] = useState('')

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

  const abrirModalApto = async (e: UsuarioResponse) => {
    setModalApto({ open: true, estudianteId: e.id, nombre: e.nombre })
    setCatalogo('')
    const r = await api.get<ApiResponse<{ content: CatalogoPracticaResponse[] }>>('/api/v1/catalogo-practicas?size=100')
    setCatalogos(r.data.datos?.content ?? [])
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

  const handleMantenerNoApto = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!motivoNoApto.trim()) return
    setProcessing(true)
    try {
      await estudianteService.mantenerNoApto(modalNoApto.estudianteId, motivoNoApto.trim())
      setModalNoApto({ open: false, estudianteId: 0, nombre: '' })
      setMotivoNoApto('')
      showToast('Motivo registrado correctamente.')
      await cargar(estado, pagina)
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al registrar.', 'error')
    } finally {
      setProcessing(false)
    }
  }

  const estudiantesFiltrados = busqueda
    ? estudiantes.filter(e =>
        e.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
        (e.correo ?? '').toLowerCase().includes(busqueda.toLowerCase()))
    : estudiantes

  const HEADERS = ['Estudiante', 'Contacto', 'Programa', 'Semestre', 'Estado', 'Acciones']

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

      <Table headers={HEADERS} loading={loading} empty={estudiantesFiltrados.length === 0}
        emptyMessage="No hay estudiantes para mostrar." emptyIcon="👨‍🎓">
        {estudiantesFiltrados.map(e => (
          <tr key={e.id} className="border-b border-gray-100 hover:bg-gray-50">
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
              <div className="flex gap-2">
                <button onClick={() => abrirModalApto(e)} disabled={processing}
                  className="text-xs bg-green-50 text-green-700 px-3 py-1 rounded-lg hover:bg-green-100 transition-colors font-medium">
                  Marcar APTO
                </button>
                <button
                  onClick={() => { setModalNoApto({ open: true, estudianteId: e.id, nombre: e.nombre }); setMotivoNoApto('') }}
                  disabled={processing}
                  className="text-xs bg-red-50 text-red-700 px-3 py-1 rounded-lg hover:bg-red-100 transition-colors font-medium">
                  NO_APTO
                </button>
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

      {/* Modal: Marcar APTO */}
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

      {/* Modal: NO_APTO */}
      {modalNoApto.open && (
        <Modal title="Mantener como NO_APTO" subtitle={`Registra el motivo para ${modalNoApto.nombre}.`}
          onClose={() => setModalNoApto({ open: false, estudianteId: 0, nombre: '' })}>
          <form onSubmit={handleMantenerNoApto} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Motivo <span className="text-red-500">*</span>
              </label>
              <textarea className="input-field" rows={3} required
                placeholder="Describe por qué el estudiante no cumple los requisitos..."
                value={motivoNoApto} onChange={e => setMotivoNoApto(e.target.value)} />
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => setModalNoApto({ open: false, estudianteId: 0, nombre: '' })}>Cancelar</Button>
              <Button variant="danger" className="flex-1" type="submit" loading={processing}>Registrar</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
