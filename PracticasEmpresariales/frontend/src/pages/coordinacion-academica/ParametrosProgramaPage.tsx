import { useState, useEffect, useCallback } from 'react'
import { sprint4Service, type ConfigurarProgramaRequest } from '../../services/sprint4Service'
import { REQUISITOS_CIERRE, CONFIG_DEFAULTS } from '../../constants/configuracion'
import { Input } from '../../components/common/Input/Input'
import { Button } from '../../components/common/Button/Button'
import { useToast } from '../../components/common/Notifications/Toast'
import type { ProgramaResponse, ProgramaConfiguracionResponse, ApiResponse, Pageable } from '../../types'
import api from '../../services/api'

interface EstadoPrograma {
  programa: ProgramaResponse
  config: ProgramaConfiguracionResponse | null
  cargando: boolean
}

export default function ParametrosProgramaPage() {
  const { showToast } = useToast()

  const [estados, setEstados]       = useState<EstadoPrograma[]>([])
  const [cargandoLista, setCargandoLista] = useState(true)

  // Editor
  const [programaEditando, setProgramaEditando] = useState<ProgramaResponse | null>(null)
  const [form, setForm]       = useState<ConfigurarProgramaRequest>(CONFIG_DEFAULTS)
  const [guardando, setGuardando] = useState(false)

  const cargarTodo = useCallback(async () => {
    setCargandoLista(true)
    try {
      const res = await api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas?size=100')
      const programas = res.data.datos?.content ?? []

      // Carga las configuraciones de todos los programas en paralelo
      const resultados = await Promise.all(
        programas.map(async (p) => {
          try {
            const cfg = await sprint4Service.obtenerConfiguracionPrograma(p.id)
            return { programa: p, config: cfg, cargando: false }
          } catch {
            return { programa: p, config: null, cargando: false }
          }
        })
      )
      setEstados(resultados)
    } finally {
      setCargandoLista(false)
    }
  }, [])

  useEffect(() => { cargarTodo() }, [cargarTodo])

  const abrirEditor = (ep: EstadoPrograma) => {
    setProgramaEditando(ep.programa)
    const cfg = ep.config
    setForm(cfg
      ? {
          numeroPracticas:       cfg.numeroPracticas,
          semanasSeguimiento:    cfg.semanasSeguimiento,
          notaMinimaAprobacion:  cfg.notaMinimaAprobacion,
          requisitosCierre:      cfg.requisitosCierre ?? CONFIG_DEFAULTS.requisitosCierre,
          umbralInactividadDias: cfg.umbralInactividadDias,
        }
      : CONFIG_DEFAULTS
    )
    setTimeout(() => {
      document.getElementById('editor-parametros')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 50)
  }

  const requisitosActivos = form.requisitosCierre
    ? form.requisitosCierre.split(',').map(r => r.trim()).filter(Boolean)
    : []

  const toggleRequisito = (valor: string) => {
    const siguiente = requisitosActivos.includes(valor)
      ? requisitosActivos.filter(r => r !== valor)
      : [...requisitosActivos, valor]
    setForm({ ...form, requisitosCierre: siguiente.join(',') })
  }

  const handleGuardar = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!programaEditando) return
    setGuardando(true)
    try {
      await sprint4Service.configurarPrograma(programaEditando.id, form)
      showToast(`Parámetros de "${programaEditando.nombre}" guardados correctamente.`)
      await cargarTodo()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? 'Error al guardar los parámetros.', 'error')
    } finally {
      setGuardando(false)
    }
  }

  const configurados = estados.filter(e => e.config?.id != null).length
  const total        = estados.length

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Parámetros del Programa</h1>
        <p className="text-sm text-gray-500 mt-1">
          Configura las reglas operativas que el sistema aplica a todas las prácticas de cada programa académico.
        </p>
      </div>

      {/* ── Lista de estado por programa ─────────────────────────────────── */}
      <div className="card">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-gray-900">Estado de configuraciones</h2>
            <p className="text-xs text-gray-400 mt-0.5">
              {configurados} de {total} programas con parámetros personalizados
            </p>
          </div>
          {!cargandoLista && (
            <button
              onClick={cargarTodo}
              className="text-xs text-gray-400 hover:text-gray-600 transition-colors"
            >
              Actualizar
            </button>
          )}
        </div>

        {cargandoLista ? (
          <div className="flex justify-center py-10">
            <div className="animate-spin rounded-full h-7 w-7 border-b-2 border-cue-primary" />
          </div>
        ) : estados.length === 0 ? (
          <div className="text-center py-10 text-gray-400 text-sm">
            No hay programas registrados en el sistema.
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {estados.map(ep => {
              const configurado = ep.config?.id != null
              const esEditando  = programaEditando?.id === ep.programa.id
              return (
                <div
                  key={ep.programa.id}
                  className={`flex items-center justify-between py-3 gap-4 ${esEditando ? 'bg-blue-50 -mx-6 px-6 rounded-lg' : ''}`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    {configurado ? (
                      <span className="shrink-0 w-5 h-5 rounded-full bg-green-100 text-green-600 flex items-center justify-center text-xs font-bold">
                        ✓
                      </span>
                    ) : (
                      <span className="shrink-0 w-5 h-5 rounded-full bg-gray-100 text-gray-400 flex items-center justify-center text-xs">
                        ○
                      </span>
                    )}
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-800 truncate">{ep.programa.nombre}</p>
                      <p className="text-xs text-gray-400 truncate">{ep.programa.facultadNombre}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    {configurado ? (
                      <span className="text-xs text-green-700 font-medium bg-green-50 px-2 py-0.5 rounded-full">
                        Configurado
                      </span>
                    ) : (
                      <span className="text-xs text-amber-600 font-medium bg-amber-50 px-2 py-0.5 rounded-full">
                        Usando valores por defecto
                      </span>
                    )}
                    <button
                      onClick={() => abrirEditor(ep)}
                      className={`text-xs font-medium px-3 py-1.5 rounded-lg transition-colors ${
                        esEditando
                          ? 'bg-cue-primary text-white'
                          : 'text-cue-primary hover:underline'
                      }`}
                    >
                      {esEditando ? 'Editando...' : configurado ? 'Editar' : 'Configurar'}
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* ── Editor ───────────────────────────────────────────────────────── */}
      <div className="card" id="editor-parametros">
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-gray-900">
            {programaEditando ? `Editando: ${programaEditando.nombre}` : 'Editor de parámetros'}
          </h2>
          <p className="text-sm text-gray-500 mt-1">
            {programaEditando
              ? 'Modifica los valores y guarda para aplicarlos a todas las prácticas de este programa.'
              : 'Selecciona un programa de la lista de arriba para comenzar a editarlo.'}
          </p>
        </div>

        {!programaEditando ? (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="text-4xl mb-3 text-gray-300">⚙️</div>
            <p className="text-gray-400 text-sm">Haz clic en "Editar" o "Configurar" en cualquier programa de la lista</p>
          </div>
        ) : (
          <form onSubmit={handleGuardar} className="space-y-6">

            {/* Parámetros numéricos */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-gray-600 uppercase tracking-wider border-b border-gray-100 pb-2">
                Reglas operativas
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Número de prácticas del programa"
                  type="number"
                  min={1}
                  value={form.numeroPracticas}
                  onChange={e => setForm({ ...form, numeroPracticas: Number(e.target.value) })}
                  hint="Total de prácticas que puede cursar un estudiante."
                />
                <Input
                  label="Semanas de seguimiento"
                  type="number"
                  min={1}
                  value={form.semanasSeguimiento}
                  onChange={e => setForm({ ...form, semanasSeguimiento: Number(e.target.value) })}
                  hint="Duración del período de seguimiento por práctica."
                />
                <Input
                  label="Nota mínima de aprobación (0 – 5)"
                  type="number"
                  step="0.1"
                  min={0}
                  max={5}
                  value={form.notaMinimaAprobacion}
                  onChange={e => setForm({ ...form, notaMinimaAprobacion: Number(e.target.value) })}
                  hint="Nota mínima para que la práctica quede aprobada."
                />
                <Input
                  label="Días de inactividad permitidos"
                  type="number"
                  min={1}
                  value={form.umbralInactividadDias}
                  onChange={e => setForm({ ...form, umbralInactividadDias: Number(e.target.value) })}
                  hint="Sin reportes en este plazo → práctica marcada como inactiva."
                />
              </div>
            </div>

            {/* Requisitos de cierre */}
            <div className="space-y-3">
              <h3 className="text-sm font-semibold text-gray-600 uppercase tracking-wider border-b border-gray-100 pb-2">
                Requisitos para el cierre formal
              </h3>
              <p className="text-xs text-gray-400">
                Condiciones que deben cumplirse antes de poder cerrar oficialmente una práctica.
                El sistema bloqueará el cierre hasta que todas las marcadas estén completas.
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {REQUISITOS_CIERRE.map(req => (
                  <label
                    key={req.valor}
                    className={`flex items-center gap-3 px-3 py-3 rounded-lg border cursor-pointer transition-colors ${
                      requisitosActivos.includes(req.valor)
                        ? 'border-cue-primary bg-blue-50'
                        : 'border-gray-200 hover:border-gray-300 bg-white'
                    }`}
                  >
                    <input
                      type="checkbox"
                      className="accent-cue-primary h-4 w-4 shrink-0"
                      checked={requisitosActivos.includes(req.valor)}
                      onChange={() => toggleRequisito(req.valor)}
                    />
                    <span className="text-sm text-gray-700">{req.etiqueta}</span>
                  </label>
                ))}
              </div>
              <p className="text-xs text-gray-400">
                {requisitosActivos.length} de {REQUISITOS_CIERRE.length} requisitos activados.
              </p>
            </div>

            <div className="flex gap-3 pt-2 border-t border-gray-100">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setProgramaEditando(null)}
              >
                Cancelar
              </Button>
              <Button type="submit" loading={guardando} className="flex-1">
                Guardar parámetros de &ldquo;{programaEditando.nombre}&rdquo;
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
