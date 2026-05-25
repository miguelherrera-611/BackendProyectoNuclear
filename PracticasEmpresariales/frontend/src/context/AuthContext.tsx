import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import { AuthUser } from '../types'
import { authService } from '../services/authService'

/**
 * PATRON SINGLETON + MEDIATOR — Frontend
 *
 * AuthContext es el único punto de verdad de autenticación en la app.
 * Actúa como Mediator entre el sistema de login y todos los componentes
 * que necesitan saber qué usuario está autenticado y con qué rol.
 *
 * React garantiza una única instancia del contexto en el árbol de componentes.
 */

interface AuthContextType {
  user: AuthUser | null
  loading: boolean
  login: (correo: string, password: string) => Promise<void>
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })
  const [loading, setLoading] = useState(false)

  const login = useCallback(async (correo: string, password: string) => {
    setLoading(true)
    try {
      const data = await authService.login({ correo, password })
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data))
      setUser(data)
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
