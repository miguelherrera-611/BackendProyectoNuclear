import { createContext, useContext, useState, ReactNode } from 'react'

type ToastType = 'success' | 'error' | 'warning'

interface ToastItem {
  id: number
  message: string
  type: ToastType
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void
}

const ToastContext = createContext<ToastContextValue>({ showToast: () => {} })

export const useToast = () => useContext(ToastContext)

const STYLES: Record<ToastType, string> = {
  success: 'bg-green-600 text-white',
  error:   'bg-red-600 text-white',
  warning: 'bg-amber-500 text-white',
}

const ICONS: Record<ToastType, string> = {
  success: '✓',
  error:   '✕',
  warning: '⚠',
}

function ToastCard({ toast, onClose }: { toast: ToastItem; onClose: () => void }) {
  return (
    <div className={`flex items-start gap-3 px-4 py-3 rounded-xl shadow-lg min-w-72 max-w-sm animate-fade-in ${STYLES[toast.type]}`}>
      <span className="font-bold mt-0.5 shrink-0">{ICONS[toast.type]}</span>
      <p className="text-sm flex-1 leading-snug">{toast.message}</p>
      <button onClick={onClose} className="opacity-70 hover:opacity-100 transition-opacity shrink-0 text-lg leading-none mt-0.5">
        ×
      </button>
    </div>
  )
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const showToast = (message: string, type: ToastType = 'success') => {
    const id = Date.now()
    setToasts(prev => [...prev, { id, message, type }])
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 3500)
  }

  const remove = (id: number) => setToasts(prev => prev.filter(t => t.id !== id))

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-5 right-5 z-[200] flex flex-col gap-2">
        {toasts.map(t => (
          <ToastCard key={t.id} toast={t} onClose={() => remove(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}
