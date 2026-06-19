import { useState, useEffect, useMemo } from 'react'
import { FacultadResponse, ApiResponse, Pageable } from '../../types'
import api from '../../services/api'
import { Modal, ConfirmModal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Select } from '../../components/common/Select/Select'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'

export default function FacultadesPage() {
  const { showToast } = useToast()
  const [facultades, setFacultades]   = useState<FacultadResponse[]>([])
  const [busqueda, setBusqueda] = useState('')
  const [estadoFiltro, setEstadoFiltro] = useState<'todas' | 'activas' | 'inactivas'>('todas')
  const [loading, setLoading]         = useState(true)
  const [saving, setSaving]           = useState(false)
  const [modalCrear, setModalCrear]   = useState(false)
  const [form, setForm]               = useState({ nombre: '', descripcion: '' })
  const [errorModal, setErrorModal]   = useState('')
  const [confirm, setConfirm] = useState<{ open: boolean; id: number; nombre: string; accion: 'activar' | 'desactivar' }>({
    open: false, id: 0, nombre: '', accion: 'desactivar',
  })
  const [alerta, setAlerta] = useState<{ open: boolean; mensaje: string }>({ open: false, mensaje: '' })
  const [panel, setPanel] = useState<FacultadResponse | null>(null)

  const cargar = () => {
    setLoading(true)
    api.get<ApiResponse<Pageable<FacultadResponse>>>('/facultades?incluirInactivas=true&size=200')
      .then(r => setFacultades(r.data.datos?.content ?? []))
      .finally(() => setLoading(false))
  }

  useEffect(() => { cargar() }, [])

  const facultadesFiltradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return facultades.filter(f => {
      const coincideTexto = !texto || [f.nombre, f.descripcion].some(valor => valor?.toLowerCase().includes(texto))
      const coincideEstado = estadoFiltro === 'todas'
        ? true
        : estadoFiltro === 'activas'
          ? f.activa
          : !f.activa
      return coincideTexto && coincideEstado
    })
  }, [busqueda, facultades, estadoFiltro])

  const limpiarFiltros = () => {
    setBusqueda('')
    setEstadoFiltro('todas')
  }

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await api.post('/facultades', form)
      setModalCrear(false)
      setForm({ nombre: '', descripcion: '' })
      cargar()
      showToast('Facultad creada correctamente.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setErrorModal(msg ?? 'Error al crear la facultad.')
    } finally {
      setSaving(false)
    }
  }

  const handleConfirmar = async () => {
    try {
      await api.patch(`/facultades/${confirm.id}/${confirm.accion}`)
      setConfirm({ open: false, id: 0, nombre: '', accion: 'desactivar' })
      if (panel?.id === confirm.id) setPanel(null)
      cargar()
      showToast(confirm.accion === 'activar' ? 'Facultad activada.' : 'Facultad desactivada.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? `No se puede ${confirm.accion} la facultad.`, 'error')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Facultades</h1>
        <Button onClick={() => { setErrorModal(''); setModalCrear(true) }}>
          + Nueva Facultad
        </Button>
      </div>

      <ListFilters
        search={{
          label: 'Buscar facultad',
          placeholder: 'Nombre o descripción...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${facultadesFiltradas.length} de ${facultades.length}`}
        onClear={limpiarFiltros}
      >
        <div className="w-full sm:w-56">
          <Select label="Estado" value={estadoFiltro} onChange={e => setEstadoFiltro(e.target.value as typeof estadoFiltro)}>
            <option value="todas">Todas</option>
            <option value="activas">Activas</option>
            <option value="inactivas">Inactivas</option>
          </Select>
        </div>
      </ListFilters>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {loading ? (
          <div className="col-span-3 flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
          </div>
        ) : facultadesFiltradas.length === 0 ? (
          <div className="col-span-3 card text-center py-16">
            <div className="text-gray-300 text-5xl mb-3">🏛️</div>
            <p className="text-gray-400 text-sm">
              {facultades.length === 0 ? 'No hay facultades registradas.' : 'No hay facultades que coincidan con los filtros.'}
            </p>
            <button className="mt-3 text-cue-primary text-sm font-medium hover:underline"
              onClick={() => setModalCrear(true)}>
              Crear la primera
            </button>
          </div>
        ) : facultadesFiltradas.map(f => (
          <div
            key={f.id}
            className="card hover:shadow-md transition-shadow cursor-pointer"
            onClick={() => setPanel(f)}
          >
            <div className="flex items-start justify-between">
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-gray-800 truncate">{f.nombre}</h3>
                {f.descripcion && <p className="text-xs text-gray-500 mt-1">{f.descripcion}</p>}
              </div>
              <span className={f.activa ? 'badge-apto ml-2 shrink-0' : 'badge-no-apto ml-2 shrink-0'}>
                {f.activa ? 'Activa' : 'Inactiva'}
              </span>
            </div>
            <div className="mt-3 flex items-center justify-between">
              <p className="text-sm text-gray-600">
                <span className="font-semibold text-cue-primary">{f.numeroProgramas}</span> programas
              </p>
              {f.activa ? (
                <button
                  onClick={e => {
                    e.stopPropagation()
                    if (f.tieneProgramasActivos) {
                      setAlerta({ open: true, mensaje: `"${f.nombre}" no puede desactivarse porque tiene programas activos. Desactiva primero todos sus programas.` })
                    } else {
                      setConfirm({ open: true, id: f.id, nombre: f.nombre, accion: 'desactivar' })
                    }
                  }}
                  className="text-xs text-red-500 hover:text-red-700 transition-colors"
                >
                  Desactivar
                </button>
              ) : (
                <button
                  onClick={e => {
                    e.stopPropagation()
                    setConfirm({ open: true, id: f.id, nombre: f.nombre, accion: 'activar' })
                  }}
                  className="text-xs text-green-600 hover:text-green-800 transition-colors"
                >
                  Activar
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Drawer lateral izquierdo */}
      {panel && (
        <>
          <div
            className="fixed inset-0 bg-black/30 z-40"
            onClick={() => setPanel(null)}
          />
          <div className="fixed left-0 top-0 h-full w-80 bg-white shadow-2xl z-50 flex flex-col">
            <div className="flex items-start justify-between px-5 py-4 border-b border-gray-100 shrink-0">
              <div className="min-w-0 pr-3">
                <h2 className="font-bold text-gray-800 truncate">{panel.nombre}</h2>
                {panel.descripcion && (
                  <p className="text-xs text-gray-500 mt-0.5">{panel.descripcion}</p>
                )}
                <p className="text-xs text-gray-400 mt-1">
                  {panel.programas.length} {panel.programas.length === 1 ? 'programa' : 'programas'}
                </p>
              </div>
              <button
                onClick={() => setPanel(null)}
                className="text-gray-400 hover:text-gray-600 text-2xl leading-none shrink-0"
              >
                ×
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-3">
              {panel.programas.length === 0 ? (
                <div className="text-center py-12">
                  <div className="text-gray-200 text-4xl mb-2">📚</div>
                  <p className="text-sm text-gray-400">Sin programas registrados</p>
                </div>
              ) : (
                <ul className="space-y-3">
                  {panel.programas.map(p => (
                    <li
                      key={p.id}
                      className="rounded-lg border border-gray-100 p-3 hover:bg-gray-50 transition-colors"
                    >
                      <div className="flex items-center justify-between gap-2 mb-2">
                        <span className="font-medium text-sm text-gray-800 leading-snug">{p.nombre}</span>
                        <span className={`text-xs shrink-0 ${p.activo ? 'badge-apto' : 'badge-no-apto'}`}>
                          {p.activo ? 'Activo' : 'Inactivo'}
                        </span>
                      </div>
                      <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
                        <div>
                          <p className="text-gray-400">Prácticas</p>
                          <p className="font-semibold text-gray-700">{p.numeroTotalPracticas}</p>
                        </div>
                        <div>
                          <p className="text-gray-400">Promedio mín.</p>
                          <p className="font-semibold text-gray-700">{p.promedioMinimoGeneral != null ? p.promedioMinimoGeneral.toFixed(1) : '—'}</p>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </>
      )}

      {/* Modal: Crear */}
      {modalCrear && (
        <Modal title="Nueva Facultad" onClose={() => setModalCrear(false)}>
          {errorModal && (
            <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>
          )}
          <form onSubmit={handleCrear} className="space-y-4">
            <Input
              label="Nombre"
              required
              value={form.nombre}
              onChange={e => setForm({ ...form, nombre: e.target.value })}
            />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
              <textarea
                className="input-field"
                rows={3}
                value={form.descripcion}
                onChange={e => setForm({ ...form, descripcion: e.target.value })}
              />
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalCrear(false)}>
                Cancelar
              </Button>
              <Button className="flex-1" type="submit" loading={saving}>
                Crear
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {alerta.open && (
        <Modal title="No se puede desactivar" size="sm" onClose={() => setAlerta({ open: false, mensaje: '' })}>
          <div className="flex justify-center mb-4">
            <div className="w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center text-2xl">⚠️</div>
          </div>
          <p className="text-gray-600 text-sm text-center mb-6">{alerta.mensaje}</p>
          <Button className="w-full" onClick={() => setAlerta({ open: false, mensaje: '' })}>
            Entendido
          </Button>
        </Modal>
      )}

      <ConfirmModal
        open={confirm.open}
        title={confirm.accion === 'activar' ? 'Activar facultad' : 'Desactivar facultad'}
        message={confirm.accion === 'activar'
          ? `¿Activar "${confirm.nombre}"?`
          : `¿Desactivar "${confirm.nombre}"?`
        }
        confirmLabel={confirm.accion === 'activar' ? 'Activar' : 'Desactivar'}
        variant={confirm.accion === 'activar' ? 'primary' : 'danger'}
        onConfirm={handleConfirmar}
        onCancel={() => setConfirm({ open: false, id: 0, nombre: '', accion: 'desactivar' })}
      />
    </div>
  )
}
