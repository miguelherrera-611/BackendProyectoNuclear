import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import type { InstanciaPracticaResponse } from '../../types'
import api from '../../services/api'
import type { ApiResponse } from '../../types'

export default function VinculacionPage() {
  const { instanciaId } = useParams<{ instanciaId: string }>()
  const navigate = useNavigate()
  const [instancia, setInstancia] = useState<InstanciaPracticaResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [uploadingDocType, setUploadingDocType] = useState<string | null>(null)

  // Confirmación vinculación
  const [fechaInicio, setFechaInicio] = useState('')
  const [fechaFin, setFechaFin] = useState('')
  const [firmaTutor, setFirmaTutor] = useState(false)
  const [firmaDocente, setFirmaDocente] = useState(false)
  const [firmaEstudiante, setFirmaEstudiante] = useState(false)
  const [docenteAsesorId, setDocenteAsesorId] = useState('')

  const cargar = async () => {
    if (!instanciaId) return
    setLoading(true)
    try {
      const res = await api.get<ApiResponse<InstanciaPracticaResponse>>(`/v1/vinculaciones/${instanciaId}`)
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

  useEffect(() => { cargar() }, [instanciaId])

  const handleSubirDocumento = async (tipo: string) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.pdf,.jpg,.jpeg,.png'
    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0]
      if (!file) return
      setUploadingDocType(tipo)
      const form = new FormData()
      form.append('archivo', file)
      try {
        await api.post(`/v1/documentos-practica/${instanciaId}?tipo=${tipo}`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        setError('')
      } catch {
        setError('Error al subir el documento.')
      } finally {
        setUploadingDocType(null)
      }
    }
    input.click()
  }

  const handleRegistrarFirma = async (tipo: string) => {
    setSaving(true)
    try {
      const res = await api.patch<ApiResponse<InstanciaPracticaResponse>>(`/v1/vinculaciones/${instanciaId}/firmas/${tipo}`)
      setInstancia(res.data.datos!)
    } catch {
      setError('Error al registrar la firma.')
    } finally {
      setSaving(false)
    }
  }

  const handleConfirmar = async () => {
    if (!fechaInicio || !fechaFin) {
      setError('Las fechas de inicio y fin son obligatorias.')
      return
    }
    setSaving(true)
    setError('')
    try {
      await api.patch(`/v1/vinculaciones/${instanciaId}/confirmar`, {
        fechaInicio,
        fechaFin,
        firmaTutor,
        firmaDocente,
        firmaEstudiante,
        docenteAsesorId: docenteAsesorId ? Number(docenteAsesorId) : null,
      })
      navigate('/asignaciones')
    } catch (e: any) {
      setError(e?.response?.data?.mensaje ?? 'Error al confirmar la vinculación.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="flex justify-center py-16 text-gray-400">Cargando...</div>

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

      {/* Documentos */}
      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-800">GPE-162 — Carta de presentación</h2>
        <button
          className="btn-secondary"
          onClick={() => handleSubirDocumento('CARTA_PRESENTACION')}
          disabled={uploadingDocType !== null || instancia?.estado === 'FINALIZADA' || instancia?.estado === 'CANCELADA'}
        >
          {uploadingDocType === 'CARTA_PRESENTACION' ? 'Subiendo...' : 'Subir carta (PDF / JPG / PNG)'}
        </button>
      </div>

      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-800">GPE-163 — Convenio de práctica</h2>
        <button
          className="btn-secondary"
          onClick={() => handleSubirDocumento('CONVENIO')}
          disabled={uploadingDocType !== null || instancia?.estado === 'FINALIZADA' || instancia?.estado === 'CANCELADA'}
        >
          {uploadingDocType === 'CONVENIO' ? 'Subiendo...' : 'Subir convenio (PDF)'}
        </button>
      </div>

      {/* Firmas */}
      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-800">Firmas del convenio</h2>
        <div className="grid grid-cols-3 gap-3">
          {(['TUTOR', 'DOCENTE', 'ESTUDIANTE'] as const).map(tipo => {
            const confirmada = tipo === 'TUTOR' ? instancia?.firmaTutor : tipo === 'DOCENTE' ? instancia?.firmaDocente : instancia?.firmaEstudiante
            return (
              <div key={tipo} className={`p-3 border rounded-lg text-center ${confirmada ? 'border-green-300 bg-green-50' : 'border-gray-200'}`}>
                <div className="text-lg mb-1">{confirmada ? '✓' : '○'}</div>
                <div className="text-sm font-medium text-gray-700">{tipo}</div>
                {!confirmada && (
                  <button
                    className="mt-2 text-xs btn-secondary"
                    onClick={() => handleRegistrarFirma(tipo)}
                    disabled={saving}
                  >
                    Registrar
                  </button>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Confirmación vinculación */}
      {instancia?.estado === 'ASIGNADA_PENDIENTE_INICIO' && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-800">GPE-164 — Confirmar vinculación y activar EN_CURSO</h2>
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
              <label className="block text-sm font-medium text-gray-700 mb-1">ID Docente Asesor (si no fue asignado antes)</label>
              <input type="number" className="input-field" placeholder="ID del docente" value={docenteAsesorId} onChange={e => setDocenteAsesorId(e.target.value)} />
            </div>
          )}
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
            <strong>Requiere las tres firmas:</strong> tutor, docente y estudiante confirmadas para activar EN_CURSO.
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
