import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponse, EstadoPractica } from '../../types'
import { asignacionService } from '../../services/asignacionService'
import { Modal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Table } from '../../components/common/Table/Table'
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
  const [loading, setLoading]       = useState(true)
  const [cancelandoId, setCanceladoId] = useState<number | null>(null)
  const [modalCancelar, setModalCancelar] = useState<{ open: boolean; instancia: InstanciaPracticaResponse | null }>({
    open: false, instancia: null,
  })
  const [motivoCancelacion, setMotivoCancelacion] = useState('')

  const cargar = async (filtroEstado = estado) => {
    setLoading(true)
    try {
      const data = await asignacionService.listar(filtroEstado || undefined)
      setLista(data)
    } catch {
      showToast('No se pudieron cargar las asignaciones.', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { cargar(estado) }, [estado])

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
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar por estado</label>
          <select className="input-field" value={estado} onChange={e => setEstado(e.target.value)}>
            {ESTADOS.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </div>
        <button className="btn-secondary self-end" onClick={() => cargar(estado)} disabled={loading}>Refrescar</button>
      </div>

      <Table headers={HEADERS} loading={loading} empty={lista.length === 0}
        emptyMessage="No hay asignaciones para mostrar." emptyIcon="🔗">
        {lista.map(i => (
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
              </div>
            </td>
          </tr>
        ))}
      </Table>

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
    </div>
  )
}
