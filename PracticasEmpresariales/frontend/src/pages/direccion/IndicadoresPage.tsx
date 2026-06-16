import { useEffect, useState } from 'react'
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  ResponsiveContainer,
} from 'recharts'
import { sprint4Service } from '../../services/sprint4Service'
import type { TableroGerencialResponse } from '../../types'

// ── Paleta de colores ─────────────────────────────────────────────────────────
const COLORS = ['#1a365d', '#2b6cb0', '#3182ce', '#4299e1', '#63b3ed', '#90cdf4', '#bee3f8']

// ── Gauge semicircular SVG puro ───────────────────────────────────────────────
function SemiGauge({ pct, color }: { pct: number; color: string }) {
  const R      = 80
  const stroke = 16
  const cx     = 100
  const cy     = 100
  const r      = R - stroke / 2
  // Semicírculo: de 180° a 0° (sentido horario por la izquierda)
  const angle  = Math.PI * (1 - Math.min(pct, 100) / 100)
  const ex     = cx + r * Math.cos(angle)   // extremo del arco
  const ey     = cy - r * Math.sin(angle)
  const large  = pct > 50 ? 1 : 0

  // Arco de fondo
  const bgD = `M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`
  // Arco relleno
  const fgD = pct <= 0
    ? ''
    : pct >= 100
      ? `M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`
      : `M ${cx - r} ${cy} A ${r} ${r} 0 ${large} 1 ${ex} ${ey}`

  return (
    <div className="flex flex-col items-center">
      <svg width="200" height="110" viewBox="0 0 200 110">
        {/* Track de fondo */}
        <path d={bgD} fill="none" stroke="#e2e8f0" strokeWidth={stroke} strokeLinecap="round" />
        {/* Arco de progreso */}
        {fgD && (
          <path
            d={fgD}
            fill="none"
            stroke={color}
            strokeWidth={stroke}
            strokeLinecap="round"
            style={{ transition: 'all 1.2s ease-in-out' }}
          />
        )}
        {/* Etiqueta central */}
        <text x="100" y="88" textAnchor="middle" fontSize="28" fontWeight="800" fill="#1a202c">
          {pct}
        </text>
        <text x="100" y="104" textAnchor="middle" fontSize="12" fill="#718096">
          %
        </text>
      </svg>
    </div>
  )
}

// ── Tooltip personalizado para el donut ───────────────────────────────────────
function DonutTooltip({ active, payload }: { active?: boolean; payload?: { name: string; value: number }[] }) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white border border-gray-200 rounded-xl shadow-lg px-4 py-2.5 text-sm">
      <p className="font-semibold text-gray-800">{payload[0].name}</p>
      <p className="text-cue-primary font-bold text-base mt-0.5">{payload[0].value} practicantes</p>
    </div>
  )
}

// ── Tooltip para barra ────────────────────────────────────────────────────────
function BarTooltip({ active, payload, label }: { active?: boolean; payload?: { value: number }[]; label?: string }) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white border border-gray-200 rounded-xl shadow-lg px-4 py-2.5 text-sm max-w-xs">
      <p className="font-medium text-gray-600 text-xs leading-snug mb-1">{label}</p>
      <p className="text-cue-primary font-bold text-base">{payload[0].value} practicantes</p>
    </div>
  )
}


