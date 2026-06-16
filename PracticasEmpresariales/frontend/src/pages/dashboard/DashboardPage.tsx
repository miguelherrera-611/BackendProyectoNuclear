import { useEffect, useState } from 'react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Cell,
  ResponsiveContainer,
} from 'recharts'
import { useAuth } from '../../context/AuthContext'
import { dashboardService } from '../../services/dashboardService'
import type { DashboardResponse, DashboardSeccion } from '../../types'
import { useNavigate } from 'react-router-dom'

// ── Colores por posición ──────────────────────────────────────────────────────
const BAR_COLORS = [
  '#1a365d', '#2b6cb0', '#3182ce', '#4299e1',
  '#059669', '#d97706', '#7c3aed', '#db2777',
]

// ── Mapeo de icono + acento por sección ───────────────────────────────────────
type SeccionMeta = {
  icon:      React.ReactNode
  accent:    string
  iconBg:    string
  iconColor: string
  valColor:  string
}

const IcoSVG = {
  users: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>,
  check: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>,
  alert: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>,
  brief: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>,
  chart: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>,
  file:  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>,
  cal:   <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>,
  clip:  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/></svg>,
  flag:  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>,
  build: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>,
  trend: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>,
  circ:  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/></svg>,
}

function getSeccionMeta(id: string): SeccionMeta {
  if (id.startsWith('usuarios'))        return { icon: IcoSVG.users, accent: 'bg-indigo-500',  iconBg: 'bg-indigo-50',   iconColor: 'text-indigo-500',  valColor: 'text-indigo-700'  }
  if (id.includes('no-apto'))           return { icon: IcoSVG.alert, accent: 'bg-rose-500',    iconBg: 'bg-rose-50',     iconColor: 'text-rose-500',    valColor: 'text-rose-700'    }
  if (id.startsWith('estudiantes-ap') || id === 'estudiantes-aptos')
                                        return { icon: IcoSVG.check, accent: 'bg-emerald-500', iconBg: 'bg-emerald-50',  iconColor: 'text-emerald-500', valColor: 'text-emerald-700' }
  if (id === 'vacantes')                return { icon: IcoSVG.brief, accent: 'bg-amber-500',   iconBg: 'bg-amber-50',    iconColor: 'text-amber-500',   valColor: 'text-amber-700'   }
  if (id === 'practicas-activas')       return { icon: IcoSVG.trend, accent: 'bg-blue-500',    iconBg: 'bg-blue-50',     iconColor: 'text-blue-500',    valColor: 'text-blue-700'    }
  if (id === 'planes')                  return { icon: IcoSVG.file,  accent: 'bg-violet-500',  iconBg: 'bg-violet-50',   iconColor: 'text-violet-500',  valColor: 'text-violet-700'  }
  if (id === 'cierres')                 return { icon: IcoSVG.flag,  accent: 'bg-orange-500',  iconBg: 'bg-orange-50',   iconColor: 'text-orange-500',  valColor: 'text-orange-700'  }
  if (id.includes('practicante'))       return { icon: IcoSVG.users, accent: 'bg-teal-500',    iconBg: 'bg-teal-50',     iconColor: 'text-teal-500',    valColor: 'text-teal-700'    }
  if (id.includes('seguimiento'))       return { icon: IcoSVG.cal,   accent: 'bg-cyan-500',    iconBg: 'bg-cyan-50',     iconColor: 'text-cyan-500',    valColor: 'text-cyan-700'    }
  if (id.includes('sustentacion'))      return { icon: IcoSVG.clip,  accent: 'bg-purple-500',  iconBg: 'bg-purple-50',   iconColor: 'text-purple-500',  valColor: 'text-purple-700'  }
  if (id.includes('encuesta'))          return { icon: IcoSVG.clip,  accent: 'bg-pink-500',    iconBg: 'bg-pink-50',     iconColor: 'text-pink-500',    valColor: 'text-pink-700'    }
  if (id.includes('document'))          return { icon: IcoSVG.file,  accent: 'bg-slate-500',   iconBg: 'bg-slate-50',    iconColor: 'text-slate-500',   valColor: 'text-slate-700'   }
  if (id === 'mi-practica')             return { icon: IcoSVG.trend, accent: 'bg-blue-500',    iconBg: 'bg-blue-50',     iconColor: 'text-blue-500',    valColor: 'text-blue-700'    }
  if (id === 'indicadores')             return { icon: IcoSVG.chart, accent: 'bg-cue-primary', iconBg: 'bg-cue-light',   iconColor: 'text-cue-primary', valColor: 'text-cue-primary' }
  if (id === 'reportes')                return { icon: IcoSVG.trend, accent: 'bg-cue-secondary',iconBg:'bg-blue-50',     iconColor: 'text-cue-secondary',valColor:'text-cue-secondary'}
  if (id.startsWith('programa-'))       return { icon: IcoSVG.build, accent: 'bg-indigo-400',  iconBg: 'bg-indigo-50',   iconColor: 'text-indigo-500',  valColor: 'text-indigo-700'  }
  return                                         { icon: IcoSVG.circ, accent: 'bg-gray-400',    iconBg: 'bg-gray-50',     iconColor: 'text-gray-400',    valColor: 'text-gray-700'    }
}

