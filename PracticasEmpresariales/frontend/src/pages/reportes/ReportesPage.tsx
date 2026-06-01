export default function ReportesPage() {
  const reportes = [
    { id: 1, titulo: 'Reporte de prácticas activas', descripcion: 'Listado completo de prácticas EN_CURSO con información de empresa, docente y estudiante.', icono: '📊', tipo: 'Excel / PDF' },
    { id: 2, titulo: 'Reporte de asignaciones por período', descripcion: 'Historial de asignaciones realizadas en un rango de fechas específico.', icono: '📅', tipo: 'Excel' },
    { id: 3, titulo: 'Reporte de seguimientos', descripcion: 'Estado de los seguimientos semanales de todas las prácticas activas.', icono: '📝', tipo: 'PDF' },
    { id: 4, titulo: 'Reporte de empresas participantes', descripcion: 'Empresas registradas y aprobadas con sus vacantes y cupos.', icono: '🏢', tipo: 'Excel' },
    { id: 5, titulo: 'Reporte de rendimiento académico', descripcion: 'Promedio de calificaciones y estados de los estudiantes por programa.', icono: '🎓', tipo: 'PDF' },
    { id: 6, titulo: 'Bitácora de auditoría', descripcion: 'Registro completo de acciones realizadas en el sistema por todos los usuarios.', icono: '🔍', tipo: 'PDF' },
  ]

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Reportes</h1>
        <p className="text-sm text-gray-500 mt-1">Generación de reportes gerenciales del sistema de prácticas.</p>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3">
        <span className="text-amber-500 text-xl mt-0.5">🔔</span>
        <div className="text-sm text-amber-800">
          <p className="font-semibold mb-0.5">Módulo en desarrollo</p>
          <p>La generación automática de reportes estará disponible próximamente.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {reportes.map(rep => (
          <div key={rep.id} className="card opacity-70 hover:opacity-90 transition-opacity">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 bg-cue-light rounded-xl flex items-center justify-center text-2xl shrink-0">{rep.icono}</div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap mb-1">
                  <h3 className="font-semibold text-gray-800">{rep.titulo}</h3>
                  <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">{rep.tipo}</span>
                </div>
                <p className="text-sm text-gray-500">{rep.descripcion}</p>
                <button disabled className="mt-3 text-sm text-cue-primary font-medium opacity-50 cursor-not-allowed">
                  Generar reporte →
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
