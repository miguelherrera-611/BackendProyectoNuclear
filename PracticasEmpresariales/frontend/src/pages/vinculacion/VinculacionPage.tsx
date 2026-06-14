import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponseV2, UsuarioResponse, ApiResponse, Pageable } from '../../types'
import api from '../../services/api'

const TIPOS_FIRMA = ['TUTOR', 'DOCENTE', 'ESTUDIANTE'] as const
type TipoFirma = typeof TIPOS_FIRMA[number]

export default function VinculacionPage() {
  const { instanciaId } = useParams<{ instanciaId: string }>()
  const navigate = useNavigate()
  const [instancia, setInstancia] = useState<InstanciaPracticaResponseV2 | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [uploadingDocType, setUploadingDocType] = useState<string | null>(null)
  const [uploadingFirmaType, setUploadingFirmaType] = useState<TipoFirma | null>(null)

  const [fechaInicio, setFechaInicio] = useState('')
  const [fechaFin, setFechaFin] = useState('')
  const [firmaTutor, setFirmaTutor] = useState(false)
  const [firmaDocente, setFirmaDocente] = useState(false)
  const [firmaEstudiante, setFirmaEstudiante] = useState(false)
  const [docenteAsesorId, setDocenteAsesorId] = useState('')
  const [docentes, setDocentes] = useState<UsuarioResponse[]>([])

  const refCarta = useRef<HTMLInputElement>(null)
  const refConvenio = useRef<HTMLInputElement>(null)
  const refFirmaTutor = useRef<HTMLInputElement>(null)
  const refFirmaDocente = useRef<HTMLInputElement>(null)
  const refFirmaEstudiante = useRef<HTMLInputElement>(null)

  const firmaRefs: Record<TipoFirma, React.RefObject<HTMLInputElement>> = {
    TUTOR: refFirmaTutor,
    DOCENTE: refFirmaDocente,
    ESTUDIANTE: refFirmaEstudiante,
  }

  const cargar = async () => {
    if (!instanciaId) return
    setLoading(true)
    try {
      const res = await api.get<ApiResponse<InstanciaPracticaResponseV2>>(`/api/v1/vinculaciones/${instanciaId}`)
      const data = res.data.datos!
      setInstancia(data)
      if (data.firmaTutor) setFirmaTutor(true)
      if (data.firmaDocente) setFirmaDocente(true)
      if (data.firmaEstudiante) setFirmaEstudiante(true)
      if (data.fechaInicio) setFechaInicio(data.fechaInicio)
      if (data.fechaFin) setFechaFin(data.fechaFin)
    } catch {
      setError('No se pudo cargar la instancia.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    cargar()
    api.get<ApiResponse<Pageable<UsuarioResponse>>>('/usuarios', { params: { size: 200 } })
      .then(r => setDocentes((r.data.datos?.content ?? []).filter(u => u.rol === 'DOCENTE_ASESOR')))
  }, [instanciaId])

  const handleSubirDocumento = async (tipo: string, file: File) => {
    setUploadingDocType(tipo)
    setError('')
    const form = new FormData()
    form.append('archivo', file)
    try {
      await api.post(`/api/v1/documentos-practica/${instanciaId}?tipo=${tipo}`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    } catch {
      setError('Error al subir el documento. Verifica el formato e intenta de nuevo.')
    } finally {
      setUploadingDocType(null)
    }
  }

  const handleSubirFirma = async (tipo: TipoFirma, file: File) => {
    setUploadingFirmaType(tipo)
    setError('')
    const form = new FormData()
    form.append('archivo', file)
    try {
      await api.post(`/api/v1/documentos-practica/${instanciaId}?tipo=FIRMA_${tipo}`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      const res = await api.patch<ApiResponse<InstanciaPracticaResponseV2>>(`/api/v1/vinculaciones/${instanciaId}/firmas/${tipo}`)
      setInstancia(res.data.datos!)
      if (tipo === 'TUTOR') setFirmaTutor(true)
      if (tipo === 'DOCENTE') setFirmaDocente(true)
      if (tipo === 'ESTUDIANTE') setFirmaEstudiante(true)
    } catch {
      setError('Error al subir la firma. Verifica el formato e intenta de nuevo.')
    } finally {
      setUploadingFirmaType(null)
    }
  }

  const handleConfirmar = async () => {
    if (!fechaInicio || !fechaFin) { setError('Las fechas de inicio y fin son obligatorias.'); return }
    setSaving(true)
    setError('')
    try {
      await api.patch(`/api/v1/vinculaciones/${instanciaId}/confirmar`, {
        fechaInicio, fechaFin, firmaTutor, firmaDocente, firmaEstudiante,
        docenteAsesorId: docenteAsesorId ? Number(docenteAsesorId) : null,
      })
      navigate('/asignaciones')
    } catch (e: unknown) {
      setError((e as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? 'Error al confirmar la vinculación.')
    } finally {
      setSaving(false)
    }
  }

  const estaDeshabilitado = instancia?.estado === 'FINALIZADA' || instancia?.estado === 'CANCELADA'

  const firmaLabel: Record<TipoFirma, string> = {
    TUTOR: 'Tutor Empresarial',
    DOCENTE: 'Docente Asesor',
    ESTUDIANTE: 'Estudiante',
  }

  const firmaConfirmada = (tipo: TipoFirma) =>
    tipo === 'TUTOR' ? instancia?.firmaTutor : tipo === 'DOCENTE' ? instancia?.firmaDocente : instancia?.firmaEstudiante

  if (loading) return <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-cue-primary" /></div>

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div>
        <button className="text-sm text-gray-500 hover:text-gray-700 mb-2" onClick={() => navigate('/asignaciones')}>
          ← Volver a asignaciones
        </button>
        <h1 className="text-2xl font-bold text-gray-900">Proceso de vinculación</h1>
        {instancia && (
          <p className="text-sm text-gray-500">
            {instancia.nombre} · {instancia.razonSocialEmpresa} ·{' '}
            <span className="font-medium">{instancia.estado.replace(/_/g, ' ')}</span>
          </p>
        )}
      </div>

      {error && <div className="card border-red-200 bg-red-50 text-red-700 text-sm">{error}</div>}

      {/* Carta de presentación */}
      <div className="card space-y-3">
        <h2 className="font-semibold text-gray-800">Carta de presentación</h2>
        <p className="text-xs text-gray-500">Formatos admitidos: PDF, JPG, PNG</p>
        <input
          ref={refCarta}
          type="file"
          accept=".pdf,.jpg,.jpeg,.png"
          className="hidden"
          onChange={e => {
            const file = e.target.files?.[0]
            if (file) handleSubirDocumento('CARTA_PRESENTACION', file)
            e.target.value = ''
          }}
        />
        <button
          className="btn-secondary"
          onClick={() => refCarta.current?.click()}
          disabled={uploadingDocType !== null || estaDeshabilitado}
        >
          {uploadingDocType === 'CARTA_PRESENTACION' ? 'Subiendo...' : 'Seleccionar archivo (PDF / JPG / PNG)'}
        </button>
      </div>

      {/* Convenio de práctica */}
      <div className="card space-y-3">
        <h2 className="font-semibold text-gray-800">Convenio de práctica</h2>
        <p className="text-xs text-gray-500">Formatos admitidos: PDF, JPG, PNG</p>
        <input
          ref={refConvenio}
          type="file"
          accept=".pdf,.jpg,.jpeg,.png"
          className="hidden"
          onChange={e => {
            const file = e.target.files?.[0]
            if (file) handleSubirDocumento('CONVENIO', file)
            e.target.value = ''
          }}
        />
        <button
          className="btn-secondary"
          onClick={() => refConvenio.current?.click()}
          disabled={uploadingDocType !== null || estaDeshabilitado}
        >
          {uploadingDocType === 'CONVENIO' ? 'Subiendo...' : 'Seleccionar archivo (PDF / JPG / PNG)'}
        </button>
      </div>

      {/* Firmas del convenio */}
      <div className="card space-y-4">
        <div>
          <h2 className="font-semibold text-gray-800">Firmas del convenio</h2>
          <p className="text-xs text-gray-500 mt-1">
            Sube el documento con la firma de cada parte. Al subir el archivo la firma queda registrada automáticamente.
            Formatos admitidos: PDF, JPG, PNG.
          </p>
        </div>

        {/* Inputs ocultos, uno por firma */}
        {TIPOS_FIRMA.map(tipo => (
          <input
            key={tipo}
            ref={firmaRefs[tipo]}
            type="file"
            accept=".pdf,.jpg,.jpeg,.png"
            className="hidden"
            onChange={e => {
              const file = e.target.files?.[0]
              if (file) handleSubirFirma(tipo, file)
              e.target.value = ''
            }}
          />
        ))}

        <div className="grid grid-cols-3 gap-4">
          {TIPOS_FIRMA.map(tipo => {
            const confirmada = firmaConfirmada(tipo)
            const subiendo = uploadingFirmaType === tipo
            return (
              <div
                key={tipo}
                className={`flex flex-col items-center gap-3 p-4 border-2 rounded-xl transition-colors ${
                  confirmada ? 'border-green-400 bg-green-50' : 'border-dashed border-gray-300 bg-white hover:border-cue-primary hover:bg-gray-50'
                }`}
              >
                <div className={`text-3xl ${confirmada ? 'text-green-500' : 'text-gray-300'}`}>
                  {confirmada ? '✓' : '📄'}
                </div>
                <div className="text-center">
                  <p className="text-sm font-semibold text-gray-800">{firmaLabel[tipo]}</p>
                  {confirmada && (
                    <p className="text-xs text-green-700 mt-1 font-medium">Firma registrada</p>
                  )}
                </div>
                {!confirmada && (
                  <button
                    className="w-full text-xs bg-cue-primary text-white px-3 py-2 rounded-lg hover:opacity-90 transition-opacity font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                    onClick={() => firmaRefs[tipo].current?.click()}
                    disabled={uploadingFirmaType !== null || estaDeshabilitado}
                  >
                    {subiendo ? 'Subiendo...' : 'Subir firma'}
                  </button>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Confirmar vinculación */}
      {instancia?.estado === 'ASIGNADA_PENDIENTE_INICIO' && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">Confirmar vinculación → EN_CURSO</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Fecha de inicio *</label>
              <input type="date" className="input-field" value={fechaInicio} onChange={e => setFechaInicio(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Fecha de fin *</label>
              <input type="date" className="input-field" value={fechaFin} onChange={e => setFechaFin(e.target.value)} />
            </div>
          </div>
          {!instancia.docenteAsesorId && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Docente Asesor</label>
              <select className="input-field" value={docenteAsesorId} onChange={e => setDocenteAsesorId(e.target.value)}>
                <option value="">— Sin asignar —</option>
                {docentes.map(d => (
                  <option key={d.id} value={d.id}>{d.nombre}</option>
                ))}
              </select>
            </div>
          )}
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
            <strong>Requiere las tres firmas:</strong> tutor, docente y estudiante registradas para activar EN_CURSO.
          </div>
          <button
            className="btn-primary w-full"
            onClick={handleConfirmar}
            disabled={saving || !firmaTutor || !firmaDocente || !firmaEstudiante}
          >
            {saving ? 'Confirmando...' : 'Confirmar vinculación → EN_CURSO'}
          </button>
        </div>
      )}

      {instancia?.estado === 'EN_CURSO' && (
        <div className="card border-green-200 bg-green-50">
          <p className="text-green-800 font-medium">Práctica EN_CURSO</p>
          <p className="text-sm text-green-600">Inicio: {instancia.fechaInicio} · Fin: {instancia.fechaFin}</p>
        </div>
      )}
    </div>
  )
}
