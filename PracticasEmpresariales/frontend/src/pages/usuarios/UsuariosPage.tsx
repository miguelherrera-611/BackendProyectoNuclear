import { useState, useEffect, useMemo } from 'react'
import { UsuarioResponse, Rol, EtiquetaCargo, EstadoCuenta, FacultadResponse, ProgramaResponse } from '../../types'
import { usuarioService } from '../../services/usuarioService'
import { ROL_LABELS } from '../../constants/roles'
import api from '../../services/api'
import { ApiResponse, Pageable } from '../../types'
import { Modal } from '../../components/common/Modal/Modal'
import { Button } from '../../components/common/Button/Button'
import { Input } from '../../components/common/Input/Input'
import { Select } from '../../components/common/Select/Select'
import { Table } from '../../components/common/Table/Table'
import { Pagination } from '../../components/common/Table/Pagination'
import { ListFilters } from '../../components/common/ListFilters'
import { useToast } from '../../components/common/Notifications/Toast'

const ROL_OPTIONS: Rol[] = [
  'ADMIN_DTI', 'COORDINACION_ACADEMICA', 'COORDINADOR_PRACTICAS',
  'DOCENTE_ASESOR', 'TUTOR_EMPRESARIAL', 'ESTUDIANTE', 'DIRECCION',
]

