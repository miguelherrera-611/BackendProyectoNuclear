import { useState, useEffect } from 'react'
import { EmpresaResponse, EstadoEmpresa } from '../../types'
import { empresaService } from '../../services/empresaService'

const BADGE: Record<EstadoEmpresa, string> = {
  PENDIENTE: 'bg-yellow-100 text-yellow-800',
  APROBADA:  'bg-green-100 text-green-800',
  RECHAZADA: 'bg-red-100 text-red-800',
  INACTIVA:  'bg-gray-100 text-gray-600',
}

const FORM_INICIAL = {
  razonSocial: '', nit: '', sector: '', direccion: '',
  municipio: '', telefono: '', nombreContacto: '', correo: '', areas: '',
}

const CLONAR_INICIAL = { razonSocial: '', nit: '' }
const RECHAZAR_INICIAL = { id: 0, motivo: '' }

export default function EmpresasPage() {
  const [empresas, setEmpresas] = useState<EmpresaResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [modalCrear, setModalCrear] = useState(false)
  const [modalClonar, setModalClonar] = useState<{ open: boolean; id: number }>({ open: false, id: 0 })
  const [modalRechazar, setModalRechazar] = useState(RECHAZAR_INICIAL)
  const [form, setForm] = useState(FORM_INICIAL)
  const [clonarForm, setClonarForm] = useState(CLONAR_INICIAL)
  const [error, setError] = useState('')

  const cargar = () => {
    setLoading(true)
    empresaService.listar()
      .then(setEmpresas)
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await empresaService.crear({
        ...form,
        areasDisponibles: form.areas.split(',').map(a => a.trim()).filter(Boolean),
      })
      setModalCrear(false)
      setForm(FORM_INICIAL)
      cargar()
    } catch (err: unknown) {
      setError((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear la empresa.')
    }
  }

  const handleClonar = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await empresaService.clonar(modalClonar.id, clonarForm.razonSocial, clonarForm.nit)
      setModalClonar({ open: false, id: 0 })
      setClonarForm(CLONAR_INICIAL)
      cargar()
    } catch (err: unknown) {
      setError((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al clonar.')
    }
  }

  const handleAprobar = async (id: number) => {
    if (!confirm('¿Aprobar esta empresa?')) return
    try {
      await empresaService.aprobar(id)
      cargar()
    } catch (err: unknown) {
      alert((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al aprobar.')
    }
  }

  const handleRechazar = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await empresaService.rechazar(modalRechazar.id, modalRechazar.motivo)
      setModalRechazar(RECHAZAR_INICIAL)
      cargar()
    } catch (err: unknown) {
      alert((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al rechazar.')
    }
  }

  const handleInactivar = async (id: number) => {
    if (!confirm('¿Inactivar esta empresa? Sus vacantes activas serán cerradas.')) return
    try {
      await empresaService.inactivar(id)
      cargar()
    } catch (err: unknown) {
      alert((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al inactivar.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Empresas</h1>
        <button className="btn-primary" onClick={() => setModalCrear(true)}>+ Nueva Empresa</button>
      </div>

      <div className="card overflow-x-auto">
        {loading ? (
          <p className="text-gray-400 py-8 text-center">Cargando...</p>
        ) : empresas.length === 0 ? (
          <p className="text-gray-400 py-8 text-center">No hay empresas registradas.</p>
        ) : (
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b border-gray-100">
                <th className="py-3 pr-4 font-semibold">Razón Social</th>
                <th className="py-3 pr-4 font-semibold">NIT</th>
                <th className="py-3 pr-4 font-semibold">Sector</th>
                <th className="py-3 pr-4 font-semibold">Municipio</th>
                <th className="py-3 pr-4 font-semibold">Contacto</th>
                <th className="py-3 pr-4 font-semibold">Estado</th>
                <th className="py-3 font-semibold">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {empresas.map(e => (
                <tr key={e.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 pr-4 font-medium text-gray-800">{e.razonSocial}</td>
                  <td className="py-3 pr-4 text-gray-600">{e.nit}</td>
                  <td className="py-3 pr-4 text-gray-600">{e.sector ?? '—'}</td>
                  <td className="py-3 pr-4 text-gray-600">{e.municipio ?? '—'}</td>
                  <td className="py-3 pr-4 text-gray-600">{e.nombreContacto}</td>
                  <td className="py-3 pr-4">
                    <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${BADGE[e.estado]}`}>
                      {e.estado}
                    </span>
                  </td>
                  <td className="py-3">
                    <div className="flex flex-wrap gap-2">
                      {e.estado === 'PENDIENTE' && (
                        <>
                          <button onClick={() => handleAprobar(e.id)}
                            className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded hover:bg-green-200 transition-colors">
                            Aprobar
                          </button>
                          <button onClick={() => setModalRechazar({ id: e.id, motivo: '' })}
                            className="text-xs bg-red-100 text-red-700 px-2 py-1 rounded hover:bg-red-200 transition-colors">
                            Rechazar
                          </button>
                        </>
                      )}
                      {e.estado === 'APROBADA' && (
                        <>
                          <button onClick={() => { setModalClonar({ open: true, id: e.id }); setClonarForm(CLONAR_INICIAL) }}
                            className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded hover:bg-blue-200 transition-colors">
                            Clonar
                          </button>
                          <button onClick={() => handleInactivar(e.id)}
                            className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded hover:bg-gray-200 transition-colors">
                            Inactivar
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal: Nueva Empresa */}
      {modalCrear && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Nueva Empresa</h2>
            {error && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{error}</div>}
            <form onSubmit={handleCrear} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Razón Social *</label>
                  <input className="input-field" required value={form.razonSocial}
                    onChange={e => setForm({ ...form, razonSocial: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">NIT *</label>
                  <input className="input-field" required value={form.nit}
                    onChange={e => setForm({ ...form, nit: e.target.value })} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Sector</label>
                  <input className="input-field" value={form.sector}
                    onChange={e => setForm({ ...form, sector: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Municipio</label>
                  <input className="input-field" value={form.municipio}
                    onChange={e => setForm({ ...form, municipio: e.target.value })} />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Dirección</label>
                <input className="input-field" value={form.direccion}
                  onChange={e => setForm({ ...form, direccion: e.target.value })} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Nombre del Contacto *</label>
                  <input className="input-field" required value={form.nombreContacto}
                    onChange={e => setForm({ ...form, nombreContacto: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Correo</label>
                  <input className="input-field" type="email" value={form.correo}
                    onChange={e => setForm({ ...form, correo: e.target.value })} />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
                <input className="input-field" value={form.telefono}
                  onChange={e => setForm({ ...form, telefono: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Áreas Disponibles <span className="text-gray-400 font-normal">(separadas por coma)</span>
                </label>
                <input className="input-field" placeholder="Sistemas, Contabilidad, Mercadeo"
                  value={form.areas}
                  onChange={e => setForm({ ...form, areas: e.target.value })} />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" className="btn-secondary flex-1"
                  onClick={() => { setModalCrear(false); setError('') }}>Cancelar</button>
                <button type="submit" className="btn-primary flex-1">Crear</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Rechazar */}
      {modalRechazar.id > 0 && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Rechazar Empresa</h2>
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

      {/* Modal: Clonar */}
      {modalClonar.open && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h2 className="text-lg font-bold text-gray-800 mb-1">Clonar Empresa</h2>
            <p className="text-sm text-gray-500 mb-4">Se copiarán sector, dirección, municipio y áreas.</p>
            {error && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{error}</div>}
            <form onSubmit={handleClonar} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nueva Razón Social *</label>
                <input className="input-field" required value={clonarForm.razonSocial}
                  onChange={e => setClonarForm({ ...clonarForm, razonSocial: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nuevo NIT *</label>
                <input className="input-field" required value={clonarForm.nit}
                  onChange={e => setClonarForm({ ...clonarForm, nit: e.target.value })} />
              </div>
              <div className="flex gap-3">
                <button type="button" className="btn-secondary flex-1"
                  onClick={() => { setModalClonar({ open: false, id: 0 }); setError('') }}>Cancelar</button>
                <button type="submit" className="btn-primary flex-1">Clonar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
