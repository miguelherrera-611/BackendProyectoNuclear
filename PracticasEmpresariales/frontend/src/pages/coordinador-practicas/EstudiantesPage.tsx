import { useEffect, useMemo, useState } from 'react'
import { EstadoEstudiante, Pageable, UsuarioResponse } from '../../types'
import { estudianteService } from '../../services/estudianteService'

const ESTADOS: Array<{ label: string; value: '' | EstadoEstudiante }> = [
  { label: 'Todos', value: '' },
  { label: 'No aptos', value: 'NO_APTO' },
  { label: 'Aptos', value: 'APTO' },
]

export default function EstudiantesPage() {
  const [estado, setEstado] = useState<'' | EstadoEstudiante>('APTO')
  const [pageData, setPageData] = useState<Pageable<UsuarioResponse> | null>(null)
  const [pagina, setPagina] = useState(0)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const estudiantes = useMemo(() => pageData?.content ?? [], [pageData])

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

  useEffect(() => {
    setPagina(0)
    setSelectedIds([])
    cargar(estado, 0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [estado])

  useEffect(() => {
    cargar(estado, pagina)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina])

  const toggleSelection = (id: number) => {
    setSelectedIds((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    )
  }

  const handleMarcarApto = async (id: number) => {
    const catalogoPracticaId = Number(window.prompt('ID del catálogo de práctica para crear la instancia'))
    if (!catalogoPracticaId) return

    setProcessing(true)
    setError('')
    try {
      await estudianteService.marcarApto(id, catalogoPracticaId)
      await cargar(estado, pagina)
    } catch {
      setError('No se pudo marcar el estudiante como APTO.')
    } finally {
      setProcessing(false)
    }
  }

  const handleNoApto = async (id: number) => {
    const motivo = window.prompt('Motivo para mantener al estudiante como NO_APTO')?.trim()
    if (!motivo) return

    setProcessing(true)
    setError('')
    try {
      await estudianteService.mantenerNoApto(id, motivo)
      await cargar(estado, pagina)
    } catch {
      setError('No se pudo registrar el motivo de NO_APTO.')
    } finally {
      setProcessing(false)
    }
  }

  const handleEnviarAlProceso = async () => {
    if (selectedIds.length === 0) {
      setError('Selecciona al menos un estudiante APTO para enviarlo al proceso.')
      return
    }

    setProcessing(true)
    setError('')
    try {
      await estudianteService.enviarAlProceso(selectedIds)
      setSelectedIds([])
      await cargar(estado, pagina)
    } catch {
      setError('No se pudieron enviar los estudiantes al proceso.')
    } finally {
      setProcessing(false)
    }
  }

  const totalPages = pageData?.totalPages ?? 0
  const canGoBack = pagina > 0
  const canGoNext = totalPages > 0 && pagina < totalPages - 1

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Estudiantes</h1>
          <p className="text-sm text-gray-500">Listado de estudiantes con estado académico y acciones del proceso.</p>
        </div>
        <button
          type="button"
          className="btn-primary"
          onClick={handleEnviarAlProceso}
          disabled={processing || selectedIds.length === 0}
        >
          Enviar seleccionados al proceso
        </button>
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700">{error}</div>}

      <div className="card flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="w-full md:max-w-sm">
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar por estado</label>
          <select className="input-field" value={estado} onChange={(e) => setEstado(e.target.value as '' | EstadoEstudiante)}>
            {ESTADOS.map((item) => (
              <option key={item.label} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500">
          <span className="badge-apto">{selectedIds.length} seleccionados</span>
          <button type="button" className="btn-secondary" onClick={() => cargar(estado, pagina)} disabled={loading}>
            Refrescar
          </button>
        </div>
      </div>

      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {['', 'Estudiante', 'Contacto', 'Programa', 'Estado', 'Acciones'].map((header) => (
                <th key={header || 'checkbox'} className="text-left px-4 py-3 text-gray-600 font-semibold">
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="text-center py-8 text-gray-400">
                  Cargando estudiantes...
                </td>
              </tr>
            ) : estudiantes.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center py-8 text-gray-400">
                  No hay estudiantes para mostrar.
                </td>
              </tr>
            ) : (
              estudiantes.map((estudiante) => (
                <tr key={estudiante.id} className="border-b border-gray-100 hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(estudiante.id)}
                      onChange={() => toggleSelection(estudiante.id)}
                      disabled={estudiante.estadoEstudiante !== 'APTO'}
                      title={estudiante.estadoEstudiante !== 'APTO' ? 'Solo se envían estudiantes APTO' : 'Seleccionar'}
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div className="font-medium text-gray-900">{estudiante.nombre}</div>
                    <div className="text-xs text-gray-500">ID {estudiante.id} · {estudiante.identificacion ?? 'Sin identificación'}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    <div>{estudiante.correo}</div>
                    <div className="text-xs text-gray-500">{estudiante.telefono ?? 'Sin teléfono'}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    <div>{estudiante.facultadNombre ?? '—'}</div>
                    <div className="text-xs text-gray-500">{estudiante.programaNombre ?? 'Sin programa'}</div>
                  </td>
                  <td className="px-4 py-3">
                    <span className={estudiante.estadoEstudiante === 'APTO' ? 'badge-apto' : 'badge-no-apto'}>
                      {estudiante.estadoEstudiante ?? 'N/D'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <button type="button" className="btn-secondary" onClick={() => handleMarcarApto(estudiante.id)} disabled={processing}>
                        Marcar APTO
                      </button>
                      <button type="button" className="btn-secondary" onClick={() => handleNoApto(estudiante.id)} disabled={processing}>
                        Mantener NO_APTO
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-gray-500">
          Página {pageData ? pageData.number + 1 : 1} de {totalPages || 1}
        </p>
        <div className="flex gap-2">
          <button type="button" className="btn-secondary" onClick={() => setPagina((current) => Math.max(0, current - 1))} disabled={!canGoBack || loading}>
            Anterior
          </button>
          <button type="button" className="btn-secondary" onClick={() => setPagina((current) => current + 1)} disabled={!canGoNext || loading}>
            Siguiente
          </button>
        </div>
      </div>
    </div>
  )
}