// ── Tooltip personalizado del BarChart ────────────────────────────────────────
function ChartTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean
  payload?: { value: number }[]
  label?: string
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white border border-gray-200 rounded-xl shadow-lg px-4 py-2.5 text-sm max-w-xs">
      <p className="text-gray-500 text-xs leading-snug mb-1">{label}</p>
      <p className="font-bold text-gray-900 text-base tabular-nums">{payload[0].value}</p>
    </div>
  )
}

// ── Tarjeta de sección ────────────────────────────────────────────────────────
function SeccionCard({
  seccion,
  maxContador,
  onClick,
  soloLectura,
}: {
  seccion: DashboardSeccion
  maxContador: number
  onClick?: () => void
  soloLectura: boolean
}) {
  const meta    = getSeccionMeta(seccion.id)
  const urgente = seccion.contador > 0
  const fillPct = maxContador > 0 ? (seccion.contador / maxContador) * 100 : 0

  const inner = (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden h-full">
      <div className={`h-1 ${meta.accent}`} />
      <div className="p-5">
        {/* Icono + alerta */}
        <div className="flex items-start justify-between mb-4">
          <div className={`w-9 h-9 rounded-xl ${meta.iconBg} flex items-center justify-center ${meta.iconColor} shrink-0`}>
            <span className="w-4 h-4 [&>svg]:w-4 [&>svg]:h-4">{meta.icon}</span>
          </div>
          {urgente && !soloLectura && (
            <span className="text-[10px] font-bold uppercase tracking-wide bg-red-50 text-red-600 border border-red-200 px-2 py-0.5 rounded-full">
              Atención
            </span>
          )}
        </div>

        {/* Título */}
        <p className="text-sm font-medium text-gray-600 leading-snug line-clamp-2 mb-3 min-h-[2.5rem]">
          {seccion.titulo}
        </p>

        {/* Contador */}
        <p className={`text-3xl font-extrabold leading-none tabular-nums ${urgente ? 'text-red-600' : meta.valColor}`}>
          {seccion.contador}
        </p>

        {/* Barra de proporción relativa */}
        <div className="mt-4 h-1 bg-gray-100 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-700 ${meta.accent}`}
            style={{ width: `${fillPct}%` }}
          />
        </div>
      </div>
    </div>
  )

  if (soloLectura || !onClick) return <div className="h-full">{inner}</div>

  return (
    <button
      onClick={onClick}
      className="w-full text-left h-full group hover:scale-[1.01] active:scale-[0.99] transition-transform duration-150 focus:outline-none focus:ring-2 focus:ring-cue-accent focus:ring-offset-1 rounded-2xl"
    >
      {inner}
    </button>
  )
}

// ── Skeleton ──────────────────────────────────────────────────────────────────
function Skeleton() {
  return (
    <div className="space-y-6">
      <div className="h-7 bg-gray-100 rounded-lg w-56 animate-pulse" />
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 animate-pulse">
        <div className="h-4 bg-gray-100 rounded w-32 mb-5" />
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-6 bg-gray-100 rounded-full" style={{ width: `${80 - i * 15}%` }} />
          ))}
        </div>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden animate-pulse">
            <div className="h-1 bg-gray-200" />
            <div className="p-5 space-y-4">
              <div className="w-9 h-9 rounded-xl bg-gray-100" />
              <div className="h-3 bg-gray-100 rounded w-3/4" />
              <div className="h-8 bg-gray-100 rounded w-1/3" />
              <div className="h-1 bg-gray-100 rounded" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ── Componente principal ──────────────────────────────────────────────────────
