import { useState, useEffect } from 'react'
import { FacultadResponse } from '../../types'
import api from '../../services/api'
import { ApiResponse, Pageable } from '../../types'

export default function FacultadesPage() {
  const [facultades, setFacultades] = useState<FacultadResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [form, setForm] = useState({ nombre: '', descripcion: '' })
  const [error, setError] = useState('')

  const cargar = () => {
    setLoading(true)
    api.get<ApiResponse<Pageable<FacultadResponse>>>('/facultades')
      .then(r => setFacultades(r.data.datos?.content ?? []))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await api.post('/facultades', form)
      setModal(false)
      setForm({ nombre: '', descripcion: '' })
      cargar()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setError(msg ?? 'Error al crear la facultad.')
    }
  }

  const handleDesactivar = async (id: number) => {
    if (!confirm('¿Desactivar esta facultad?')) return
    try {
      await api.patch(`/facultades/${id}/desactivar`)
      cargar()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      alert(msg ?? 'No se puede desactivar.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Facultades</h1>
        <button className="btn-primary" onClick={() => setModal(true)}>+ Nueva Facultad</button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {loading ? (
          <p className="text-gray-400 col-span-3">Cargando...</p>
        ) : facultades.length === 0 ? (
          <div className="col-span-3 card text-center text-gray-400 py-12">
            No hay facultades registradas. Crea la primera.
          </div>
        ) : facultades.map(f => (
          <div key={f.id} className="card">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold text-gray-800">{f.nombre}</h3>
                {f.descripcion && <p className="text-xs text-gray-500 mt-1">{f.descripcion}</p>}
              </div>
              <span className={f.activa ? 'badge-apto' : 'badge-no-apto'}>
                {f.activa ? 'Activa' : 'Inactiva'}
              </span>
            </div>
            <p className="text-sm text-gray-600 mt-3">
              <span className="font-medium">{f.numeroProgramas}</span> programas
            </p>
            {f.activa && (
              <button
                onClick={() => handleDesactivar(f.id)}
                className="mt-3 text-xs text-red-500 hover:text-red-700 transition-colors"
              >
                Desactivar facultad
              </button>
            )}
          </div>
        ))}
      </div>

      {modal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Nueva Facultad</h2>
            {error && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{error}</div>}
            <form onSubmit={handleCrear} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre *</label>
                <input className="input-field" required value={form.nombre}
                  onChange={e => setForm({ ...form, nombre: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
                <textarea className="input-field" rows={3} value={form.descripcion}
                  onChange={e => setForm({ ...form, descripcion: e.target.value })} />
              </div>
              <div className="flex gap-3">
                <button type="button" className="btn-secondary flex-1" onClick={() => setModal(false)}>Cancelar</button>
                <button type="submit" className="btn-primary flex-1">Crear</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
