import { useState, useEffect } from 'react'
import type { UsuarioResponse, EmpresaResponse, Pageable } from '../../types'
import { usuarioService } from '../../services/usuarioService'
import { empresaService } from '../../services/empresaService'
import { Select } from '../../components/common/Select/Select'
import { Pagination } from '../../components/common/Table/Pagination'

export default function TutoresPage() {
  const [tutores, setTutores] = useState<UsuarioResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [pageData, setPageData] = useState<Pageable<UsuarioResponse> | null>(null)
  const [empresas, setEmpresas] = useState<EmpresaResponse[]>([])
  const [busqueda, setBusqueda] = useState('')
  const [loading, setLoading] = useState(true)

  // Estado del panel de vinculación
  const [tutorActivo, setTutorActivo] = useState<UsuarioResponse | null>(null)
  const [empresaSeleccionada, setEmpresaSeleccionada] = useState<number | ''>('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    Promise.all([
      usuarioService.listarTutoresPaginado(pagina),
      empresaService.listarActivas(),
    ]).then(([t, e]) => {
      setTutores(t.content)
      setPageData(t)
      setEmpresas(e)
    }).finally(() => setLoading(false))
  }, [pagina])

  const tutoresFiltrados = tutores.filter(t => {
    const texto = busqueda.toLowerCase()
    return !texto ||
      t.nombre.toLowerCase().includes(texto) ||
      t.correo.toLowerCase().includes(texto) ||
      (t.razonSocialEmpresa ?? '').toLowerCase().includes(texto)
  })

  const abrirPanel = (tutor: UsuarioResponse) => {
    setTutorActivo(tutor)
    setEmpresaSeleccionada(tutor.empresaId ?? '')
    setError('')
  }

  const cerrarPanel = () => {
    setTutorActivo(null)
    setEmpresaSeleccionada('')
    setError('')
  }

  const guardar = async () => {
    if (!tutorActivo) return
    setGuardando(true)
    setError('')
    try {
      const actualizado = await usuarioService.vincularEmpresa(
        tutorActivo.id,
        empresaSeleccionada !== '' ? Number(empresaSeleccionada) : null
      )
      setTutores(prev => prev.map(t => t.id === actualizado.id ? actualizado : t))
      cerrarPanel()
    } catch {
      setError('No se pudo guardar el vínculo. Intenta de nuevo.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Encabezado */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Tutores Empresariales</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          Usuarios con rol Tutor Empresarial. Vincúlalos a la empresa correspondiente.
        </p>
      </div>

      {/* Buscador */}
      <div className="card py-4">
        <input
          type="text"
          placeholder="Buscar por nombre, correo o empresa..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
          className="input-field max-w-sm"
        />
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
        </div>
      ) : tutores.length === 0 ? (
        <div className="card text-center py-16">
          <div className="text-gray-300 text-5xl mb-3">👤</div>
          <p className="text-gray-400 text-sm">No hay tutores empresariales registrados.</p>
        </div>
      ) : (
        <>
          <p className="text-xs text-gray-400">
            {tutoresFiltrados.length} de {tutores.length} tutor{tutores.length !== 1 ? 'es' : ''}
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {tutoresFiltrados.map(t => (
              <div key={t.id} className="card hover:shadow-md transition-shadow flex flex-col gap-3">
                {/* Nombre + estado */}
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-gray-800 truncate">{t.nombre}</h3>
                    <p className="text-xs text-gray-400 mt-0.5">ID: {t.id}</p>
                  </div>
                  <span className={t.activo ? 'badge-apto ml-2 shrink-0' : 'badge-no-apto ml-2 shrink-0'}>
                    {t.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </div>

                {/* Datos de contacto */}
                <div className="space-y-1">
                  <p className="text-sm text-gray-600 truncate">✉ {t.correo}</p>
                  {t.telefono
                    ? <p className="text-sm text-gray-600">📞 {t.telefono}</p>
                    : <p className="text-xs text-gray-400 italic">Sin teléfono</p>
                  }
                </div>

                {/* Empresa vinculada */}
                <div className="border-t border-gray-100 pt-2">
                  {t.razonSocialEmpresa ? (
                    <p className="text-sm text-gray-700">
                      🏢 <span className="font-medium">{t.razonSocialEmpresa}</span>
                    </p>
                  ) : (
                    <p className="text-xs text-amber-600 italic">Sin empresa vinculada</p>
                  )}
                </div>

                {/* Acción vincular */}
                <button
                  className="btn-secondary text-sm py-1.5"
                  onClick={() => abrirPanel(t)}
                >
                  {t.empresaId ? 'Cambiar empresa' : 'Vincular empresa'}
                </button>
              </div>
            ))}
          </div>

          <Pagination
            page={pagina}
            totalPages={pageData?.totalPages ?? 0}
            totalElements={pageData?.totalElements}
            onPageChange={setPagina}
            disabled={loading}
          />
        </>
      )}

      {/* Panel / modal de vinculación */}
      {tutorActivo && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Vincular empresa</h2>
              <p className="text-sm text-gray-500 mt-0.5">Tutor: <strong>{tutorActivo.nombre}</strong></p>
            </div>

            <div className="space-y-2">
              <Select
                label="Empresa"
                value={empresaSeleccionada}
                onChange={e => setEmpresaSeleccionada(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <option value="">— Sin empresa (desvincular) —</option>
                {empresas.map(e => (
                  <option key={e.id} value={e.id}>{e.razonSocial} · {e.nit}</option>
                ))}
              </Select>
              {empresas.length === 0 && (
                <p className="text-xs text-amber-600">No hay empresas activas registradas.</p>
              )}
            </div>

            {error && <p className="text-sm text-red-600">{error}</p>}

            <div className="flex gap-3 pt-1">
              <button className="btn-secondary flex-1" onClick={cerrarPanel} disabled={guardando}>
                Cancelar
              </button>
              <button className="btn-primary flex-1" onClick={guardar} disabled={guardando}>
                {guardando ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
