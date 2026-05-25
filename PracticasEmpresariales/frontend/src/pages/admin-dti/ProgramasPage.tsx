import { useState, useEffect } from 'react'
import { ProgramaResponse } from '../../types'
import api from '../../services/api'
import { ApiResponse, Pageable } from '../../types'

export default function ProgramasPage() {
  const [programas, setProgramas] = useState<ProgramaResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [form, setForm] = useState({
    nombre: '', descripcion: '', facultadId: '',
    numeroTotalPracticas: 1, promedioMinimoGeneral: 3.0,
  })
  const [error, setError] = useState('')

  const cargar = () => {
    setLoading(true)
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas')
      .then(r => setProgramas(r.data.datos?.content ?? []))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await api.post('/programas', {
        ...form,
        facultadId: Number(form.facultadId),
      })
      setModal(false)
      setForm({ nombre: '', descripcion: '', facultadId: '', numeroTotalPracticas: 1, promedioMinimoGeneral: 3.0 })
      cargar()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setError(msg ?? 'Error al crear el programa.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Programas Académicos</h1>
        <button className="btn-primary" onClick={() => setModal(true)}>+ Nuevo Programa</button>
      </div>

      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {['Programa', 'Facultad', 'N° Prácticas', 'Promedio Mín.', 'Estado'].map(h => (
                <th key={h} className="text-left px-4 py-3 text-gray-600 font-semibold">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5} className="text-center py-8 text-gray-400">Cargando...</td></tr>
            ) : programas.length === 0 ? (
              <tr><td colSpan={5} className="text-center py-8 text-gray-400">No hay programas registrados.</td></tr>
            ) : programas.map(p => (
              <tr key={p.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="px-4 py-3 font-medium">{p.nombre}</td>
                <td className="px-4 py-3 text-gray-500">{p.facultadNombre}</td>
                <td className="px-4 py-3 text-center">{p.numeroTotalPracticas}</td>
                <td className="px-4 py-3 text-center">{p.promedioMinimoGeneral.toFixed(1)}</td>
                <td className="px-4 py-3">
                  <span className={p.activo ? 'badge-apto' : 'badge-no-apto'}>
                    {p.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Nuevo Programa</h2>
            <p className="text-xs text-gray-500 mb-4">
              Patrón Builder: el programa se construye paso a paso en el backend.
            </p>
            {error && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{error}</div>}
            <form onSubmit={handleCrear} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Nombre *</label>
                  <input className="input-field" required value={form.nombre}
                    onChange={e => setForm({ ...form, nombre: e.target.value })} />
                </div>
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
                  <textarea className="input-field" rows={2} value={form.descripcion}
                    onChange={e => setForm({ ...form, descripcion: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">ID Facultad *</label>
                  <input className="input-field" type="number" required value={form.facultadId}
                    onChange={e => setForm({ ...form, facultadId: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">N° de Prácticas *</label>
                  <input className="input-field" type="number" min={1} required value={form.numeroTotalPracticas}
                    onChange={e => setForm({ ...form, numeroTotalPracticas: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Promedio Mínimo General</label>
                  <input className="input-field" type="number" step="0.1" min={0} max={5} value={form.promedioMinimoGeneral}
                    onChange={e => setForm({ ...form, promedioMinimoGeneral: Number(e.target.value) })} />
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" className="btn-secondary flex-1" onClick={() => setModal(false)}>Cancelar</button>
                <button type="submit" className="btn-primary flex-1">Crear Programa</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