export default function DashboardPage() {
  const { user }  = useAuth()
  const navigate  = useNavigate()
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState<string | null>(null)

  const fetchDashboard = async () => {
    try {
      const data = await dashboardService.obtener()
      setDashboard(data)
    } catch {
      setError('No se pudo cargar el panel. Verifica que el servidor esté activo.')
    } finally {
      setLoading(false)
    }
  }

  const recargar = () => { setError(null); setLoading(true); fetchDashboard() }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { fetchDashboard() }, [])

  if (loading) return <Skeleton />

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-center max-w-sm">
          <div className="w-14 h-14 rounded-full bg-red-50 border border-red-100 flex items-center justify-center mx-auto mb-4">
            <svg className="w-7 h-7 text-red-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <p className="text-gray-700 font-medium mb-1">Error al cargar el panel</p>
          <p className="text-sm text-gray-400 mb-5">{error}</p>
          <button onClick={recargar} className="btn-primary">Reintentar</button>
        </div>
      </div>
    )
  }

  const soloLectura  = dashboard?.soloLectura ?? false
  const secciones    = dashboard?.secciones ?? []
  const maxContador  = Math.max(...secciones.map(s => s.contador), 1)

  // Datos para el BarChart resumen (excluye secciones con contador 0 si hay muchas)
  const barData = secciones.map((s, i) => ({
    name:  s.titulo.length > 30 ? s.titulo.slice(0, 28) + '…' : s.titulo,
    full:  s.titulo,
    valor: s.contador,
    ruta:  s.ruta,
    color: BAR_COLORS[i % BAR_COLORS.length],
  }))

  const chartHeight = Math.max(barData.length * 46, 120)

  return (
    <div className="space-y-6">

      {/* ── Encabezado ─────────────────────────────────────────────────────── */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {dashboard?.titulo ?? 'Panel de inicio'}
          </h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Bienvenido/a, <span className="font-semibold text-gray-700">{user?.nombre}</span>
          </p>
        </div>
        {soloLectura && (
          <div className="flex items-center gap-2 bg-amber-50 border border-amber-200 text-amber-800 text-sm font-medium px-4 py-2 rounded-full shrink-0">
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
            </svg>
            Vista gerencial — Solo lectura
          </div>
        )}
      </div>

      {/* ── Gráfica de barras resumen ───────────────────────────────────────── */}
      {secciones.length > 0 && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-cue-primary via-cue-accent to-blue-400" />
          <div className="p-6">
            <div className="flex items-start justify-between mb-5 flex-wrap gap-2">
              <div>
                <h2 className="text-sm font-semibold text-gray-800">Resumen de indicadores</h2>
                <p className="text-xs text-gray-400 mt-0.5">
                  {soloLectura
                    ? 'Vista ejecutiva consolidada — solo lectura'
                    : 'Haz clic en una barra para ir directamente a esa sección'}
                </p>
              </div>
              <span className="text-xs font-semibold bg-cue-light text-cue-primary px-3 py-1.5 rounded-full border border-cue-accent/20 shrink-0">
                {secciones.length} secciones
              </span>
            </div>

            <ResponsiveContainer width="100%" height={chartHeight}>
              <BarChart
                data={barData}
                layout="vertical"
                margin={{ top: 0, right: 56, left: 8, bottom: 0 }}
                barCategoryGap="30%"
              >
                <CartesianGrid
                  strokeDasharray="3 3"
                  horizontal={false}
                  stroke="#f1f5f9"
                />
                <XAxis
                  type="number"
                  tick={{ fontSize: 11, fill: '#94a3b8' }}
                  axisLine={false}
                  tickLine={false}
                  allowDecimals={false}
                />
                <YAxis
                  dataKey="name"
                  type="category"
                  width={200}
                  tick={{ fontSize: 11, fill: '#475569' }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip content={<ChartTooltip />} cursor={{ fill: '#f8fafc', radius: 6 }} />
                <Bar
                  dataKey="valor"
                  radius={[0, 6, 6, 0]}
                  maxBarSize={24}
                  label={{
                    position: 'right',
                    fontSize: 11,
                    fill: '#64748b',
                    fontWeight: 700,
                  }}
                  onClick={(d) => {
                    if (!soloLectura) navigate((d as unknown as typeof barData[0]).ruta)
                  }}
                  style={{ cursor: soloLectura ? 'default' : 'pointer' }}
                >
                  {barData.map((entry, i) => (
                    <Cell key={i} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* ── Grid de tarjetas ───────────────────────────────────────────────── */}
      {secciones.length === 0 ? (
        <div className="text-center py-16 text-gray-400 text-sm">
          No hay secciones disponibles para tu rol.
        </div>
      ) : (
        <>
          <p className="text-xs font-semibold uppercase tracking-widest text-gray-400">
            {soloLectura ? 'Detalle por indicador' : 'Acceso rápido'}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {secciones.map(seccion => (
              <SeccionCard
                key={seccion.id}
                seccion={seccion}
                maxContador={maxContador}
                soloLectura={soloLectura}
                onClick={soloLectura ? undefined : () => navigate(seccion.ruta)}
              />
            ))}
          </div>
        </>
      )}

      {/* ── Nota Dirección ─────────────────────────────────────────────────── */}
      {soloLectura && (
        <p className="text-xs text-gray-400 text-center pt-1">
          Los indicadores estadísticos detallados están en{' '}
          <strong className="text-gray-500">Tablero Gerencial</strong> en el menú lateral.
        </p>
      )}

    </div>
  )
}
