import { useState, useEffect } from 'react'
import { UsuarioResponse, Rol, EtiquetaCargo } from '../../types'
import { usuarioService } from '../../services/usuarioService'
import { ROL_LABELS } from '../../constants/roles'

const ROL_OPTIONS: Rol[] = [
  'ADMIN_DTI', 'COORDINACION_ACADEMICA', 'COORDINADOR_PRACTICAS',
  'DOCENTE_ASESOR', 'TUTOR_EMPRESARIAL', 'ESTUDIANTE', 'DIRECCION',
]

const ESTADO_BADGE = (activo: boolean) =>
  activo
    ? <span className="badge-apto">Activo</span>
    : <span className="badge-no-apto">Inactivo</span>

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<UsuarioResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [modalAbierto, setModalAbierto] = useState(false)
  const [form, setForm] = useState({
    nombre: '', correo: '', rol: '' as Rol,
    etiquetaCargo: '' as EtiquetaCargo | '',
    telefono: '', facultadId: '', programaId: '',
  })
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')

  const cargar = () => {
    setLoading(true)
    usuarioService.listar().then(p => setUsuarios(p.content)).finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await usuarioService.crear({
        nombre: form.nombre,
        correo: form.correo,
        rol: form.rol,
        etiquetaCargo: form.etiquetaCargo || undefined,
        telefono: form.telefono || undefined,
        facultadId: form.facultadId ? Number(form.facultadId) : undefined,
        programaId: form.programaId ? Number(form.programaId) : undefined,
      })
      setExito('Usuario creado. Se envió la contraseña temporal al correo.')
      setModalAbierto(false)
      setForm({ nombre: '', correo: '', rol: '' as Rol, etiquetaCargo: '', telefono: '', facultadId: '', programaId: '' })
      cargar()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setError(msg ?? 'Error al crear el usuario.')
    }
  }

  const handleToggle = async (u: UsuarioResponse) => {
    try {
      if (u.activo) await usuarioService.desactivar(u.id)
      else await usuarioService.activar(u.id)
      cargar()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      alert(msg ?? 'Error al cambiar estado.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Gestión de Usuarios</h1>
        <button className="btn-primary" onClick={() => setModalAbierto(true)}>
          + Crear Usuario
        </button>
      </div>

      {exito && (
        <div className="bg-green-50 border border-green-200 text-green-700 rounded-lg px-4 py-3 text-sm">
          {exito}
        </div>
      )}

      {/* Tabla de usuarios */}
      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {['Nombre', 'Correo', 'Rol', 'Cargo', 'Estado', 'Acciones'].map(h => (
                <th key={h} className="text-left px-4 py-3 text-gray-600 font-semibold">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="text-center py-8 text-gray-400">Cargando...</td></tr>
            ) : usuarios.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-8 text-gray-400">No hay usuarios registrados.</td></tr>
            ) : usuarios.map(u => (
              <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="px-4 py-3 font-medium">{u.nombre}</td>
                <td className="px-4 py-3 text-gray-500">{u.correo}</td>
                <td className="px-4 py-3"><span className="badge-rol">{ROL_LABELS[u.rol]}</span></td>
                <td className="px-4 py-3 text-gray-500 text-xs">{u.etiquetaCargo ?? '—'}</td>
                <td className="px-4 py-3">{ESTADO_BADGE(u.activo)}</td>
                <td className="px-4 py-3">
                  <button
                    onClick={() => handleToggle(u)}
                    className={`text-xs px-3 py-1 rounded-full border transition-colors ${
                      u.activo
                        ? 'border-red-300 text-red-600 hover:bg-red-50'
                        : 'border-green-300 text-green-600 hover:bg-green-50'
                    }`}
                  >
                    {u.activo ? 'Desactivar' : 'Activar'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal crear usuario */}
      {modalAbierto && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-lg font-bold text-gray-800">Crear nuevo usuario</h2>
            </div>
            <form onSubmit={handleCrear} className="p-6 space-y-4">
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">{error}</div>
              )}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Nombre completo *</label>
                  <input className="input-field" required value={form.nombre}
                    onChange={e => setForm({ ...form, nombre: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Correo electrónico *</label>
                  <input className="input-field" type="email" required value={form.correo}
                    onChange={e => setForm({ ...form, correo: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Rol *</label>
                  <select className="input-field" required value={form.rol}
                    onChange={e => setForm({ ...form, rol: e.target.value as Rol, etiquetaCargo: '' })}>
                    <option value="">Seleccionar...</option>
                    {ROL_OPTIONS.map(r => <option key={r} value={r}>{ROL_LABELS[r]}</option>)}
                  </select>
                </div>
                {form.rol === 'COORDINACION_ACADEMICA' && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Etiqueta de cargo *</label>
                    <select className="input-field" required value={form.etiquetaCargo}
                      onChange={e => setForm({ ...form, etiquetaCargo: e.target.value as EtiquetaCargo })}>
                      <option value="">Seleccionar...</option>
                      <option value="COORDINACION_ACADEMICA">Coordinación Académica</option>
                      <option value="SECRETARIA">Secretaría</option>
                    </select>
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
                  <input className="input-field" value={form.telefono}
                    onChange={e => setForm({ ...form, telefono: e.target.value })} />
                </div>
                {['COORDINACION_ACADEMICA'].includes(form.rol) && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">ID Facultad</label>
                    <input className="input-field" type="number" value={form.facultadId}
                      onChange={e => setForm({ ...form, facultadId: e.target.value })} />
                  </div>
                )}
                {['COORDINADOR_PRACTICAS', 'ESTUDIANTE'].includes(form.rol) && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">ID Programa</label>
                    <input className="input-field" type="number" value={form.programaId}
                      onChange={e => setForm({ ...form, programaId: e.target.value })} />
                  </div>
                )}
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" className="btn-secondary flex-1" onClick={() => setModalAbierto(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary flex-1">
                  Crear y enviar contraseña
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
