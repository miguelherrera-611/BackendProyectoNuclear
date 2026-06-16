import { useState, FormEvent, useRef, useEffect, KeyboardEvent, ClipboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

type Paso = 'credenciales' | 'verificacion'

export default function LoginPage() {
  const { iniciarLogin, verificarCodigo, loading } = useAuth()
  const navigate = useNavigate()

  const [paso, setPaso] = useState<Paso>('credenciales')
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [correoConfirmado, setCorreoConfirmado] = useState('')
  const [digitos, setDigitos] = useState<string[]>(['', '', '', '', '', ''])
  const [error, setError] = useState('')
  const [tiempoRestante, setTiempoRestante] = useState(600)
  const [contadorIniciado, setContadorIniciado] = useState(false)
  const inputsRef = useRef<(HTMLInputElement | null)[]>([])
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Limpia el intervalo al desmontar el componente
  useEffect(() => {
    return () => { if (intervalRef.current) clearInterval(intervalRef.current) }
  }, [])

  const iniciarContador = (segundos: number) => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    const total = segundos > 0 ? segundos : 600
    setTiempoRestante(total)
    setContadorIniciado(true)
    intervalRef.current = setInterval(() => {
      setTiempoRestante(prev => {
        if (prev <= 1) {
          clearInterval(intervalRef.current!)
          intervalRef.current = null
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  const formatTiempo = (s: number) => {
    const m = Math.floor(s / 60)
    const seg = s % 60
    return `${m}:${seg.toString().padStart(2, '0')}`
  }

  const handleCredenciales = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const res = await iniciarLogin(correo, password)
      setCorreoConfirmado(res.correo)
      setDigitos(['', '', '', '', '', ''])
      iniciarContador(res.expiresInSeconds)
      setPaso('verificacion')
      setTimeout(() => inputsRef.current[0]?.focus(), 100)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })
        ?.response?.data?.mensaje
      setError(msg ?? 'Credenciales incorrectas o cuenta inactiva.')
    }
  }

  const handleDigitoChange = (index: number, value: string) => {
    if (!/^\d?$/.test(value)) return
    const nuevo = [...digitos]
    nuevo[index] = value
    setDigitos(nuevo)
    if (value && index < 5) {
      inputsRef.current[index + 1]?.focus()
    }
  }

  const handleDigitoKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digitos[index] && index > 0) {
      inputsRef.current[index - 1]?.focus()
    }
    if (e.key === 'ArrowLeft' && index > 0) inputsRef.current[index - 1]?.focus()
    if (e.key === 'ArrowRight' && index < 5) inputsRef.current[index + 1]?.focus()
  }

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault()
    const texto = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (!texto) return
    const nuevo = [...digitos]
    for (let i = 0; i < 6; i++) nuevo[i] = texto[i] ?? ''
    setDigitos(nuevo)
    const siguienteVacio = Math.min(texto.length, 5)
    inputsRef.current[siguienteVacio]?.focus()
  }

  const handleVerificacion = async (e: FormEvent) => {
    e.preventDefault()
    const codigo = digitos.join('')
    if (codigo.length < 6) {
      setError('Ingresa los 6 dígitos del código.')
      return
    }
    setError('')
    try {
      await verificarCodigo(correoConfirmado, codigo)
      navigate('/dashboard', { replace: true })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })
        ?.response?.data?.mensaje
      setError(msg ?? 'Código incorrecto o expirado.')
      setDigitos(['', '', '', '', '', ''])
      setTimeout(() => inputsRef.current[0]?.focus(), 50)
    }
  }

  const volverACredenciales = () => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    setContadorIniciado(false)
    setPaso('credenciales')
    setError('')
    setDigitos(['', '', '', '', '', ''])
  }

  const reenviarCodigo = async () => {
    setError('')
    try {
      const res = await iniciarLogin(correo, password)
      setDigitos(['', '', '', '', '', ''])
      iniciarContador(res.expiresInSeconds)
      setTimeout(() => inputsRef.current[0]?.focus(), 100)
    } catch {
      setError('No se pudo reenviar el código. Vuelve a iniciar sesión.')
    }
  }

  const codigoExpirado = contadorIniciado && tiempoRestante === 0

  return (
    <div className="min-h-screen bg-gradient-to-br from-cue-primary via-cue-primary to-cue-secondary flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl w-full max-w-md overflow-hidden border-2 border-white/20 shadow-[0_8px_40px_rgba(0,0,0,0.55)]">

        {/* Header */}
        <div className="bg-cue-primary px-6 py-7 sm:px-8 sm:py-8 text-center">
          <h1 className="text-xl sm:text-2xl font-bold text-white">Sistema de Prácticas</h1>
          <p className="text-blue-300 text-xs sm:text-sm mt-1">Universidad Alexander Von Humboldt</p>
        </div>

        <div className="px-6 py-7 sm:px-8 sm:py-8">

          {/* ── PASO 1: Credenciales ── */}
          {paso === 'credenciales' && (
            <>
              <h2 className="text-lg sm:text-xl font-semibold text-gray-800 mb-5 sm:mb-6">
                Iniciar sesión
              </h2>

              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 mb-4 text-sm">
                  {error}
                </div>
              )}

              <form onSubmit={handleCredenciales} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Correo electrónico
                  </label>
                  <input
                    type="email"
                    value={correo}
                    onChange={e => setCorreo(e.target.value)}
                    className="input-field"
                    placeholder="usuario@cue.edu.co"
                    required
                    autoComplete="email"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Contraseña
                  </label>
                  <input
                    type="password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    className="input-field"
                    placeholder="••••••••"
                    required
                    autoComplete="current-password"
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full btn-primary py-3 flex items-center justify-center text-sm sm:text-base"
                >
                  {loading
                    ? <><span className="animate-spin mr-2 text-lg">⟳</span>Verificando...</>
                    : 'Ingresar'}
                </button>
              </form>

              <p className="text-xs text-gray-400 mt-6 text-center">
                Si no recuerdas tu contraseña, contacta al Administrador DTI.
              </p>
            </>
          )}

          {/* ── PASO 2: Verificación 2FA ── */}
          {paso === 'verificacion' && (
            <>
              {/* Ícono de correo */}
              <div className="flex justify-center mb-4">
                <div className="w-14 h-14 sm:w-16 sm:h-16 bg-blue-50 rounded-full flex items-center justify-center">
                  <svg className="w-7 h-7 sm:w-8 sm:h-8 text-cue-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8}
                      d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>
                </div>
              </div>

              <h2 className="text-lg sm:text-xl font-semibold text-gray-800 text-center mb-2">
                Verificación en dos pasos
              </h2>
              <p className="text-sm text-gray-500 text-center mb-5 sm:mb-6 px-2">
                Ingresa el código de 6 dígitos que enviamos a{' '}
                <span className="font-medium text-gray-700 break-all">{correoConfirmado}</span>
              </p>

              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 mb-4 text-sm text-center">
                  {error}
                </div>
              )}

              <form onSubmit={handleVerificacion}>
                {/* Cajas de dígitos */}
                <div className="flex justify-center gap-2 sm:gap-3 mb-5 sm:mb-6">
                  {digitos.map((d, i) => (
                    <input
                      key={i}
                      ref={el => { inputsRef.current[i] = el }}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={d}
                      onChange={e => handleDigitoChange(i, e.target.value)}
                      onKeyDown={e => handleDigitoKeyDown(i, e)}
                      onPaste={i === 0 ? handlePaste : undefined}
                      onFocus={e => e.target.select()}
                      className={`
                        w-10 h-12 sm:w-12 sm:h-14 text-center text-xl sm:text-2xl font-bold
                        border-2 rounded-lg outline-none transition-colors
                        ${d
                          ? 'border-cue-primary bg-blue-50 text-cue-primary'
                          : 'border-gray-300 bg-white text-gray-900'}
                        focus:border-cue-primary focus:ring-2 focus:ring-blue-200
                      `}
                    />
                  ))}
                </div>

                <button
                  type="submit"
                  disabled={loading || digitos.join('').length < 6 || codigoExpirado}
                  className="w-full btn-primary py-3 flex items-center justify-center text-sm sm:text-base disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading
                    ? <><span className="animate-spin mr-2 text-lg">⟳</span>Verificando...</>
                    : 'Confirmar acceso'}
                </button>
              </form>

              {/* Contador y reenvío */}
              <div className="mt-4 text-center">
                {contadorIniciado && (
                  codigoExpirado
                    ? <p className="text-sm text-red-500 font-medium">El código ha expirado.</p>
                    : <p className="text-sm text-gray-500">
                        El código expira en{' '}
                        <span className="font-semibold text-gray-700">{formatTiempo(tiempoRestante)}</span>
                      </p>
                )}

                <div className="mt-3 flex flex-col sm:flex-row items-center justify-center gap-2 sm:gap-4">
                  <button
                    type="button"
                    onClick={reenviarCodigo}
                    disabled={loading}
                    className="text-sm text-cue-primary hover:underline font-medium disabled:opacity-50"
                  >
                    Reenviar código
                  </button>
                  <span className="hidden sm:inline text-gray-300">|</span>
                  <button
                    type="button"
                    onClick={volverACredenciales}
                    className="text-sm text-gray-500 hover:underline"
                  >
                    ← Volver al inicio de sesión
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
