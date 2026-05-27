import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authService } from '../../services/authService'

export default function CambiarPasswordPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    passwordActual: '',
    passwordNueva: '',
    passwordConfirmacion: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (form.passwordNueva !== form.passwordConfirmacion) {
      setError('Las contraseñas nuevas no coinciden.')
      return
    }
    setLoading(true)
    setError('')
    try {
      await authService.cambiarPassword(form)
      // Recargar usuario para actualizar primerIngreso = false
      logout()
      navigate('/login', { replace: true })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })
        ?.response?.data?.mensaje
      setError(msg ?? 'Error al cambiar la contraseña.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-cue-primary flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8">
        <div className="text-center mb-6">
          <span className="text-4xl">🔐</span>
          <h2 className="text-xl font-semibold text-gray-800 mt-3">Cambio de contraseña obligatorio</h2>
          <p className="text-sm text-gray-500 mt-1">
            Hola <strong>{user?.nombre}</strong>, debes cambiar tu contraseña temporal antes de continuar.
          </p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {(['passwordActual', 'passwordNueva', 'passwordConfirmacion'] as const).map((field) => (
            <div key={field}>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {field === 'passwordActual' ? 'Contraseña temporal' :
                 field === 'passwordNueva' ? 'Nueva contraseña' : 'Confirmar nueva contraseña'}
              </label>
              <input
                type="password"
                value={form[field]}
                onChange={(e) => setForm({ ...form, [field]: e.target.value })}
                className="input-field"
                required
                minLength={field !== 'passwordActual' ? 8 : undefined}
              />
            </div>
          ))}

          <button type="submit" disabled={loading} className="w-full btn-primary py-3">
            {loading ? 'Guardando...' : 'Cambiar contraseña'}
          </button>
        </form>
      </div>
    </div>
  )
}
