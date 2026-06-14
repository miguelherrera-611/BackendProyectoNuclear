import { useState, useEffect } from 'react'
import { TutorEmpresarialResponse, EmpresaResponse } from '../../types'
import { tutorService } from '../../services/tutorService'
import { empresaService } from '../../services/empresaService'
import { Modal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { useToast } from '../../components/common/Notifications/Toast'

export default function TutoresPage() {
  const { showToast } = useToast()
  const [tutores, setTutores]           = useState<TutorEmpresarialResponse[]>([])
  const [empresas, setEmpresas]         = useState<EmpresaResponse[]>([])
  const [empresaFiltro, setEmpresaFiltro] = useState<number>(0)
  const [loading, setLoading]           = useState(false)
  const [saving, setSaving]             = useState(false)
  const [modalTelefono, setModalTelefono] = useState<{
    open: boolean; tutorId: number; nombre: string; telefonoActual: string
  }>({ open: false, tutorId: 0, nombre: '', telefonoActual: '' })
  const [telefono, setTelefono]         = useState('')
  const [errorModal, setErrorModal]     = useState('')

  useEffect(() => { empresaService.listarAprobadas().then(setEmpresas) }, [])

  const cargar = (empresaId: number) => {
    if (!empresaId) { setTutores([]); return }
    setLoading(true)
    tutorService.listarPorEmpresa(empresaId).then(setTutores).finally(() => setLoading(false))
  }

  const abrirModalTelefono = (t: TutorEmpresarialResponse) => {
    setTelefono(t.telefono ?? '')
    setErrorModal('')
    setModalTelefono({ open: true, tutorId: t.id, nombre: t.nombre, telefonoActual: t.telefono ?? '' })
  }

  const cerrarModalTelefono = () => {
    setModalTelefono({ open: false, tutorId: 0, nombre: '', telefonoActual: '' })
    setTelefono('')
    setErrorModal('')
  }

  const handleActualizarTelefono = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      const actualizado = await tutorService.actualizarTelefono(modalTelefono.tutorId, telefono)
      setTutores(prev => prev.map(t => t.id === actualizado.id ? actualizado : t))
      cerrarModalTelefono()
      showToast('Teléfono actualizado correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al actualizar el teléfono.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Tutores Empresariales</h1>
          <p className="text-sm text-gray-500 mt-0.5">Consulta de información. Solo se permite actualizar el teléfono de contacto.</p>
        </div>
      </div>

      <div className="card py-4 flex items-end gap-4">
        <div className="flex-1 max-w-sm">
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar por empresa</label>
          <select
            className="input-field"
            value={empresaFiltro || ''}
            onChange={e => { const id = Number(e.target.value); setEmpresaFiltro(id); cargar(id) }}
          >
            <option value="">Selecciona una empresa</option>
            {empresas.map(em => <option key={em.id} value={em.id}>{em.razonSocial}</option>)}
          </select>
        </div>
        {empresaFiltro > 0 && (
          <span className="text-sm text-gray-500 pb-2">
            <strong>{tutores.length}</strong> tutor{tutores.length !== 1 ? 'es' : ''}
          </span>
        )}
      </div>

      {empresaFiltro === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">👨‍💼</div>
          <p className="text-gray-400 text-sm">Selecciona una empresa para ver sus tutores.</p>
        </div>
      ) : loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
        </div>
      ) : tutores.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">👤</div>
          <p className="text-gray-400 text-sm">Esta empresa no tiene tutores registrados.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {tutores.map(t => (
            <div key={t.id} className="card hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-800 truncate">{t.nombre}</h3>
                  {t.cargo && <p className="text-xs text-gray-500 mt-0.5">{t.cargo}</p>}
                  <p className="text-xs text-cue-primary font-medium mt-0.5 truncate">🏢 {t.razonSocialEmpresa}</p>
                </div>
                <span className={t.activo ? 'badge-apto ml-2 shrink-0' : 'badge-no-apto ml-2 shrink-0'}>
                  {t.activo ? 'Activo' : 'Inactivo'}
                </span>
              </div>
              <div className="mt-3 space-y-1.5">
                <p className="text-sm text-gray-600">✉ {t.correo}</p>
                <div className="flex items-center justify-between">
                  <p className="text-sm text-gray-600">
                    {t.telefono ? `📞 ${t.telefono}` : <span className="text-gray-400 italic text-xs">Sin teléfono registrado</span>}
                  </p>
                  <button
                    onClick={() => abrirModalTelefono(t)}
                    className="text-xs text-cue-primary hover:underline transition-colors ml-2 shrink-0"
                  >
                    {t.telefono ? 'Editar' : 'Agregar'}
                  </button>
                </div>
                <p className={`text-xs mt-1 font-medium ${t.disponible ? 'text-green-600' : 'text-amber-600'}`}>
                  {t.disponible ? '● Disponible' : '● No disponible'}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalTelefono.open && (
        <Modal
          title="Actualizar teléfono"
          subtitle={modalTelefono.nombre}
          size="sm"
          onClose={cerrarModalTelefono}
        >
          {errorModal && (
            <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>
          )}
          <form onSubmit={handleActualizarTelefono} className="space-y-4">
            <Input
              label="Teléfono de contacto"
              value={telefono}
              required
              onChange={e => setTelefono(e.target.value)}
              placeholder="Ej. +57 300 123 4567"
            />
            <div className="flex gap-3 pt-1">
              <Button variant="secondary" className="flex-1" type="button" onClick={cerrarModalTelefono}>
                Cancelar
              </Button>
              <Button className="flex-1" type="submit" loading={saving}>
                Guardar
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
