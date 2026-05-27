import { NavLink } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import { MENUS_POR_ROL } from '../../../constants/menus'
import { ROL_LABELS } from '../../../constants/roles'

/**
 * PATRON MEDIATOR — Frontend
 *
 * El Sidebar consulta el rol del usuario al AuthContext (Mediator)
 * y renderiza únicamente los ítems del menú permitidos para ese rol.
 * Ningún rol ve módulos que no le corresponden.
 */
export default function Sidebar() {
  const { user, logout } = useAuth()
  if (!user) return null

  const menuItems = MENUS_POR_ROL[user.rol] ?? []

  return (
    <aside className="w-64 bg-cue-primary text-white flex flex-col shadow-lg">
      {/* Logo / header */}
      <div className="p-6 border-b border-blue-800">
        <h1 className="text-lg font-bold leading-tight">Prácticas Empresariales</h1>
        <p className="text-blue-300 text-xs mt-1">Univ. Alexander Von Humboldt</p>
      </div>

      {/* Info usuario */}
      <div className="px-4 py-3 border-b border-blue-800">
        <p className="text-sm font-medium truncate">{user.nombre}</p>
        <span className="text-xs bg-blue-700 px-2 py-0.5 rounded-full mt-1 inline-block">
          {ROL_LABELS[user.rol]}
        </span>
        {user.etiquetaCargo && (
          <span className="text-xs text-blue-300 block mt-0.5">
            {user.etiquetaCargo === 'SECRETARIA' ? 'Secretaría' : 'Coordinación'}
          </span>
        )}
      </div>

      {/* Menú dinámico por rol */}
      <nav className="flex-1 overflow-y-auto py-4">
        {menuItems.map((item) => (
          <NavLink
            key={item.id}
            to={item.ruta}
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
