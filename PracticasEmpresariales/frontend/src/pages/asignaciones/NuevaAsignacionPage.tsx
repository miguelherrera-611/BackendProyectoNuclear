import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { VacanteResponse, UsuarioResponse, TutorEmpresarialResponse } from '../../types'
import { vacanteService } from '../../services/vacanteService'
import { estudianteService } from '../../services/estudianteService'
import { tutorService } from '../../services/tutorService'
import { usuarioService } from '../../services/usuarioService'
import { asignacionService } from '../../services/asignacionService'

type Paso = 1 | 2 | 3

export default function NuevaAsignacionPage() {
  const navigate = useNavigate()

  const [paso, setPaso]           = useState<Paso>(1)
  const [vacantes, setVacantes]   = useState<VacanteResponse[]>([])
  const [estudiantes, setEstudiantes] = useState<UsuarioResponse[]>([])
  const [tutores, setTutores]     = useState<TutorEmpresarialResponse[]>([])
  const [docentes, setDocentes]   = useState<UsuarioResponse[]>([])

  const [vacanteId, setVacanteId]     = useState<number | null>(null)
  const [estudianteId, setEstudianteId] = useState<number | null>(null)
  const [tutorId, setTutorId]         = useState<number | null>(null)
  const [docenteId, setDocenteId]     = useState<number | null>(null)

  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const [busquedaEst, setBusquedaEst]     = useState('')
  const [filtroProgramaEst, setFiltroProgramaEst] = useState('')

  const vacanteSeleccionada = vacantes.find(v => v.id === vacanteId)

  const programasDisponibles = Array.from(
    new Map(estudiantes.filter(e => e.programaNombre).map(e => [e.programaNombre, e.programaNombre])).values()
  ).sort()

  const estudiantesFiltrados = estudiantes.filter(e => {
    const texto = busquedaEst.toLowerCase()
    const matchTexto = !texto ||
      e.nombre.toLowerCase().includes(texto) ||
      (e.identificacion ?? '').toLowerCase().includes(texto)
    const matchPrograma = !filtroProgramaEst || e.programaNombre === filtroProgramaEst
    return matchTexto && matchPrograma
  })

  useEffect(() => {
    vacanteService.listarDisponibles().then(setVacantes).catch(() => setError('Error cargando vacantes.'))
  }, [])

  useEffect(() => {
    if (paso === 2) {
      estudianteService.listar('APTO', 0, 100)
        .then(p => setEstudiantes(p.content.filter(e => e.enviadoAlProceso)))
        .catch(() => setError('Error cargando estudiantes APTOS.'))
    }
    if (paso === 3) {
      tutorService.listarTodos().then(setTutores).catch(() => setError('Error cargando tutores.'))
      usuarioService.listarDocentes().then(setDocentes).catch(() => setError('Error cargando docentes asesores.'))
    }
  }, [paso])

  const estudianteSeleccionado = estudiantes.find(e => e.id === estudianteId)

  const handleAsignar = async () => {
    if (!vacanteId || !estudianteId) { setError('Debes seleccionar una vacante y un estudiante.'); return }
    if (!docenteId) { setError('Debes asignar un docente asesor.'); return }
    if (!tutorId) { setError('Debes asignar un tutor empresarial.'); return }
    setLoading(true)
    setError('')
    try {
      await asignacionService.crear({
        vacanteId,
        estudianteId,
        tutorEmpresarialId: tutorId!,
        docenteAsesorId: docenteId!,
      })
      navigate('/asignaciones')
    } catch (e: unknown) {
      setError((e as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'No se pudo crear la asignación.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div>
        <button className="text-sm text-gray-500 hover:text-gray-700 mb-2" onClick={() => navigate('/asignaciones')}>
          ← Volver a asignaciones
        </button>
        <h1 className="text-2xl font-bold text-gray-900">Nueva asignación</h1>
        <p className="text-sm text-gray-500">Paso {paso} de 3</p>
      </div>

      <div className="flex gap-2">
        {([1, 2, 3] as Paso[]).map(p => (
          <div key={p} className={`flex-1 h-2 rounded-full ${paso >= p ? 'bg-cue-primary' : 'bg-gray-200'}`} />
        ))}
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      {paso === 1 && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">1. Selecciona la vacante disponible</h2>
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {vacantes.length === 0 && <p className="text-gray-400 text-sm">No hay vacantes disponibles.</p>}
            {vacantes.map(v => (
              <label key={v.id} className={`flex items-start gap-3 p-3 border rounded-lg cursor-pointer transition-colors ${vacanteId === v.id ? 'border-cue-primary bg-blue-50' : 'border-gray-200 hover:bg-gray-50'}`}>
                <input type="radio" name="vacante" value={v.id} checked={vacanteId === v.id} onChange={() => setVacanteId(v.id)} className="mt-1" />
                <div>
                  <div className="font-medium text-gray-900">{v.area}</div>
                  <div className="text-sm text-gray-500">{v.razonSocialEmpresa} · Cupos: {v.cuposTotales - v.cuposOcupados} disponibles</div>
                </div>
              </label>
            ))}
          </div>
          <button className="btn-primary w-full" disabled={!vacanteId} onClick={() => setPaso(2)}>Siguiente →</button>
        </div>
      )}

      {paso === 2 && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">2. Selecciona el estudiante APTO</h2>

          {/* Filtros */}
          <div className="flex gap-3 flex-wrap">
            <input
              type="text"
              placeholder="Buscar por nombre o cédula..."
              value={busquedaEst}
              onChange={e => setBusquedaEst(e.target.value)}
              className="input-field flex-1 min-w-48"
            />
            <select
              className="input-field min-w-48"
              value={filtroProgramaEst}
              onChange={e => setFiltroProgramaEst(e.target.value)}
            >
              <option value="">Todos los programas</option>
              {programasDisponibles.map(p => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>

          {/* Lista */}
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {estudiantes.length === 0 && (
              <p className="text-gray-400 text-sm">No hay estudiantes APTOS disponibles enviados al proceso.</p>
            )}
            {estudiantes.length > 0 && estudiantesFiltrados.length === 0 && (
              <p className="text-gray-400 text-sm">Ningún estudiante coincide con los filtros aplicados.</p>
            )}
            {estudiantesFiltrados.map(e => (
              <label key={e.id} className={`flex items-start gap-3 p-3 border rounded-lg cursor-pointer transition-colors ${estudianteId === e.id ? 'border-cue-primary bg-blue-50' : 'border-gray-200 hover:bg-gray-50'}`}>
                <input type="radio" name="estudiante" value={e.id} checked={estudianteId === e.id} onChange={() => setEstudianteId(e.id)} className="mt-1" />
                <div>
                  <div className="font-medium text-gray-900">{e.nombre}</div>
                  <div className="text-sm text-gray-500">
                    {e.identificacion ? `CC ${e.identificacion} · ` : ''}{e.programaNombre ?? 'Sin programa'}
                  </div>
                  <div className="text-xs text-gray-400">{e.correo}</div>
                </div>
              </label>
            ))}
          </div>

          {estudiantesFiltrados.length > 0 && (
            <p className="text-xs text-gray-400">
              Mostrando {estudiantesFiltrados.length} de {estudiantes.length} estudiante{estudiantes.length !== 1 ? 's' : ''}
            </p>
          )}

          <div className="flex gap-2">
            <button className="btn-secondary flex-1" onClick={() => setPaso(1)}>← Anterior</button>
            <button className="btn-primary flex-1" disabled={!estudianteId} onClick={() => setPaso(3)}>Siguiente →</button>
          </div>
        </div>
      )}

      {paso === 3 && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">3. Asignar docente asesor y tutor empresarial</h2>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Docente Asesor *</label>
            {docentes.length === 0
              ? <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">No hay docentes asesores activos registrados en el sistema.</p>
              : <select className="input-field" value={docenteId ?? ''} onChange={e => setDocenteId(e.target.value ? Number(e.target.value) : null)}>
                  <option value="">-- Selecciona un docente --</option>
                  {docentes.map(d => <option key={d.id} value={d.id}>{d.nombre}</option>)}
                </select>
            }
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tutor Empresarial *</label>
            {tutores.length === 0
              ? <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">No hay tutores empresariales registrados en el sistema.</p>
              : <select className="input-field" value={tutorId ?? ''} onChange={e => setTutorId(e.target.value ? Number(e.target.value) : null)}>
                  <option value="">-- Selecciona un tutor --</option>
                  {tutores.map(t => <option key={t.id} value={t.id}>{t.nombre} — {t.cargo}</option>)}
                </select>
            }
          </div>
          <div className="bg-gray-50 rounded-lg p-4 text-sm text-gray-600 space-y-1">
            <p className="font-semibold text-gray-700">Resumen:</p>
            <p>Vacante: <strong>{vacanteSeleccionada?.area}</strong> en {vacanteSeleccionada?.razonSocialEmpresa}</p>
            <p>Estudiante: <strong>{estudianteSeleccionado?.nombre ?? `ID ${estudianteId}`}</strong></p>
            {docenteId && <p>Docente: <strong>{docentes.find(d => d.id === docenteId)?.nombre}</strong></p>}
            {tutorId && <p>Tutor: <strong>{tutores.find(t => t.id === tutorId)?.nombre}</strong></p>}
          </div>
          <div className="flex gap-2">
            <button className="btn-secondary flex-1" onClick={() => setPaso(2)}>← Anterior</button>
            <button className="btn-primary flex-1" disabled={loading || !docenteId || !tutorId} onClick={handleAsignar}>
              {loading ? 'Asignando...' : 'Confirmar asignación'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
