import { EncuestasAsignadasPanel } from '../../components/encuestas/EncuestasAsignadasPanel'

export default function EncuestasPage() {
  return (
    <EncuestasAsignadasPanel
      titulo="Encuestas asignadas"
      descripcion="Encuestas de satisfaccion pendientes o completadas."
      permiteBorradorTutor
    />
  )
}
