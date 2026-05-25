import { Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Sidebar from '../components/common/Sidebar/Sidebar'
import Navbar from '../components/common/Navbar/Navbar'

export default function MainLayout() {
  const { user } = useAuth()

  // Fuerza cambio de contraseña en primer ingreso
  if (user?.primerIngreso) {
    return <Navigate to="/cambiar-password" replace />
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <div className="flex flex-col flex-1 overflow-hidden">
        <Navbar />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
