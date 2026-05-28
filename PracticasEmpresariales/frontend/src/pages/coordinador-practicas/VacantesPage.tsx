import { useState, useEffect, useCallback } from 'react'
import { VacanteResponse, EmpresaResponse, EstadoVacante } from '../../types'
import { vacanteService } from '../../services/vacanteService'
import { empresaService } from '../../services/empresaService'

type Tab = 'todas' | 'pendientes' | 'disponibles'

const BADGE: Record<EstadoVacante, string> = {
  PENDIENTE:  'bg-yellow-100 text-yellow-800',
  DISPONIBLE: 'bg-green-100 text-green-800',
  RECHAZADA:  'bg-red-100 text-red-800',
  CERRADA:    'bg-gray-100 text-gray-600',
}

const FORM_INICIAL = { empresaId: 0, area: '', cuposTotales: 1 }
const RECHAZAR_INICIAL = { id: 0, motivo: '' }

export default function VacantesPage() {
  const [vacantes, setVacantes] = useState<VacanteResponse[]>([])
  const [empresas, setEmpresas] = useState<EmpresaResponse[]>([])
  const [tab, setTab] = useState<Tab>('todas')
  const [loading, setLoading] = useState(true)
  const [modalCrear, setModalCrear] = useState(false)
  const [modalRechazar, setModalRechazar] = useState(RECHAZAR_INICIAL)
  const [form, setForm] = useState(FORM_INICIAL)
  const [error, setError] = useState('')

  const cargar = useCallback(() => {
    setLoading(true)
    const fetch = tab === 'pendientes' ? vacanteService.listarPendientes()
      : tab === 'disponibles' ? vacanteService.listarDisponibles()
      : vacanteService.listar()
    fetch.then(setVacantes).finally(() => setLoading(false))
  }, [tab])

  useEffect(() => { cargar() }, [cargar])

  useEffect(() => {
    empresaService.listarAprobadas().then(setEmpresas)
  }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await vacanteService.crear(form)
      setModalCrear(false)
      setForm(FORM_INICIAL)
      cargar()
    } catch (err: unknown) {
      setError((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear la vacante.')
    }
  }

  const handleAprobar = async (id: number) => {
    if (!confirm('¿Aprobar esta vacante?')) return
    try {
      await vacanteService.aprobar(id)
      cargar()
    } catch (err: unknown) {
      alert((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al aprobar.')
    }
  }

  const handleRechazar = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await vacanteService.rechazar(modalRechazar.id, modalRechazar.motivo)
      setModalRechazar(RECHAZAR_INICIAL)
      cargar()
    } catch (err: unknown) {
      alert((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al rechazar.')
    }
  }

  const handleCerrar = async (id: number) => {
    if (!confirm('¿Cerrar esta vacante?')) return
    try {
      await vacanteService.cerrar(id)
      cargar()
    } catch (err: unknown) {
      alert((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al cerrar.')
    }
  }

  const tabClass = (t: Tab) =>
    `px-4 py-2 text-sm font-medium rounded-lg transition-colors ${tab === t
      ? 'bg-cue-primary text-white'
      : 'text-gray-600 hover:bg-gray-100'}`

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Vacantes</h1>
        <button className="btn-primary" onClick={() => setModalCrear(true)}>+ Nueva Vacante</button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2">
        <button className={tabClass('todas')} onClick={() => setTab('todas')}>Todas</button>
        <button className={tabClass('pendientes')} onClick={() => setTab('pendientes')}>Pendientes</button>
        <button className={tabClass('disponibles')} onClick={() => setTab('disponibles')}>Disponibles</button>
      </div>

      <div className="card overflow-x-auto">
        {loading ? (
          <p className="text-gray-400 py-8 text-center">Cargando...</p>
        ) : vacantes.length === 0 ? (
          <p className="text-gray-400 py-8 text-center">No hay vacantes en esta categoría.</p>
        ) : (
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b border-gray-100">
                <th className="py-3 pr-4 font-semibold">Empresa</th>
                <th className="py-3 pr-4 font-semibold">Área</th>
                <th className="py-3 pr-4 font-semibold">Cupos</th>
                <th className="py-3 pr-4 font-semibold">Publicación</th>
                <th className="py-3 pr-4 font-semibold">Estado</th>
                <th className="py-3 font-semibold">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {vacantes.map(v => (
                <tr key={v.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 pr-4 font-medium text-gray-800">{v.razonSocialEmpresa}</td>
                  <td className="py-3 pr-4 text-gray-600">{v.area}</td>
                  <td className="py-3 pr-4 text-gray-600">
                    {v.cuposOcupados} / {v.cuposTotales}
                  </td>
                  <td className="py-3 pr-4 text-gray-600">{v.fechaPublicacion}</td>
                  <td className="py-3 pr-4">
                    <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${BADGE[v.estado]}`}>
                      {v.estado}
                    </span>
                  </td>
                  <td className="py-3">
                    <div className="flex flex-wrap gap-2">
                      {v.estado === 'PENDIENTE' && (
                        <>
                          <button onClick={() => handleAprobar(v.id)}
                            className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded hover:bg-green-200 transition-colors">
                            Aprobar
                          </button>
                          <button onClick={() => setModalRechazar({ id: v.id, motivo: '' })}
                            className="text-xs bg-red-100 text-red-700 px-2 py-1 rounded hover:bg-red-200 transition-colors">
                            Rechazar
                          </button>
                        </>
                      )}
                      {v.estado === 'DISPONIBLE' && (
                        <button onClick={() => handleCerrar(v.id)}
                          className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded hover:bg-gray-200 transition-colors">
                          Cerrar
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal: Nueva Vacante */}
      {modalCrear && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Nueva Vacante</h2>
            {error && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{error}</div>}
            <form onSubmit={handleCrear} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Empresa *</label>
                <select className="input-field" required value={form.empresaId || ''}
                  onChange={e => setForm({ ...form, empresaId: Number(e.target.value) })}>
                  <option value="">Selecciona una empresa aprobada</option>
                  {empresas.map(em => (
                    <option key={em.id} value={em.id}>{em.razonSocial}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Área *</label>
                <input className="input-field" required placeholder="Ej. Sistemas, Contabilidad"
                  value={form.area}
                  onChange={e => setForm({ ...form, area: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Cupos Totales *</label>
                <input className="input-field" type="number" min={1} required
                  value={form.cuposTotales}
                  onChange={e => setForm({ ...form, cuposTotales: Number(e.target.value) })} />
              </div>
              <div className="flex gap-3">
                <button type="button" className="btn-secondary flex-1"
                  onClick={() => { setModalCrear(false); setError('') }}>Cancelar</button>
                <button type="submit" className="btn-primary flex-1">Crear</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Rechazar Vacante */}
      {modalRechazar.id > 0 && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Rechazar Vacante</h2>
            <form onSubmit={handleRechazar} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Motivo de Rechazo *</label>
                <textarea className="input-field" rows={4} required
                  value={modalRechazar.motivo}
                  onChange={e => setModalRechazar({ ...modalRechazar, motivo: e.target.value })} />
              </div>
              <div className="flex gap-3">
                <button type="button" className="btn-secondary flex-1"
                  onClick={() => setModalRechazar(RECHAZAR_INICIAL)}>Cancelar</button>
                <button type="submit" className="btn-danger flex-1">Rechazar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
