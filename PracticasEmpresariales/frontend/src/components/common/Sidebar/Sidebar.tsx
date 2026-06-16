import { NavLink } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import { MENUS_POR_ROL } from '../../../constants/menus'

interface SidebarProps {
  isOpen: boolean
  onClose: () => void
}

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { user, logout } = useAuth()
  if (!user) return null

  const menuItems = MENUS_POR_ROL[user.rol] ?? []

  return (
    <aside
      className={`
        fixed top-0 left-0 h-full z-40 w-64 bg-cue-primary text-white flex flex-col shadow-lg
        transition-transform duration-300 ease-in-out
        lg:static lg:translate-x-0 lg:z-auto lg:shrink-0
        ${isOpen ? 'translate-x-0' : '-translate-x-full'}
      `}
    >
      {/* Logo / header */}
      <div className="p-6 border-b border-blue-800 flex items-start justify-between">
        <div>
          <h1 className="text-lg font-bold leading-tight">Prácticas Empresariales</h1>
          <p className="text-blue-300 text-xs mt-1">Univ. Alexander Von Humboldt</p>
        </div>
        {/* Botón cerrar solo visible en móvil */}
        <button
          onClick={onClose}
          className="lg:hidden text-blue-300 hover:text-white transition-colors text-xl leading-none mt-0.5 ml-2 shrink-0"
          aria-label="Cerrar menú"
        >
          ✕
        </button>
      </div>

      {/* Info usuario */}
      <div className="px-4 py-3 border-b border-blue-800">
        <p className="text-sm font-medium truncate">{user.nombre}</p>
        <p className="text-xs text-blue-300 mt-0.5 truncate">{user.correo}</p>
      </div>

      {/* Menú dinámico por rol */}
      <nav className="flex-1 overflow-y-auto py-4">
        {menuItems.map((item) => (
          <NavLink
            key={item.id}
            to={item.ruta}
            onClick={onClose}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 text-sm transition-colors ${
                isActive
                  ? 'bg-cue-secondary text-white font-medium'
                  : 'text-blue-200 hover:bg-blue-800 hover:text-white'
              }`
            }
          >
            <span>{item.icono}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Logout */}
      <div className="p-4 border-t border-blue-800">
        <button
          onClick={logout}
          className="w-full text-sm text-blue-300 hover:text-white transition-colors text-left flex items-center gap-2"
        >
          <span>🚪</span> Cerrar sesión
        </button>
      </div>
    </aside>
  )
}