// ── Componente principal ──────────────────────────────────────────────────────
export default function IndicadoresPage() {
  const [tablero, setTablero] = useState<TableroGerencialResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')

  useEffect(() => {
    sprint4Service
      .tableroDireccion()
      .then(setTablero)
      .catch(() => setError('No se pudieron cargar los indicadores.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-64 gap-4">
        <div className="w-10 h-10 rounded-full border-4 border-cue-primary border-t-transparent animate-spin" />
        <p className="text-sm text-gray-400">Cargando tablero gerencial…</p>
      </div>
    )
  }

  // ── Datos ─────────────────────────────────────────────────────────────────
  const porPrograma = Object.entries(tablero?.practicantesEnCursoPorPrograma ?? {})
  const total       = porPrograma.reduce((s, [, v]) => s + v, 0)
  const tasa        = tablero?.tasaAprobacionGlobal ?? 0
  const empresas    = tablero?.empresasActivas ?? 0

  const tasaColor   = tasa >= 80 ? '#059669' : tasa >= 60 ? '#d97706' : '#dc2626'
  const tasaVariant = tasa >= 80 ? { bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200', dot: 'bg-emerald-500', label: 'Rendimiento óptimo' }
                    : tasa >= 60 ? { bg: 'bg-amber-50',   text: 'text-amber-700',   border: 'border-amber-200',   dot: 'bg-amber-500',   label: 'Requiere atención'  }
                    :              { bg: 'bg-red-50',      text: 'text-red-700',     border: 'border-red-200',     dot: 'bg-red-500',     label: 'Estado crítico'     }

  // Datos para donut
  const pieData = porPrograma
    .sort(([, a], [, b]) => b - a)
    .map(([name, value]) => ({ name, value }))

  // Datos para donut de aprobación
  const aprobData = [
    { name: 'Aprobados', value: tasa },
    { name: 'No aprobados', value: 100 - tasa },
  ]

  // Datos para barra horizontal
  const barData = [...porPrograma]
    .sort(([, a], [, b]) => b - a)
    .map(([name, value]) => ({
      name: name.length > 28 ? name.slice(0, 26) + '…' : name,
      fullName: name,
      value,
    }))

  const fechaHoy = new Date().toLocaleDateString('es-CO', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
  })

  return (
    <div className="space-y-6 max-w-6xl">

      {/* ── Encabezado ─────────────────────────────────────────────────────── */}
      <div className="flex items-start justify-between flex-wrap gap-4">
        <div>
          <div className="flex items-center gap-2.5 mb-1">
            <div className="w-1 h-7 rounded-full bg-cue-primary" />
            <h1 className="text-2xl font-bold text-gray-900">Tablero Gerencial</h1>
          </div>
          <p className="text-sm text-gray-500 ml-3.5">Indicadores ejecutivos · Dirección · Solo lectura</p>
        </div>
        <div className="text-right shrink-0">
          <p className="text-[10px] font-bold uppercase tracking-widest text-gray-400">Período actual</p>
          <p className="text-sm font-medium text-gray-700 capitalize mt-0.5">{fechaHoy}</p>
        </div>
      </div>

      {/* ── Error ──────────────────────────────────────────────────────────── */}
      {error && (
        <div className="flex items-center gap-3 bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm">
          <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          {error}
        </div>
      )}

      {/* ── Fila 1: KPIs resumen ────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">

        {/* KPI — Empresas activas */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-blue-400 to-blue-600" />
          <div className="p-5">
            <p className="text-[10px] font-bold uppercase tracking-widest text-gray-400">Empresas aliadas activas</p>
            <p className="text-5xl font-extrabold text-gray-900 mt-3 leading-none tabular-nums">{empresas}</p>
            <p className="text-sm text-gray-400 mt-2">vinculadas a prácticas en curso</p>
          </div>
        </div>

        {/* KPI — Total practicantes */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-teal-400 to-emerald-500" />
          <div className="p-5">
            <p className="text-[10px] font-bold uppercase tracking-widest text-gray-400">Practicantes en curso</p>
            <p className="text-5xl font-extrabold text-gray-900 mt-3 leading-none tabular-nums">{total}</p>
            <p className="text-sm text-gray-400 mt-2">
              distribuidos en {porPrograma.length} programa{porPrograma.length !== 1 ? 's' : ''}
            </p>
          </div>
        </div>

        {/* KPI — Estado tasa de aprobación */}
        <div className={`rounded-2xl border shadow-sm overflow-hidden ${tasaVariant.bg} ${tasaVariant.border}`}>
          <div className="h-1" style={{ backgroundColor: tasaColor }} />
          <div className="p-5">
            <p className="text-[10px] font-bold uppercase tracking-widest" style={{ color: tasaColor }}>
              Tasa de aprobación global
            </p>
            <p className="text-5xl font-extrabold mt-3 leading-none tabular-nums" style={{ color: tasaColor }}>
              {tasa}<span className="text-2xl font-semibold">%</span>
            </p>
            <span className={`inline-flex items-center gap-1.5 mt-3 text-xs font-semibold px-2.5 py-1 rounded-full border ${tasaVariant.text} ${tasaVariant.border} bg-white/60`}>
              <span className={`w-1.5 h-1.5 rounded-full ${tasaVariant.dot}`} />
              {tasaVariant.label}
            </span>
          </div>
        </div>
      </div>

      {/* ── Fila 2: Gauge aprobación + Donut distribución ───────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">

        {/* Gauge semicircular — Tasa de aprobación */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-cue-primary to-cue-accent" />
          <div className="p-6">
            <h2 className="text-sm font-semibold text-gray-800 mb-1">Tasa de aprobación global</h2>
            <p className="text-xs text-gray-400 mb-4">Porcentaje de prácticas finalizadas con resultado aprobado</p>

            <SemiGauge pct={tasa} color={tasaColor} />

            {/* Leyenda aprobados / no aprobados */}
            <div className="flex justify-center gap-8 mt-2">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-emerald-500" />
                <span className="text-xs text-gray-600">Aprobados <strong>{tasa}%</strong></span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-gray-200" />
                <span className="text-xs text-gray-600">No aprobados <strong>{100 - tasa}%</strong></span>
              </div>
            </div>

            {/* Mini donut pie de aprobación */}
            <div className="mt-4">
              <ResponsiveContainer width="100%" height={160}>
                <PieChart>
                  <Pie
                    data={aprobData}
                    cx="50%"
                    cy="50%"
                    innerRadius={48}
                    outerRadius={68}
                    startAngle={90}
                    endAngle={-270}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    <Cell fill={tasaColor} />
                    <Cell fill="#e2e8f0" />
                  </Pie>
                  <Tooltip
                    formatter={(val) => [`${val}%`, '']}
                    contentStyle={{ borderRadius: '12px', border: '1px solid #e5e7eb', fontSize: '13px' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* Donut — Distribución por programa */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-indigo-400 to-blue-500" />
          <div className="p-6">
            <h2 className="text-sm font-semibold text-gray-800 mb-1">Distribución por programa académico</h2>
            <p className="text-xs text-gray-400 mb-4">Practicantes EN_CURSO agrupados por programa — {total} total</p>

            {pieData.length === 0 ? (
              <div className="flex items-center justify-center h-48 text-gray-300 text-sm">
                Sin datos disponibles
              </div>
            ) : (
              <div className="relative">
                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={65}
                      outerRadius={95}
                      paddingAngle={3}
                      dataKey="value"
                    >
                      {pieData.map((_, i) => (
                        <Cell key={i} fill={COLORS[i % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<DonutTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
                {/* Etiqueta central del donut */}
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                  <div className="text-center">
                    <p className="text-3xl font-extrabold text-gray-900 leading-none">{total}</p>
                    <p className="text-xs text-gray-400 mt-1">practicantes</p>
                  </div>
                </div>
              </div>
            )}
            {/* Leyenda externa (no superpuesta) */}
            {pieData.length > 0 && (
              <div className="mt-3 grid grid-cols-1 gap-1.5">
                {pieData.map((entry, i) => (
                  <div key={i} className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="w-2.5 h-2.5 rounded-sm shrink-0" style={{ backgroundColor: COLORS[i % COLORS.length] }} />
                      <span className="text-gray-600 truncate">{entry.name}</span>
                    </div>
                    <span className="font-bold text-gray-800 ml-3 shrink-0">{entry.value}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Fila 3: Gráfica de barras comparativa ──────────────────────────── */}
      {barData.length > 0 && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-cue-primary via-cue-accent to-blue-400" />
          <div className="p-6">
            <div className="flex items-start justify-between mb-5 flex-wrap gap-3">
              <div>
                <h2 className="text-sm font-semibold text-gray-800">Comparativa de practicantes por programa</h2>
                <p className="text-xs text-gray-400 mt-0.5">Conteo absoluto en estado EN_CURSO por programa académico</p>
              </div>
              <span className="bg-cue-light text-cue-primary text-xs font-bold px-3 py-1.5 rounded-full border border-cue-accent/20 shrink-0">
                {total} practicantes
              </span>
            </div>

            <ResponsiveContainer width="100%" height={Math.max(barData.length * 52, 160)}>
              <BarChart
                data={barData}
                layout="vertical"
                margin={{ top: 0, right: 48, left: 8, bottom: 0 }}
              >
                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f1f5f9" />
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
                  width={190}
                  tick={{ fontSize: 11, fill: '#475569' }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip content={<BarTooltip />} cursor={{ fill: '#f8fafc' }} />
                <Bar dataKey="value" radius={[0, 6, 6, 0]} maxBarSize={28} label={{ position: 'right', fontSize: 11, fill: '#64748b', fontWeight: 600 }}>
                  {barData.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* ── Footer ─────────────────────────────────────────────────────────── */}
      <div className="flex items-center gap-3 bg-slate-50 border border-slate-200 rounded-xl px-5 py-3.5">
        <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse shrink-0" />
        <p className="text-sm text-slate-600">
          <span className="font-semibold text-slate-800">Datos en tiempo real.</span>
          {' '}Indicadores agregados de toda la institución. Acceso exclusivo para Dirección.
        </p>
      </div>

    </div>
  )
}
