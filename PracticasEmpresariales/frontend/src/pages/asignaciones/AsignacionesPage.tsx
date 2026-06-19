import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponse, EstadoPractica, ChecklistItemResponse, Pageable } from '../../types'
import { asignacionService } from '../../services/asignacionService'
import { sprint4Service } from '../../services/sprint4Service'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Select } from '../../components/common/Select/Select'
import { Table } from '../../components/common/Table/Table'
import { Pagination } from '../../components/common/Table/Pagination'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'

const ESTADOS: Array<{ label: string; value: string }> = [
  { label: 'Todas',                        value: '' },
  { label: 'Asignada (pendiente inicio)',   value: 'ASIGNADA_PENDIENTE_INICIO' },
  { label: 'En curso',                     value: 'EN_CURSO' },
  { label: 'Finalizada',                   value: 'FINALIZADA' },
  { label: 'Cancelada',                    value: 'CANCELADA' },
]

const BADGE: Record<EstadoPractica, string> = {
  ASIGNADA_PENDIENTE_INICIO: 'bg-yellow-100 text-yellow-800',
  EN_CURSO:                  'bg-green-100 text-green-800',
  FINALIZADA:                'bg-blue-100 text-blue-800',
  CANCELADA:                 'bg-red-100 text-red-800',
}

