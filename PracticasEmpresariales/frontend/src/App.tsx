import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './components/common/Notifications/Toast'
import AppRouter from './routes/AppRouter'

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </ToastProvider>
  )
}
