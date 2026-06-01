import { useEffect, useState } from 'react'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'

interface Indicador {
  label: string
  valor: number
  total: number
  color: string
  descripcion: string
}

export default function IndicadoresPage() {
  const [practicas, setPracticas] = useState<InstanciaPracticaResponseV2[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')

  useEffect(() => {
    seguimientoService.tableroGeneral()
      .then(setPracticas)
      .catch(() => setError('No se pudieron cargar los indicadores.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" />
      </div>
    )
  }

  // Cálculo de indicadores
  const total       = practicas.length
  const enCurso     = practicas.filter(p => p.estado === 'EN_CURSO').length
  const conDocente  = practicas.filter(p => p.nombreDocenteAsesor).length
  const conTutor    = practicas.filter(p => p.nombreTutorEmpresarial).length
  const firmasComp  = practicas.filter(p => p.firmaTutor && p.firmaDocente && p.firmaEstudiante).length

  // Agrupación por empresa
  const porEmpresa = practicas.reduce<Record<string, number>>((acc, p) => {
    const k = p.razonSocialEmpresa ?? 'Sin empresa'
    acc[k] = (acc[k] ?? 0) + 1
    return acc
  }, {})

  // Agrupación por docente
  const porDocente = practicas.reduce<Record<string, number>>((acc, p) => {
    const k = p.nombreDocenteAsesor ?? 'Sin asignar'
    acc[k] = (acc[k] ?? 0) + 1
    return acc
  }, {})

  const topEmpresas = Object.entries(porEmpresa).sort(([, a], [, b]) => b - a).slice(0, 5)
  const topDocentes = Object.entries(porDocente).sort(([, a], [, b]) => b - a).slice(0, 5)
  const maxEmpresa  = topEmpresas[0]?.[1] ?? 1
  const maxDocente  = topDocentes[0]?.[1] ?? 1

  const indicadores: Indicador[] = [
    {
      label:       'Prácticas en curso',
      valor:       enCurso,
      total,
      color:       'bg-cue-primary',
      descripcion: 'Prácticas activas EN_CURSO',
    },
    {
      label:       'Con docente asesor',
      valor:       conDocente,
      total,
      color:       'bg-cue-secondary',
      descripcion: 'Prácticas con docente asignado',
    },
    {
      label:       'Con tutor empresarial',
      valor:       conTutor,
      total,
      color:       'bg-cue-accent',
      descripcion: 'Prácticas con tutor asignado',
    },
    {
      label:       'Firmas completas',
      valor:       firmasComp,
      total,
      color:       'bg-green-600',
      descripcion: 'Convenios con las 3 firmas',
    },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Indicadores</h1>
        <p className="text-sm text-gray-500 mt-1">
          Vista gerencial — solo lectura. Datos al {new Date().toLocaleDateString('es-CO')}.
        </p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">{error}</div>
      )}

      {/* KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {indicadores.map(ind => {
          const pct = total > 0 ? Math.round((ind.valor / ind.total) * 100) : 0
          return (
            <div key={ind.label} className="card space-y-3">
              <p className="text-sm font-medium text-gray-600">{ind.label}</p>
              <div className="flex items-end justify-between">
                <span className="text-3xl font-bold text-gray-900">{ind.valor}</span>
                <span className="text-sm text-gray-500 mb-1">de {ind.total}</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-2">
                <div
                  className={`h-2 rounded-full ${ind.color} transition-all duration-700`}
                  style={{ width: `${pct}%` }}
                />
              </div>
              <p className="text-xs text-gray-400">{pct}% — {ind.descripcion}</p>
            </div>
          )
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top empresas */}
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">Prácticas por empresa</h2>
          {topEmpresas.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-6">Sin datos</p>
          ) : topEmpresas.map(([empresa, cant]) => (
            <div key={empresa} className="space-y-1">
              <div className="flex justify-between text-sm">
                <span className="text-gray-700 truncate mr-2">{empresa}</span>
                <span className="font-semibold text-gray-900 shrink-0">{cant}</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-2">
                <div
                  className="h-2 rounded-full bg-cue-primary transition-all duration-700"
                  style={{ width: `${(cant / maxEmpresa) * 100}%` }}
                />
              </div>
            </div>
          ))}
        </div>

        {/* Top docentes */}
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">Prácticas por docente asesor</h2>
          {topDocentes.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-6">Sin datos</p>
          ) : topDocentes.map(([docente, cant]) => (
            <div key={docente} className="space-y-1">
              <div className="flex justify-between text-sm">
                <span className="text-gray-700 truncate mr-2">{docente}</span>
                <span className="font-semibold text-gray-900 shrink-0">{cant}</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-2">
                <div
                  className="h-2 rounded-full bg-cue-secondary transition-all duration-700"
                  style={{ width: `${(cant / maxDocente) * 100}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Tabla detalle */}
      <div className="card overflow-x-auto p-0">
        <div className="px-6 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-800">Detalle de prácticas activas</h2>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {['Práctica', 'Empresa', 'Docente', 'Tutor', 'Firmas'].map(h => (
                <th key={h} className="text-left px-4 py-3 text-gray-600 font-semibold">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {practicas.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center py-10 text-gray-400">No hay prácticas EN_CURSO.</td>
              </tr>
            ) : practicas.map(p => (
              <tr key={p.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="px-4 py-3 font-medium text-gray-900">{p.nombre}</td>
                <td className="px-4 py-3 text-gray-600">{p.razonSocialEmpresa ?? '—'}</td>
                <td className="px-4 py-3 text-gray-600">{p.nombreDocenteAsesor ?? <span className="text-amber-500 text-xs">Sin asignar</span>}</td>
                <td className="px-4 py-3 text-gray-600">{p.nombreTutorEmpresarial ?? <span className="text-amber-500 text-xs">Sin asignar</span>}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-1.5 text-xs">
                    <span className={p.firmaTutor    ? 'text-green-600 font-semibold' : 'text-gray-300'}>T</span>
                    <span className={p.firmaDocente  ? 'text-green-600 font-semibold' : 'text-gray-300'}>D</span>
                    <span className={p.firmaEstudiante ? 'text-green-600 font-semibold' : 'text-gray-300'}>E</span>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