export default function UsuariosPage() {
  const { showToast } = useToast()
  const [usuarios, setUsuarios]     = useState<UsuarioResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<UsuarioResponse> | null>(null)
  const [facultades, setFacultades] = useState<FacultadResponse[]>([])
  const [programas, setProgramas]   = useState<ProgramaResponse[]>([])
  const [busqueda, setBusqueda]     = useState('')
  const [rolFiltro, setRolFiltro]   = useState<'todos' | Rol>('todos')
  const [estadoFiltro, setEstadoFiltro] = useState<'todos' | 'activos' | 'inactivos'>('todos')
  const [loading, setLoading]       = useState(true)
  const [saving, setSaving]         = useState(false)
  const [modalAbierto, setModalAbierto] = useState(false)
  const [form, setForm] = useState({
    nombre: '', correo: '', rol: '' as Rol,
    etiquetaCargo: '' as EtiquetaCargo | '',
    telefono: '', facultadId: '', programaId: '',
    identificacion: '', semestre: '', contactoEmergencia: '',
  })
  const [errorModal, setErrorModal] = useState('')

  const cargar = () => {
    setLoading(true)
    usuarioService.listar(pagina).then(p => {
      setUsuarios(p.content)
      setPageData(p)
    }).finally(() => setLoading(false))
  }

  useEffect(() => {
    cargar()
    api.get<ApiResponse<Pageable<FacultadResponse>>>('/facultades?size=100')
      .then(r => setFacultades(r.data.datos?.content ?? []))
    api.get<ApiResponse<Pageable<ProgramaResponse>>>('/programas?size=100')
      .then(r => setProgramas(r.data.datos?.content ?? []))
  }, [pagina])

  const usuariosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return usuarios.filter(u => {
      const coincideTexto = !texto || [
        u.nombre,
        u.correo,
        u.etiquetaCargo,
        u.identificacion,
        u.programaNombre,
        u.facultadNombre,
        ROL_LABELS[u.rol],
      ].some(valor => valor?.toLowerCase().includes(texto))
      const coincideRol = rolFiltro === 'todos' || u.rol === rolFiltro
      const coincideEstado = estadoFiltro === 'todos'
        ? true
        : estadoFiltro === 'activos'
          ? u.activo
          : !u.activo
      return coincideTexto && coincideRol && coincideEstado
    })
  }, [busqueda, estadoFiltro, rolFiltro, usuarios])

  const limpiarFiltros = () => {
    setBusqueda('')
    setRolFiltro('todos')
    setEstadoFiltro('todos')
  }

  const handleRolChange = (rol: Rol) => {
    setForm({ ...form, rol, etiquetaCargo: '', facultadId: '', programaId: '',
      identificacion: '', semestre: '', contactoEmergencia: '' })
  }

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorModal('')
    setSaving(true)
    try {
      await usuarioService.crear({
        nombre: form.nombre, correo: form.correo, rol: form.rol,
        etiquetaCargo: form.etiquetaCargo || undefined,
        telefono: form.telefono || undefined,
        facultadId: form.facultadId ? Number(form.facultadId) : undefined,
        programaId: form.programaId ? Number(form.programaId) : undefined,
        identificacion: form.rol === 'ESTUDIANTE' ? form.identificacion || undefined : undefined,
        semestre: form.rol === 'ESTUDIANTE' && form.semestre ? Number(form.semestre) : undefined,
        contactoEmergencia: form.rol === 'ESTUDIANTE' ? form.contactoEmergencia || undefined : undefined,
      })
      setModalAbierto(false)
      setForm({ nombre: '', correo: '', rol: '' as Rol, etiquetaCargo: '', telefono: '',
        facultadId: '', programaId: '', identificacion: '', semestre: '', contactoEmergencia: '' })
      cargar()
      showToast('Usuario creado. Se envió la contraseña temporal al correo.')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      setErrorModal(msg ?? 'Error al crear el usuario.')
    } finally {
      setSaving(false)
    }
  }

  const handleToggle = async (u: UsuarioResponse) => {
    try {
      if (u.activo) await usuarioService.desactivar(u.id)
      else await usuarioService.activar(u.id)
      cargar()
      showToast(`Usuario ${u.activo ? 'desactivado' : 'activado'}.`)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje
      showToast(msg ?? 'Error al cambiar estado.', 'error')
    }
  }

  const estadoBadge = (activo: boolean) => activo
    ? <span className="badge-apto">Habilitado</span>
    : <span className="badge-no-apto">Deshabilitado</span>

  const accesoBadge = (estado: EstadoCuenta | undefined) => estado === 'ACTIVO'
    ? <span className="badge-apto">Accedió</span>
    : <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">Pendiente</span>

  const HEADERS = ['Nombre', 'Correo', 'Rol', 'Cargo', 'Cuenta', 'Primer acceso', 'Acciones']

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Gestión de Usuarios</h1>
        <Button onClick={() => { setErrorModal(''); setModalAbierto(true) }}>+ Crear Usuario</Button>
      </div>

      <ListFilters
        search={{
          label: 'Buscar usuario',
          placeholder: 'Nombre, correo, rol, cargo o programa...',
          value: busqueda,
          onChange: setBusqueda,
        }}
        summary={`${usuariosFiltrados.length} de ${usuarios.length}`}
        onClear={limpiarFiltros}
      >
        <div className="w-full sm:w-56">
          <Select label="Rol" value={rolFiltro} onChange={e => setRolFiltro(e.target.value as typeof rolFiltro)}>
            <option value="todos">Todos</option>
            {ROL_OPTIONS.map(r => <option key={r} value={r}>{ROL_LABELS[r]}</option>)}
          </Select>
        </div>
        <div className="w-full sm:w-56">
          <Select label="Estado" value={estadoFiltro} onChange={e => setEstadoFiltro(e.target.value as typeof estadoFiltro)}>
            <option value="todos">Todos</option>
            <option value="activos">Habilitados</option>
            <option value="inactivos">Deshabilitados</option>
          </Select>
        </div>
      </ListFilters>

      <Table headers={HEADERS} loading={loading} empty={usuariosFiltrados.length === 0}
        emptyMessage={usuarios.length === 0 ? 'No hay usuarios registrados.' : 'No hay usuarios que coincidan con los filtros.'} emptyIcon="👥">
        {usuariosFiltrados.map(u => (
          <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50">
            <td className="px-4 py-3 font-medium text-gray-900">{u.nombre}</td>
            <td className="px-4 py-3 text-gray-500 text-xs">{u.correo}</td>
            <td className="px-4 py-3"><span className="badge-rol">{ROL_LABELS[u.rol]}</span></td>
            <td className="px-4 py-3 text-gray-500 text-xs">{u.etiquetaCargo ?? '—'}</td>
            <td className="px-4 py-3">{estadoBadge(u.activo)}</td>
            <td className="px-4 py-3">{accesoBadge(u.estadoCuenta)}</td>
            <td className="px-4 py-3">
              <button onClick={() => handleToggle(u)}
                className={`text-xs px-3 py-1 rounded-full border transition-colors ${
                  u.activo ? 'border-red-300 text-red-600 hover:bg-red-50' : 'border-green-300 text-green-600 hover:bg-green-50'
                }`}>
                {u.activo ? 'Desactivar' : 'Activar'}
              </button>
            </td>
          </tr>
        ))}
      </Table>

      <Pagination
        page={pagina}
        totalPages={pageData?.totalPages ?? 0}
        totalElements={pageData?.totalElements}
        onPageChange={setPagina}
        disabled={loading}
      />

      {modalAbierto && (
        <Modal title="Crear nuevo usuario" subtitle="La contraseña temporal se enviará al correo." size="lg"
          onClose={() => setModalAbierto(false)}>
          {errorModal && <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">{errorModal}</div>}
          <form onSubmit={handleCrear} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Input label="Nombre completo" required value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} />
              <Input label="Correo electrónico" type="email" required value={form.correo} onChange={e => setForm({ ...form, correo: e.target.value })} />
              <Select label="Rol" required value={form.rol} onChange={e => handleRolChange(e.target.value as Rol)}>
                <option value="">Seleccionar...</option>
                {ROL_OPTIONS.map(r => <option key={r} value={r}>{ROL_LABELS[r]}</option>)}
              </Select>
              {form.rol === 'COORDINADOR_PRACTICAS' && (
                <Select label="Cargo" required value={form.etiquetaCargo} onChange={e => setForm({ ...form, etiquetaCargo: e.target.value as EtiquetaCargo })}>
                  <option value="">Seleccionar...</option>
                  <option value="COORDINACION_ACADEMICA">Coordinación</option>
                  <option value="SECRETARIA">Secretaría</option>
                </Select>
              )}
              <Input label="Teléfono" value={form.telefono} onChange={e => setForm({ ...form, telefono: e.target.value })} />
              {form.rol === 'COORDINADOR_PRACTICAS' && (
                <Select
                  label="Facultad"
                  required
                  value={form.facultadId}
                  onChange={e => setForm({ ...form, facultadId: e.target.value })}
                >
                  <option value="">— Selecciona una facultad —</option>
                  {facultades.map(f => <option key={f.id} value={f.id}>{f.nombre}</option>)}
                </Select>
              )}
              {['COORDINACION_ACADEMICA', 'ESTUDIANTE'].includes(form.rol) && (
                <div className="col-span-2">
                  <Select label="Programa" required value={form.programaId} onChange={e => setForm({ ...form, programaId: e.target.value })}>
                    <option value="">— Selecciona un programa —</option>
                    {programas.map(p => <option key={p.id} value={p.id}>{p.nombre} ({p.facultadNombre})</option>)}
                  </Select>
                </div>
              )}
              {form.rol === 'ESTUDIANTE' && (
                <>
                  <Input label="Identificación" required placeholder="Cédula o documento" value={form.identificacion} onChange={e => setForm({ ...form, identificacion: e.target.value })} />
                  <Select label="Semestre" required value={form.semestre} onChange={e => setForm({ ...form, semestre: e.target.value })}>
                    <option value="">Seleccionar...</option>
                    {Array.from({ length: 12 }, (_, i) => i + 1).map(s => (
                      <option key={s} value={s}>Semestre {s}</option>
                    ))}
                  </Select>
                  <div className="col-span-2">
                    <Input label="Contacto de Emergencia" placeholder="Ana Herrera - 3001234567" value={form.contactoEmergencia} onChange={e => setForm({ ...form, contactoEmergencia: e.target.value })} />
                  </div>
                </>
              )}
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" type="button" onClick={() => setModalAbierto(false)}>Cancelar</Button>
              <Button className="flex-1" type="submit" loading={saving}>Crear y enviar contraseña</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
