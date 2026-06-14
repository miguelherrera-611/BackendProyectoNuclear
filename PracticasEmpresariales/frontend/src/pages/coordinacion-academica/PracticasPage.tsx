import { useState, useEffect } from 'react'
import { CatalogoPracticaResponse, ProgramaResponse, ApiResponse, Pageable } from '../../types'
import { catalogoPracticaService } from '../../services/catalogoPracticaService'
import { sprint4Service, type ConfigurarProgramaRequest } from '../../services/sprint4Service'
import { REQUISITOS_CIERRE, CONFIG_DEFAULTS } from '../../constants/configuracion'
import api from '../../services/api'
import { Modal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Table } from '../../components/common/Table/Table'
import { useToast } from '../../components/common/Notifications/Toast'

const FORM_INICIAL = {
  programaId: '',
  numeroPractica: 1,
  nombre: '',
  materiaNucleo: '',
  codigoMateria: '',
  numCortes: 3,
  duracionSemanas: 16,
  documentosRequeridos: '',
}

export default function PracticasPage() {
  const { showToast } = useToast()
  const [catalogos, setCatalogos]     = useState<CatalogoPracticaResponse[]>([])
  const [programas, setProgramas]     = useState<ProgramaResponse[]>([])
  const [loading, setLoading]         = useState(true)
  const [saving, setSaving]           = useState(false)
  const [modalCrear, setModalCrear]   = useState(false)
  const [modalEditar, setModalEditar] = useState<CatalogoPracticaResponse | null>(null)
  const [form, setForm]               = useState(FORM_INICIAL)
  const [formEditar, setFormEditar]   = useState<Partial<typeof FORM_INICIAL>>({})
  const [filtroPrograma, setFiltroPrograma] = useState('')
  const [errorModal, setErrorModal]   = useState('')

  const [config, setConfig]                 = useState<ConfigurarProgramaRequest>(CONFIG_DEFAULTS)
  const [cargandoConfig, setCargandoConfig] = useState(false)

  const cargar = () => {
    setLoading(true)
    catalogoPracticaService.listar().then(setCatalogos).finally(() => setLoading(false))
  }

  useEffect(() => {
    cargar()
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas?size=100')
      .then(r => setProgramas(r.data.datos?.content ?? []))
  }, [])

  const cargarConfigPrograma = async (programaId: string) => {
    if (!programaId) { setConfig(CONFIG_DEFAULTS); return }
    setCargandoConfig(true)
    try {
      const datos = await sprint4Service.obtenerConfiguracionPrograma(Number(programaId))
      setConfig({
        numeroPracticas:       datos.numeroPracticas,
        semanasSeguimiento:    datos.semanasSeguimiento,
        notaMinimaAprobacion:  datos.notaMinimaAprobacion,
        requisitosCierre:      datos.requisitosCierre ?? CONFIG_DEFAULTS.requisitosCierre,
        umbralInactividadDias: datos.umbralInactividadDias,
      })
    } catch {
      setConfig(CONFIG_DEFAULTS)
    } finally {
      setCargandoConfig(false)
    }
  }

  const requisitosActivos = config.requisitosCierre
    ? config.requisitosCierre.split(',').map(r => r.trim()).filter(Boolean)
    : []

  const toggleRequisito = (valor: string) => {
    const siguiente = requisitosActivos.includes(valor)
      ? requisitosActivos.filter(r => r !== valor)
      : [...requisitosActivos, valor]
    setConfig({ ...config, requisitosCierre: siguiente.join(',') })
  }

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await catalogoPracticaService.crear({ ...form, programaId: Number(form.programaId) })
      await sprint4Service.configurarPrograma(Number(form.programaId), config)
      setModalCrear(false)
      setForm(FORM_INICIAL)
      setConfig(CONFIG_DEFAULTS)
      cargar()
      showToast('Catálogo y parámetros del programa guardados correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al crear el catálogo.')
    } finally {
      setSaving(false)
    }
  }

  const handleEditar = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!modalEditar) return
    setErrorModal('')
    setSaving(true)
    try {
      await catalogoPracticaService.editar(modalEditar.id, {
        nombre:               formEditar.nombre,
        materiaNucleo:        formEditar.materiaNucleo,
        codigoMateria:        formEditar.codigoMateria,
        numCortes:            formEditar.numCortes,
        duracionSemanas:      formEditar.duracionSemanas,
        documentosRequeridos: formEditar.documentosRequeridos,
      })
      setModalEditar(null)
      cargar()
      showToast('Catálogo actualizado correctamente.')
    } catch (err: unknown) {
      setErrorModal((err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al editar.')
    } finally {
      setSaving(false)
    }
  }

  const abrirEditar = (cat: CatalogoPracticaResponse) => {
    setFormEditar({
      nombre:               cat.nombre,
      materiaNucleo:        cat.materiaNucleo,
      codigoMateria:        cat.codigoMateria,
      numCortes:            cat.numCortes,
      duracionSemanas:      cat.duracionSemanas,
      documentosRequeridos: cat.documentosRequeridos ?? '',
    })
    setErrorModal('')
    setModalEditar(cat)
  }

  const catalogosFiltrados = filtroPrograma
    ? catalogos.filter(c => String(c.programaId) === filtroPrograma)
    : catalogos

  const programasConCatalogos = programas.filter(p => catalogos.some(c => c.programaId === p.id))

  const HEADERS = ['#', 'Nombre', 'Programa', 'Materia Núcleo', 'Cortes', 'Semanas', 'Estado', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Catálogo de Prácticas</h1>
          <p className="text-sm text-gray-500 mt-1">Plantillas de prácticas por programa académico.</p>
        </div>
        <Button onClick={() => { setErrorModal(''); setForm(FORM_INICIAL); setConfig(CONFIG_DEFAULTS); setModalCrear(true) }}>
          + Nuevo Catálogo
        </Button>
      </div>

      <div className="card py-3 flex gap-4 items-end flex-wrap">
        <div className="flex-1 min-w-48">
          <label className="block text-xs font-medium text-gray-600 mb-1">Filtrar por programa</label>
          <select className="input-field" value={filtroPrograma}
            onChange={e => setFiltroPrograma(e.target.value)}>
            <option value="">Todos los programas</option>
            {programasConCatalogos.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
          </select>
        </div>
        <span className="text-sm text-gray-500 self-end pb-2">
          {catalogosFiltrados.length} catálogo{catalogosFiltrados.length !== 1 ? 's' : ''}
        </span>
      </div>

      <Table headers={HEADERS} loading={loading} empty={catalogosFiltrados.length === 0}
        emptyMessage="No hay catálogos registrados." emptyIcon="📄">
        {catalogosFiltrados.map(cat => (
          <tr key={cat.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 font-medium text-cue-primary">{cat.numeroPractica}</td>
            <td className="px-4 py-3">
              <div className="font-medium text-gray-900">{cat.nombre}</div>
              <div className="text-xs text-gray-400">{cat.codigoMateria}</div>
            </td>
            <td className="px-4 py-3 text-gray-600 text-sm">{cat.programaNombre}</td>
            <td className="px-4 py-3 text-gray-600 text-sm">{cat.materiaNucleo}</td>
            <td className="px-4 py-3 text-center text-gray-700">{cat.numCortes}</td>
            <td className="px-4 py-3 text-center text-gray-700">{cat.duracionSemanas}</td>
            <td className="px-4 py-3">
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${cat.activo ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-500'}`}>
                {cat.activo ? 'Activo' : 'Inactivo'}
              </span>
            </td>
            <td className="px-4 py-3">
              <button onClick={() => abrirEditar(cat)}
                className="text-xs bg-blue-50 text-blue-700 px-3 py-1 rounded-lg hover:bg-blue-100 transition-colors font-medium">
                Editar
              </button>
            </td>
          </tr>
        ))}
      </Table>

      {/* Modal: Crear */}
      {modalCrear && (
        <Modal title="Nuevo Catálogo de Práctica"
          subtitle="Define la plantilla de la práctica y los parámetros operativos del programa."
          size="lg" onClose={() => { setModalCrear(false); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleCrear} className="space-y-6">

            {/* ── Sección 1: Datos del catálogo ── */}
            <div className="space-y-3">
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider border-b border-gray-200 pb-2">
                Datos del catálogo
              </h3>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Programa <span className="text-red-500">*</span>
                </label>
                <select className="input-field" required value={form.programaId}
                  onChange={e => {
                    setForm({ ...form, programaId: e.target.value })
                    cargarConfigPrograma(e.target.value)
                  }}>
                  <option value="">— Selecciona un programa —</option>
                  {programas.map(p => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Input label="N° de práctica" type="number" min={1} required value={form.numeroPractica}
                  onChange={e => setForm({ ...form, numeroPractica: Number(e.target.value) })} />
                <Input label="Cortes" type="number" min={1} required value={form.numCortes}
                  onChange={e => setForm({ ...form, numCortes: Number(e.target.value) })} />
              </div>
              <Input label="Nombre" required value={form.nombre}
                onChange={e => setForm({ ...form, nombre: e.target.value })} />
              <div className="grid grid-cols-2 gap-3">
                <Input label="Materia núcleo" required value={form.materiaNucleo}
                  onChange={e => setForm({ ...form, materiaNucleo: e.target.value })} />
                <Input label="Código materia" required value={form.codigoMateria}
                  onChange={e => setForm({ ...form, codigoMateria: e.target.value })} />
              </div>
              <Input label="Duración (semanas)" type="number" min={1} required value={form.duracionSemanas}
                onChange={e => setForm({ ...form, duracionSemanas: Number(e.target.value) })} />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Documentos requeridos</label>
                <textarea className="input-field" rows={2} placeholder="Describe los documentos necesarios..."
                  value={form.documentosRequeridos}
                  onChange={e => setForm({ ...form, documentosRequeridos: e.target.value })} />
              </div>
            </div>

            {/* ── Sección 2: Parámetros del programa ── */}
            <div className="space-y-4">
              <div className="border-b border-gray-200 pb-2 flex items-center gap-2">
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Parámetros del programa
                </h3>
                {cargandoConfig && (
                  <span className="text-xs text-cue-primary flex items-center gap-1">
                    <span className="animate-spin rounded-full h-3 w-3 border-b-2 border-cue-primary inline-block" />
                    Cargando configuración...
                  </span>
                )}
              </div>
              <p className="text-xs text-gray-400 -mt-2">
                Reglas operativas que aplican a todo el programa. Al seleccionar un programa se carga su configuración actual.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Número de prácticas del programa"
                  type="number" min={1}
                  value={config.numeroPracticas}
                  onChange={e => setConfig({ ...config, numeroPracticas: Number(e.target.value) })}
                  hint="Total de prácticas que puede cursar un estudiante."
                  disabled={!form.programaId}
                />
                <Input
                  label="Semanas de seguimiento"
                  type="number" min={1}
                  value={config.semanasSeguimiento}
                  onChange={e => setConfig({ ...config, semanasSeguimiento: Number(e.target.value) })}
                  hint="Duración del período de seguimiento por práctica."
                  disabled={!form.programaId}
                />
                <Input
                  label="Nota mínima de aprobación (0 – 5)"
                  type="number" step="0.1" min={0} max={5}
                  value={config.notaMinimaAprobacion}
                  onChange={e => setConfig({ ...config, notaMinimaAprobacion: Number(e.target.value) })}
                  hint="Nota mínima para aprobar la práctica."
                  disabled={!form.programaId}
                />
                <Input
                  label="Días de inactividad permitidos"
                  type="number" min={1}
                  value={config.umbralInactividadDias}
                  onChange={e => setConfig({ ...config, umbralInactividadDias: Number(e.target.value) })}
                  hint="Sin reportes en este plazo → práctica marcada inactiva."
                  disabled={!form.programaId}
                />
              </div>

              <div>
                <p className="text-sm font-medium text-gray-700 mb-1">Requisitos para el cierre formal</p>
                <p className="text-xs text-gray-400 mb-3">
                  Condiciones que deben cumplirse antes de poder cerrar oficialmente una práctica.
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {REQUISITOS_CIERRE.map(req => (
                    <label
                      key={req.valor}
                      className={`flex items-center gap-3 px-3 py-2.5 rounded-lg border cursor-pointer transition-colors ${
                        !form.programaId
                          ? 'opacity-40 cursor-not-allowed border-gray-200 bg-gray-50'
                          : requisitosActivos.includes(req.valor)
                          ? 'border-cue-accent bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <input
                        type="checkbox"
                        className="accent-cue-accent h-4 w-4 shrink-0"
                        checked={requisitosActivos.includes(req.valor)}
                        onChange={() => toggleRequisito(req.valor)}
                        disabled={!form.programaId}
                      />
                      <span className="text-sm text-gray-700">{req.etiqueta}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex gap-3 pt-2 border-t border-gray-100">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => { setModalCrear(false); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Crear catálogo</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Editar */}
      {modalEditar && (
        <Modal title="Editar Catálogo"
          subtitle={`${modalEditar.nombre} — ${modalEditar.programaNombre}`}
          size="lg" onClose={() => { setModalEditar(null); setErrorModal('') }}>
          {errorModal && <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleEditar} className="space-y-3">
            <Input label="Nombre" value={formEditar.nombre ?? ''}
              onChange={e => setFormEditar({ ...formEditar, nombre: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Materia núcleo" value={formEditar.materiaNucleo ?? ''}
                onChange={e => setFormEditar({ ...formEditar, materiaNucleo: e.target.value })} />
              <Input label="Código materia" value={formEditar.codigoMateria ?? ''}
                onChange={e => setFormEditar({ ...formEditar, codigoMateria: e.target.value })} />
              <Input label="Cortes" type="number" min={1} value={formEditar.numCortes ?? ''}
                onChange={e => setFormEditar({ ...formEditar, numCortes: Number(e.target.value) })} />
              <Input label="Semanas" type="number" min={1} value={formEditar.duracionSemanas ?? ''}
                onChange={e => setFormEditar({ ...formEditar, duracionSemanas: Number(e.target.value) })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Documentos requeridos</label>
              <textarea className="input-field" rows={2} value={formEditar.documentosRequeridos ?? ''}
                onChange={e => setFormEditar({ ...formEditar, documentosRequeridos: e.target.value })} />
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button"
                onClick={() => { setModalEditar(null); setErrorModal('') }}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Guardar Cambios</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
