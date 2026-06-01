import { useState, useEffect } from 'react'
import { TutorEmpresarialResponse, EmpresaResponse } from '../../types'
import { tutorService } from '../../services/tutorService'
import { empresaService } from '../../services/empresaService'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { useToast } from '../../components/common/Notifications/Toast'

const FORM_INICIAL = { nombre: '', cargo: '', correo: '', telefono: '', empresaId: 0 }

export default function TutoresPage() {
  const { showToast } = useToast()
  const [tutores, setTutores]     = useState<TutorEmpresarialResponse[]>([])
  const [empresas, setEmpresas]   = useState<EmpresaResponse[]>([])
  const [empresaFiltro, setEmpresaFiltro] = useState<number>(0)
  const [loading, setLoading]     = useState(false)
  const [saving, setSaving]       = useState(false)
  const [modalCrear, setModalCrear] = useState(false)
  const [form, setForm]           = useState(FORM_INICIAL)
  const [errorModal, setErrorModal] = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; nombre: string }>({
    open: false, id: 0, nombre: '',
  })

  useEffect(() => { empresaService.listarAprobadas().then(setEmpresas) }, [])

  const cargar = (empresaId: number) => {
    if (!empresaId) { setTutores([]); return }
    setLoading(true)
    tutorService.listarPorEmpresa(empresaId).then(setTutores).finally(() => setLoading(false))
  }

  const handleFiltroEmpresa = (id: number) => { setEmpresaFiltro(id); cargar(id) }

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await tutorService.crear(form)
      setModalCrear(false)
      setForm(FORM_INICIAL)
      if (empresaFiltro) cargar(empresaFiltro)
      showToast('Tutor registrado correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al registrar el tutor.')
    } finally {
      setSaving(false)
    }
  }

  const handleDesactivar = async () => {
    try {
      await tutorService.desactivar(confirm.id)
      setConfirm({ open: false, id: 0, nombre: '' })
      if (empresaFiltro) cargar(empresaFiltro)
      showToast('Tutor desactivado.')
    } catch (err: unknown) {
      showToast((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al desactivar.', 'error')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Tutores Empresariales</h1>
        <Button onClick={() => { setErrorModal(''); setModalCrear(true) }}>+ Registrar Tutor</Button>
      </div>

      <div className="card py-4 flex items-end gap-4">
        <div className="flex-1 max-w-sm">
          <label className="block text-sm font-medium text-gray-700 mb-1">Filtrar por empresa</label>
          <select className="input-field" value={empresaFiltro || ''}
            onChange={e => handleFiltroEmpresa(Number(e.target.value))}>
            <option value="">Selecciona una empresa</option>
            {empresas.map(em => <option key={em.id} value={em.id}>{em.razonSocial}</option>)}
          </select>
        </div>
        {empresaFiltro > 0 && (
          <span className="text-sm text-gray-500 pb-2"><strong>{tutores.length}</strong> tutor{tutores.length !== 1 ? 'es' : ''}</span>
        )}
      </div>

      {empresaFiltro === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">👨‍💼</div>
          <p className="text-gray-400 text-sm">Selecciona una empresa para ver sus tutores.</p>
        </div>
      ) : loading ? (
        <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" /></div>
      ) : tutores.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">👤</div>
          <p className="text-gray-400 text-sm">Esta empresa no tiene tutores registrados.</p>
          <button className="mt-3 text-cue-primary text-sm font-medium hover:underline" onClick={() => setModalCrear(true)}>Registrar el primero</button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {tutores.map(t => (
            <div key={t.id} className="card hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-800 truncate">{t.nombre}</h3>
                  {t.cargo && <p className="text-xs text-gray-500 mt-0.5">{t.cargo}</p>}
                </div>
                <span className={t.activo ? 'badge-apto ml-2 shrink-0' : 'badge-no-apto ml-2 shrink-0'}>
                  {t.activo ? 'Activo' : 'Inactivo'}
                </span>
              </div>
              <div className="mt-3 space-y-1.5">
                <p className="text-sm text-gray-600">✉ {t.correo}</p>
                {t.telefono && <p className="text-sm text-gray-600">📞 {t.telefono}</p>}
                <p className={`text-xs mt-1 font-medium ${t.disponible ? 'text-green-600' : 'text-amber-600'}`}>
                  {t.disponible ? '● Disponible' : '● No disponible'}
                </p>
              </div>
              {t.activo && (
                <button onClick={() => setConfirm({ open: true, id: t.id, nombre: t.nombre })}
                  className="mt-3 text-xs text-red-500 hover:text-red-700 transition-colors">Desactivar tutor</button>
              )}
            </div>
          ))}
        </div>
      )}

      {modalCrear && (
        <Modal title="Registrar Tutor Empresarial" onClose={() => { setModalCrear(false); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleCrear} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Empresa <span className="text-red-500">*</span></label>
              <select className="input-field" required value={form.empresaId || ''} onChange={e => setForm({ ...form, empresaId: Number(e.target.value) })}>
                <option value="">Selecciona una empresa aprobada</option>
                {empresas.map(em => <option key={em.id} value={em.id}>{em.razonSocial}</option>)}
              </select>
            </div>
            <Input label="Nombre" required value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} />
            <Input label="Cargo" value={form.cargo} onChange={e => setForm({ ...form, cargo: e.target.value })} />
            <Input label="Correo" type="email" required value={form.correo} onChange={e => setForm({ ...form, correo: e.target.value })} />
            <Input label="Teléfono" value={form.telefono} onChange={e => setForm({ ...form, telefono: e.target.value })} />
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => { setModalCrear(false); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Registrar</Button>
            </div>
          </form>
        </Modal>
      )}

      <ConfirmModal open={confirm.open} title="Desactivar tutor"
        message={`¿Desactivar a "${confirm.nombre}"? Dejará de estar disponible para nuevas asignaciones.`}
        confirmLabel="Desactivar" variant="danger"
        onConfirm={handleDesactivar}
        onCancel={() => setConfirm({ open: false, id: 0, nombre: '' })}
      />
    </div>
  )
}
