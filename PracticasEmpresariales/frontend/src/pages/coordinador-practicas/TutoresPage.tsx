import { useState, useEffect } from 'react'
import { TutorEmpresarialResponse, EmpresaResponse } from '../../types'
import { tutorService } from '../../services/tutorService'
import { empresaService } from '../../services/empresaService'

const FORM_INICIAL = { nombre: '', cargo: '', correo: '', telefono: '', empresaId: 0 }

export default function TutoresPage() {
  const [tutores, setTutores] = useState<TutorEmpresarialResponse[]>([])
  const [empresas, setEmpresas] = useState<EmpresaResponse[]>([])
  const [empresaFiltro, setEmpresaFiltro] = useState<number>(0)
  const [loading, setLoading] = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [form, setForm] = useState(FORM_INICIAL)
  const [error, setError] = useState('')

  useEffect(() => {
    empresaService.listarAprobadas().then(setEmpresas)
  }, [])

  const cargar = (empresaId: number) => {
    if (!empresaId) { setTutores([]); return }
    setLoading(true)
    tutorService.listarPorEmpresa(empresaId)
      .then(setTutores)
      .finally(() => setLoading(false))
  }

  const handleFiltroEmpresa = (id: number) => {
    setEmpresaFiltro(id)
    cargar(id)
  }

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await tutorService.crear(form)
      setModalCrear(false)
      setForm(FORM_INICIAL)
      if (empresaFiltro) cargar(empresaFiltro)
    } catch (err: unknown) {
      setError((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al registrar el tutor.')
    }
  }

  const handleDesactivar = async (id: number) => {
    if (!confirm('¿Desactivar este tutor?')) return
    try {
      await tutorService.desactivar(id)
      if (empresaFiltro) cargar(empresaFiltro)
    } catch (err: unknown) {
      alert((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al desactivar.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Tutores Empresariales</h1>
        <button className="btn-primary" onClick={() => setModalCrear(true)}>+ Registrar Tutor</button>
      </div>

      {/* Filtro por empresa */}
      <div className="card">
        <label className="block text-sm font-medium text-gray-700 mb-2">Filtrar por Empresa</label>
        <select className="input-field max-w-sm" value={empresaFiltro || ''}
          onChange={e => handleFiltroEmpresa(Number(e.target.value))}>
          <option value="">Selecciona una empresa</option>
          {empresas.map(em => (
            <option key={em.id} value={em.id}>{em.razonSocial}</option>
          ))}
        </select>
      </div>

      {/* Lista de tutores */}
      {empresaFiltro === 0 ? (
        <div className="card text-center text-gray-400 py-12">
          Selecciona una empresa para ver sus tutores.
        </div>
      ) : loading ? (
        <p className="text-gray-400 text-center py-8">Cargando...</p>
      ) : tutores.length === 0 ? (
        <div className="card text-center text-gray-400 py-12">
          Esta empresa no tiene tutores registrados.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {tutores.map(t => (
            <div key={t.id} className="card">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-gray-800">{t.nombre}</h3>
                  {t.cargo && <p className="text-xs text-gray-500 mt-0.5">{t.cargo}</p>}
                </div>
                <span className={t.activo ? 'badge-apto' : 'badge-no-apto'}>
                  {t.activo ? 'Activo' : 'Inactivo'}
                </span>
              </div>
              <div className="mt-3 space-y-1">
                <p className="text-sm text-gray-600">✉ {t.correo}</p>
                {t.telefono && <p className="text-sm text-gray-600">📞 {t.telefono}</p>}
                <p className="text-xs text-gray-400 mt-2">
                  {t.disponible ? '✅ Disponible' : '🔴 No disponible'}
                </p>
              </div>
              {t.activo && (
                <button onClick={() => handleDesactivar(t.id)}
                  className="mt-3 text-xs text-red-500 hover:text-red-700 transition-colors">
                  Desactivar tutor
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Modal: Registrar Tutor */}
      {modalCrear && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Registrar Tutor Empresarial</h2>
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
                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre *</label>
                <input className="input-field" required value={form.nombre}
                  onChange={e => setForm({ ...form, nombre: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Cargo</label>
                <input className="input-field" value={form.cargo}
                  onChange={e => setForm({ ...form, cargo: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Correo *</label>
                <input className="input-field" type="email" required value={form.correo}
                  onChange={e => setForm({ ...form, correo: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
                <input className="input-field" value={form.telefono}
                  onChange={e => setForm({ ...form, telefono: e.target.value })} />
              </div>
              <div className="flex gap-3">
                <button type="button" className="btn-secondary flex-1"
                  onClick={() => { setModalCrear(false); setError('') }}>Cancelar</button>
                <button type="submit" className="btn-primary flex-1">Registrar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