export default function AsignacionesPage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [estado, setEstado]         = useState('')
  const [lista, setLista]           = useState<InstanciaPracticaResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<InstanciaPracticaResponse> | null>(null)
  const [busqueda, setBusqueda]     = useState('')
  const [loading, setLoading]       = useState(true)
  const [cancelandoId, setCanceladoId] = useState<number | null>(null)
  const [modalCancelar, setModalCancelar] = useState<{ open: boolean; instancia: InstanciaPracticaResponse | null }>({
    open: false, instancia: null,
  })
  const [motivoCancelacion, setMotivoCancelacion] = useState('')

  // Estado para el flujo "Dar cierre"
  const [cierreError, setCierreError] = useState<{ open: boolean; items: ChecklistItemResponse[] }>({ open: false, items: [] })
  const [cierreConfirm, setCierreConfirm] = useState<{ open: boolean; instancia: InstanciaPracticaResponse | null }>({
    open: false, instancia: null,
  })
  const [cierreExito, setCierreExito] = useState<{ open: boolean; nombre: string }>({ open: false, nombre: '' })
  const [cerrando, setCerrando] = useState<number | null>(null)

  const cargar = async (filtroEstado = estado) => {
    setLoading(true)
    try {
      const data = await asignacionService.listarPaginado(filtroEstado || undefined, pagina)
      setLista(data.content)
      setPageData(data)
    } catch {
      showToast('No se pudieron cargar las asignaciones.', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { cargar(estado) }, [estado, pagina])

  const listaFiltrada = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return lista.filter(i => {
      const coincideTexto = !texto || [i.nombre, i.razonSocialEmpresa, i.nombreDocenteAsesor, i.nombreTutorEmpresarial].some(valor => valor?.toLowerCase().includes(texto))
      return coincideTexto
    })
  }, [busqueda, lista])

  const limpiarFiltros = () => setBusqueda('')

  const handleEstadoChange = (value: string) => {
    setEstado(value)
    setPagina(0)
  }

  const handleCancelar = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!modalCancelar.instancia || !motivoCancelacion.trim()) return
    setCanceladoId(modalCancelar.instancia.id)
    try {
      await asignacionService.cancelar(modalCancelar.instancia.id, motivoCancelacion.trim())
      setModalCancelar({ open: false, instancia: null })
      setMotivoCancelacion('')
      showToast('Asignación cancelada.')
      await cargar(estado)
    } catch {
      showToast('No se pudo cancelar la asignación.', 'error')
    } finally {
      setCanceladoId(null)
    }
  }

  const handleDarCierreClick = async (instancia: InstanciaPracticaResponse) => {
    setCerrando(instancia.id)
    try {
      const checklist = await sprint4Service.checklist(instancia.id)

      if (!checklist.puedeEjecutarCierre) {
        const pendientes = checklist.items.filter(i => !i.completo)
        setCierreError({ open: true, items: pendientes })
        return
      }

      setCierreConfirm({ open: true, instancia })
    } catch {
      showToast('No se pudo verificar el estado de la práctica.', 'error')
    } finally {
      setCerrando(null)
    }
  }

  const handleConfirmarCierre = async () => {
    if (!cierreConfirm.instancia) return
    const instancia = cierreConfirm.instancia
    setCierreConfirm({ open: false, instancia: null })
    setCerrando(instancia.id)
    try {
      await sprint4Service.ejecutarCierre(instancia.id)
      setCierreExito({ open: true, nombre: instancia.nombre })
      await cargar(estado)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
        ?? 'No se pudo cerrar la práctica.'
      showToast(msg, 'error')
    } finally {
      setCerrando(null)
    }
  }

  const HEADERS = ['ID', 'Práctica', 'Empresa', 'Docente Asesor', 'Tutor', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Asignaciones</h1>
          <p className="text-sm text-gray-500">Gestión de asignaciones de estudiantes a vacantes.</p>
        </div>
        <Button onClick={() => navigate('/asignaciones/nueva')}>Nueva asignación</Button>
      </div>

      <div className="card py-3 flex gap-4 items-end">
        <div className="w-full md:max-w-xs">
          <Select label="Filtrar por estado" value={estado} onChange={e => handleEstadoChange(e.target.value)}>
            {ESTADOS.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
          </Select>
        </div>
        <button className="btn-secondary self-end" onClick={() => cargar(estado)} disabled={loading}>Refrescar</button>
      </div>

      <ListFilters
        search={{
          label: 'Buscar asignación',
          placeholder: 'Práctica, empresa, docente o tutor...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${listaFiltrada.length} de ${lista.length}`}
        onClear={limpiarFiltros}
      />

      <Table headers={HEADERS} loading={loading} empty={listaFiltrada.length === 0}
        emptyMessage={lista.length === 0 ? 'No hay asignaciones para mostrar.' : 'No hay asignaciones que coincidan con los filtros.'} emptyIcon="🔗">
        {listaFiltrada.map(i => (
          <tr key={i.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 text-gray-500 text-sm">#{i.id}</td>
            <td className="px-4 py-3">
              <div className="font-medium text-gray-900">{i.nombre}</div>
              <div className="text-xs text-gray-500">Práctica #{i.numeroPractica}</div>
            </td>
            <td className="px-4 py-3 text-gray-600 text-sm">{i.razonSocialEmpresa ?? '—'}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{i.nombreDocenteAsesor ?? <span className="text-gray-400">Sin asignar</span>}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{i.nombreTutorEmpresarial ?? <span className="text-gray-400">Sin asignar</span>}</td>
            <td className="px-4 py-3">
              <span className={`text-xs font-medium px-2 py-1 rounded-full ${BADGE[i.estado]}`}>
                {i.estado.replace(/_/g, ' ')}
              </span>
            </td>
            <td className="px-4 py-3">
              <div className="flex gap-2">
                <button className="btn-secondary text-xs" onClick={() => navigate(`/vinculacion/${i.id}`)}>
                  Vinculación
                </button>
                {i.estado === 'ASIGNADA_PENDIENTE_INICIO' && (
                  <button
                    className="text-xs bg-red-50 text-red-600 border border-red-200 px-3 py-1 rounded-lg hover:bg-red-100 transition-colors font-medium"
                    onClick={() => { setModalCancelar({ open: true, instancia: i }); setMotivoCancelacion('') }}
                    disabled={cancelandoId === i.id}
                  >
                    Cancelar
                  </button>
                )}
                {i.estado === 'EN_CURSO' && (
                  <button
                    className="text-xs bg-blue-50 text-blue-700 border border-blue-200 px-3 py-1 rounded-lg hover:bg-blue-100 transition-colors font-medium disabled:opacity-50"
                    onClick={() => handleDarCierreClick(i)}
                    disabled={cerrando === i.id}
                  >
                    {cerrando === i.id ? 'Verificando...' : 'Dar cierre'}
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

      {modalCancelar.open && (
        <Modal title="Cancelar asignación"
          subtitle={modalCancelar.instancia?.nombre}
          onClose={() => setModalCancelar({ open: false, instancia: null })}>
          <form onSubmit={handleCancelar} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Motivo de cancelación <span className="text-red-500">*</span>
              </label>
              <textarea className="input-field" rows={3} required
                placeholder="Describe el motivo de la cancelación..."
                value={motivoCancelacion} onChange={e => setMotivoCancelacion(e.target.value)} />
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => setModalCancelar({ open: false, instancia: null })}>Cancelar</Button>
              <Button variant="danger" className="flex-1" type="submit" loading={cancelandoId !== null}>
                Confirmar cancelación
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: requisitos pendientes para el cierre */}
      {cierreError.open && (
        <Modal title="No se puede cerrar la práctica" onClose={() => setCierreError({ open: false, items: [] })} size="sm">
          <div className="space-y-4">
            <div className="bg-amber-50 border border-amber-200 rounded-lg px-4 py-3">
              <p className="text-sm font-medium text-amber-800 mb-2">
                Faltan los siguientes requisitos para poder dar el cierre:
              </p>
              <ul className="space-y-1">
                {cierreError.items.map(item => (
                  <li key={item.codigo} className="flex items-start gap-2 text-sm text-amber-700">
                    <span className="text-amber-500 mt-0.5">✗</span>
                    <span>{item.nombre}</span>
                  </li>
                ))}
              </ul>
            </div>
            <Button className="w-full" onClick={() => setCierreError({ open: false, items: [] })}>Entendido</Button>
          </div>
        </Modal>
      )}

      {/* Modal: confirmar cierre */}
      <ConfirmModal
        open={cierreConfirm.open}
        title="¿Dar cierre a esta práctica?"
        message={`La práctica "${cierreConfirm.instancia?.nombre}" pasará a estado FINALIZADA de forma irreversible. Esta acción no se puede deshacer.`}
        confirmLabel="Confirmar cierre"
        variant="primary"
        onConfirm={handleConfirmarCierre}
        onCancel={() => setCierreConfirm({ open: false, instancia: null })}
      />

      {/* Modal: cierre exitoso */}
      {cierreExito.open && (
        <Modal title="Práctica cerrada" onClose={() => setCierreExito({ open: false, nombre: '' })} size="sm">
          <div className="space-y-4 text-center">
            <div className="text-5xl">✅</div>
            <p className="text-gray-800 font-medium">{cierreExito.nombre}</p>
            <p className="text-sm text-gray-500">
              La práctica ha sido cerrada exitosamente. Todos los actores (estudiante, docente asesor, tutor y coordinación académica) han sido notificados y la práctica aparece como <strong>FINALIZADA</strong>.
            </p>
            <Button className="w-full" onClick={() => setCierreExito({ open: false, nombre: '' })}>Aceptar</Button>
          </div>
        </Modal>
      )}
    </div>
  )
}
