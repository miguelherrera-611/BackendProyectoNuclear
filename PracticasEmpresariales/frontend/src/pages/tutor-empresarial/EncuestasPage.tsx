export default function EncuestasPage() {
  const encuestasMock = [
    {
      id: 1,
      titulo: 'Evaluación de desempeño — Primer corte',
      practica: 'Práctica Empresarial I',
      estudiante: 'Juan García López',
      plazo: '2026-06-30',
      completada: false,
    },
    {
      id: 2,
      titulo: 'Evaluación de desempeño — Segundo corte',
      practica: 'Práctica Empresarial I',
      estudiante: 'Juan García López',
      plazo: '2026-08-15',
      completada: false,
    },
  ]

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Encuestas de Evaluación</h1>
        <p className="text-sm text-gray-500 mt-1">
          Evalúa el desempeño de tus practicantes en cada corte.
        </p>
      </div>

      {/* Banner informativo */}
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3">
        <span className="text-amber-500 text-xl mt-0.5">🔔</span>
        <div className="text-sm text-amber-800">
          <p className="font-semibold mb-0.5">Módulo en desarrollo</p>
          <p>
            Las encuestas de evaluación estarán disponibles próximamente.
            Recibirás una notificación cuando debas completar cada evaluación.
          </p>
        </div>
      </div>

      {/* Lista de encuestas (mock) */}
      <div className="space-y-3">
        {encuestasMock.map(enc => (
          <div key={enc.id} className="card flex items-center justify-between gap-4 opacity-60">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 bg-gray-100 rounded-xl flex items-center justify-center text-xl">
                📊
              </div>
              <div>
                <p className="font-medium text-gray-800">{enc.titulo}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                  {enc.estudiante} · Vence: {enc.plazo}
                </p>
              </div>
            </div>
            <span className="text-xs bg-amber-100 text-amber-700 px-3 py-1 rounded-full font-medium whitespace-nowrap">
              Próximamente
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
