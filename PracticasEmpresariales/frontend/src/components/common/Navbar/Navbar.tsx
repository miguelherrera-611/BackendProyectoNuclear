import { useState, useRef, useEffect, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import { ROL_LABELS } from '../../../constants/roles'
import { Modal } from '../Modal/Modal'
import { authService } from '../../../services/authService'

type EmailStep = 'closed' | 'solicitar' | 'confirmar'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handler(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  // ── Cambiar contraseña ────────────────────────────────────────────────────
  const [showPwdModal, setShowPwdModal] = useState(false)
  const [pwd, setPwd] = useState({ actual: '', nueva: '', confirmacion: '' })
  const [pwdLoading, setPwdLoading] = useState(false)
  const [pwdError, setPwdError] = useState('')

  function resetPwdModal() {
    setShowPwdModal(false)
    setPwd({ actual: '', nueva: '', confirmacion: '' })
    setPwdError('')
    setPwdLoading(false)
  }

  async function handleCambiarPassword(e: FormEvent) {
    e.preventDefault()
    if (pwd.nueva !== pwd.confirmacion) {
      setPwdError('Las contraseñas nuevas no coinciden.')
      return
    }
    setPwdLoading(true)
    setPwdError('')
    try {
      await authService.cambiarPassword({
        passwordActual: pwd.actual,
        passwordNueva: pwd.nueva,
        passwordConfirmacion: pwd.confirmacion,
      })
      resetPwdModal()
      logout()
      navigate('/login', { replace: true })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setPwdError(msg ?? 'Error al cambiar la contraseña.')
    } finally {
      setPwdLoading(false)
    }
  }

  // ── Cambiar correo ────────────────────────────────────────────────────────
  const [emailStep, setEmailStep] = useState<EmailStep>('closed')
  const [emailLoading, setEmailLoading] = useState(false)
  const [emailError, setEmailError] = useState('')
  const [codigo, setCodigo] = useState('')
  const [nuevoCorreo, setNuevoCorreo] = useState('')

  function resetEmailModal() {
    setEmailStep('closed')
    setEmailLoading(false)
    setEmailError('')
    setCodigo('')
    setNuevoCorreo('')
  }

  async function handleSolicitarCodigo() {
    setEmailLoading(true)
    setEmailError('')
    try {
      await authService.solicitarCambioCorreo()
      setEmailStep('confirmar')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setEmailError(msg ?? 'No se pudo enviar el código. Inténtalo de nuevo.')
    } finally {
      setEmailLoading(false)
    }
  }

  async function handleConfirmarCambioCorreo(e: FormEvent) {
    e.preventDefault()
    setEmailLoading(true)
    setEmailError('')
    try {
      await authService.confirmarCambioCorreo({ codigo, nuevoCorreo })
      resetEmailModal()
      logout()
      navigate('/login', { replace: true })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setEmailError(msg ?? 'Error al confirmar el cambio de correo.')
    } finally {
      setEmailLoading(false)
    }
  }

  if (!user) return null

  return (
    <>
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

          <div className="relative" ref={menuRef}>
            <button
              onClick={() => setMenuOpen(o => !o)}
              className="w-9 h-9 rounded-full bg-cue-primary text-white flex items-center justify-center text-sm font-bold hover:opacity-90 transition-opacity focus:outline-none focus:ring-2 focus:ring-cue-primary focus:ring-offset-2"
              aria-label="Menú de perfil"
              aria-expanded={menuOpen}
            >
              {user.nombre.charAt(0).toUpperCase()}
            </button>

            {menuOpen && (
              <div className="absolute right-0 mt-2 w-60 bg-white rounded-xl shadow-lg border border-gray-100 z-50 overflow-hidden">
                <div className="px-4 py-3 border-b border-gray-100 bg-gray-50">
                  <p className="text-sm font-semibold text-gray-800 truncate">{user.nombre}</p>
                  <p className="text-xs text-gray-400 truncate mt-0.5">{user.correo}</p>
                </div>
                <div className="py-1">
                  <button
                    onClick={() => { setMenuOpen(false); setShowPwdModal(true) }}
                    className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                  >
                    <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                    </svg>
                    Cambiar contraseña
                  </button>
                  <button
                    onClick={() => { setMenuOpen(false); setEmailStep('solicitar') }}
                    className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                  >
                    <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                    </svg>
                    Cambiar correo electrónico
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* ── Modal: Cambiar contraseña ─────────────────────────────────────── */}
      {showPwdModal && (
        <Modal title="Cambiar contraseña" onClose={resetPwdModal} size="sm">
          <form onSubmit={handleCambiarPassword} className="space-y-4">
            {pwdError && (
              <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">
                {pwdError}
              </div>
            )}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña actual</label>
              <input
                type="password"
                value={pwd.actual}
                onChange={e => setPwd(p => ({ ...p, actual: e.target.value }))}
                className="input-field"
                required
                autoFocus
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nueva contraseña</label>
              <input
                type="password"
                value={pwd.nueva}
                onChange={e => setPwd(p => ({ ...p, nueva: e.target.value }))}
                className="input-field"
                required
                minLength={8}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Confirmar nueva contraseña</label>
              <input
                type="password"
                value={pwd.confirmacion}
                onChange={e => setPwd(p => ({ ...p, confirmacion: e.target.value }))}
                className="input-field"
                required
                minLength={8}
              />
            </div>
            <p className="text-xs text-gray-400">Al cambiar la contraseña se cerrará tu sesión.</p>
            <div className="flex gap-3 pt-1">
              <button type="button" onClick={resetPwdModal} className="flex-1 btn-secondary py-2.5">
                Cancelar
              </button>
              <button type="submit" disabled={pwdLoading} className="flex-1 btn-primary py-2.5">
                {pwdLoading ? 'Guardando...' : 'Cambiar contraseña'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── Modal: Cambiar correo ─────────────────────────────────────────── */}
      {emailStep !== 'closed' && (
        <Modal
          title="Cambiar correo electrónico"
          subtitle={emailStep === 'solicitar'
            ? 'Te enviaremos un código de verificación a tu correo actual'
            : 'Introduce el código recibido y tu nuevo correo'}
          onClose={resetEmailModal}
          size="sm"
        >
          {emailError && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">
              {emailError}
            </div>
          )}

          {emailStep === 'solicitar' && (
            <div className="space-y-4">
              <div className="bg-blue-50 border border-blue-100 rounded-lg px-4 py-3">
                <p className="text-sm text-blue-800">
                  Se enviará un código de 6 dígitos a:
                </p>
                <p className="text-sm font-semibold text-blue-900 mt-1 truncate">{user.correo}</p>
              </div>
              <p className="text-xs text-gray-400">El código expira en 10 minutos.</p>
              <div className="flex gap-3">
                <button onClick={resetEmailModal} className="flex-1 btn-secondary py-2.5">
                  Cancelar
                </button>
                <button onClick={handleSolicitarCodigo} disabled={emailLoading} className="flex-1 btn-primary py-2.5">
                  {emailLoading ? 'Enviando...' : 'Enviar código'}
                </button>
              </div>
            </div>
          )}

          {emailStep === 'confirmar' && (
            <form onSubmit={handleConfirmarCambioCorreo} className="space-y-4">
              <div className="bg-green-50 border border-green-100 rounded-lg px-4 py-3 text-sm text-green-800">
                Código enviado a <strong>{user.correo}</strong>. Revisa tu bandeja de entrada.
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Código de verificación</label>
                <input
                  type="text"
                  value={codigo}
                  onChange={e => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  className="input-field text-center text-xl tracking-widest font-mono"
                  placeholder="000000"
                  required
                  maxLength={6}
                  autoFocus
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nuevo correo electrónico</label>
                <input
                  type="email"
                  value={nuevoCorreo}
                  onChange={e => setNuevoCorreo(e.target.value)}
                  className="input-field"
                  placeholder="nuevo@correo.com"
                  required
                />
              </div>
              <p className="text-xs text-gray-400">Al confirmar el cambio se cerrará tu sesión.</p>
              <div className="flex gap-3">
                <button type="button" onClick={() => setEmailStep('solicitar')} className="flex-1 btn-secondary py-2.5">
                  Atrás
                </button>
                <button type="submit" disabled={emailLoading || codigo.length < 6} className="flex-1 btn-primary py-2.5">
                  {emailLoading ? 'Confirmando...' : 'Confirmar cambio'}
                </button>
              </div>
            </form>
          )}
        </Modal>
      )}
    </>
  )
}
