import { useAuth } from '../../../context/AuthContext'
import { ROL_LABELS } from '../../../constants/roles'

export default function Navbar() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between shadow-sm">
      <div />
      <div className="flex items-center gap-4">
        {user.rol === 'DIRECCION' && (
          <span className="text-xs bg-amber-100 text-amber-800 px-3 py-1 rounded-full font-medium">
            Solo lectura
          </span>
        )}
        <div className="text-right">
          <p className="text-sm font-medium text-gray-800">{user.nombre}</p>
          <p className="text-xs text-gray-500">{ROL_LABELS[user.rol]}</p>
        </div>
        <div className="w-9 h-9 rounded-full bg-cue-primary text-white flex items-center justify-center text-sm font-bold">
          {user.nombre.charAt(0).toUpperCase()}
        </div>
      </div>
    </header>
  )
}
