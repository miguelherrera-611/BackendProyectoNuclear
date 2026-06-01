import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponse, EstadoPractica } from '../../types'
import { asignacionService } from '../../services/asignacionService'

const ESTADOS: Array<{ label: string; value: string }> = [
  { label: 'Todas', value: '' },
  { label: 'Asignada (pendiente inicio)', value: 'ASIGNADA_PENDIENTE_INICIO' },
  { label: 'En curso', value: 'EN_CURSO' },
  { label: 'Finalizada', value: 'FINALIZADA' },
  { label: 'Cancelada', value: 'CANCELADA' },
]

const BADGE: Record<EstadoPractica, string> = {
  ASIGNADA_PENDIENTE_INICIO: 'bg-yellow-100 text-yellow-800',
  EN_CURSO: 'bg-green-100 text-green-800',
  FINALIZADA: 'bg-blue-100 text-blue-800',
  CANCELADA: 'bg-red-100 text-red-800',
}

export default function AsignacionesPage() {
  const navigate = useNavigate()
  const [estado, setEstado] = useState('')
  const [lista, setLista] = useState<InstanciaPracticaResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cancelandoId, setCanceladoId] = useState<number | null>(null)

  const cargar = async (filtroEstado = estado) => {
    setLoading(true)
    setError('')
    try {
      const data = await asignacionService.listar(filtroEstado || undefined)
      setLista(data)
    } catch {
      setError('No se pudieron cargar las asignaciones.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { cargar(estado) }, [estado])

  const handleCancelar = async (instancia: InstanciaPracticaResponse) => {
    if (instancia.estado !== 'ASIGNADA_PENDIENTE_INICIO') {
      setError('Solo se puede cancelar una asignación en estado ASIGNADA_PENDIENTE_INICIO.')
      return
    }
    const motivo = window.prompt('Motivo de cancelación (obligatorio):')?.trim()
    if (!motivo) return
    setCanceladoId(instancia.id)
    try {
      await asignacionService.cancelar(instancia.id, motivo)
      await cargar(estado)
    } catch {
      setError('No se pudo cancelar la asignación.')
    } finally {
      setCanceladoId(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Asignaciones</h1>
          <p className="text-sm text-gray-500">Gestión de asignaciones de estudiantes a vacantes.</p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/asignaciones/nueva')}>
          Nueva asignación
        </button>
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      <div className="card flex gap-4 items-end">
        <div className="w-full md:max-w-xs">
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar por estado</label>
          <select className="input-field" value={estado} onChange={e => setEstado(e.target.value)}>
            {ESTADOS.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </div>
        <button className="btn-secondary" onClick={() => cargar(estado)} disabled={loading}>Refrescar</button>
      </div>

      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {['ID', 'Práctica', 'Empresa', 'Docente Asesor', 'Tutor', 'Estado', 'Acciones'].map(h => (
                <th key={h} className="text-left px-4 py-3 text-gray-600 font-semibold">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">Cargando...</td></tr>
            ) : lista.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">No hay asignaciones para mostrar.</td></tr>
            ) : lista.map(i => (
              <tr key={i.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="px-4 py-3 text-gray-500">#{i.id}</td>
                <td className="px-4 py-3">
                  <div className="font-medium text-gray-900">{i.nombre}</div>
                  <div className="text-xs text-gray-500">Práctica #{i.numeroPractica}</div>
                </td>
                <td className="px-4 py-3 text-gray-600">{i.razonSocialEmpresa ?? '—'}</td>
                <td className="px-4 py-3 text-gray-600">{i.nombreDocenteAsesor ?? <span className="text-gray-400">Sin asignar</span>}</td>
                <td className="px-4 py-3 text-gray-600">{i.nombreTutorEmpresarial ?? <span className="text-gray-400">Sin asignar</span>}</td>
                <td className="px-4 py-3">
                  <span className={`text-xs font-medium px-2 py-1 rounded-full ${BADGE[i.estado]}`}>
                    {i.estado.replace(/_/g, ' ')}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button
                      className="btn-secondary text-xs"
                      onClick={() => navigate(`/vinculacion/${i.id}`)}
                    >
                      Vinculación
                    </button>
                    {i.estado === 'ASIGNADA_PENDIENTE_INICIO' && (
                      <button
                        className="btn-secondary text-xs text-red-600 border-red-200 hover:bg-red-50"
                        onClick={() => handleCancelar(i)}
                        disabled={cancelandoId === i.id}
                      >
                        Cancelar
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
