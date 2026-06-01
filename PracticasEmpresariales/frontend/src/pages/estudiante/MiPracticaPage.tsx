import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2 } from '../../types'
import { seguimientoService } from '../../services/seguimientoService'

export default function MiPracticaPage() {
  const navigate = useNavigate()
  const [practica, setPractica] = useState<InstanciaPracticaResponseV2 | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    seguimientoService.miPractica()
      .then(setPractica)
      .catch(() => setError('No tienes una práctica activa registrada.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex justify-center py-16 text-gray-400">Cargando...</div>

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900">Mi práctica</h1>

      {error && (
        <div className="card border-amber-200 bg-amber-50 text-amber-800">{error}</div>
      )}

      {practica && (
        <>
          <div className="card space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold text-gray-900">{practica.nombre}</h2>
              <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                practica.estado === 'EN_CURSO' ? 'bg-green-100 text-green-800'
                : practica.estado === 'ASIGNADA_PENDIENTE_INICIO' ? 'bg-yellow-100 text-yellow-800'
                : practica.estado === 'FINALIZADA' ? 'bg-blue-100 text-blue-800'
                : 'bg-red-100 text-red-800'
              }`}>
                {practica.estado.replace(/_/g, ' ')}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3 text-sm text-gray-600">
              <div><span className="font-medium text-gray-700">Empresa:</span> {practica.razonSocialEmpresa ?? '—'}</div>
              <div><span className="font-medium text-gray-700">Docente:</span> {practica.nombreDocenteAsesor ?? '—'}</div>
              <div><span className="font-medium text-gray-700">Tutor:</span> {practica.nombreTutorEmpresarial ?? '—'}</div>
              <div><span className="font-medium text-gray-700">Duración:</span> {practica.duracionSemanas} semanas</div>
              {practica.fechaInicio && <div><span className="font-medium text-gray-700">Inicio:</span> {practica.fechaInicio}</div>}
              {practica.fechaFin && <div><span className="font-medium text-gray-700">Fin:</span> {practica.fechaFin}</div>}
            </div>

            {practica.estado === 'ASIGNADA_PENDIENTE_INICIO' && (
              <div className="border-t border-gray-100 pt-3">
                <p className="text-sm font-medium text-gray-700 mb-2">Firmas del convenio</p>
                <div className="flex gap-4 text-sm">
                  <span className={practica.firmaTutor ? 'text-green-600' : 'text-gray-400'}>
                    {practica.firmaTutor ? '✓' : '○'} Tutor
                  </span>
                  <span className={practica.firmaDocente ? 'text-green-600' : 'text-gray-400'}>
                    {practica.firmaDocente ? '✓' : '○'} Docente
                  </span>
                  <span className={practica.firmaEstudiante ? 'text-green-600' : 'text-gray-400'}>
                    {practica.firmaEstudiante ? '✓' : '○'} Estudiante
                  </span>
                </div>
              </div>
            )}
          </div>

          {practica.estado === 'EN_CURSO' && (
            <div className="grid grid-cols-2 gap-4">
              <button className="card text-left hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate(`/mi-practica/plan`)}>
                <h3 className="font-semibold text-gray-800">Plan de práctica</h3>
                <p className="text-sm text-gray-500 mt-1">Carga y gestiona tu plan de práctica para iniciar seguimientos.</p>
                <span className="mt-3 text-cue-primary text-sm font-medium">Ver plan →</span>
              </button>

              <button className="card text-left hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate(`/mi-practica/seguimiento`)}>
                <h3 className="font-semibold text-gray-800">Seguimientos semanales</h3>
                <p className="text-sm text-gray-500 mt-1">Registra tus actividades y logros semana a semana.</p>
                <span className="mt-3 text-cue-primary text-sm font-medium">Ver seguimientos →</span>
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
