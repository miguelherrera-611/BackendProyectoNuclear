export const formatFecha = (iso: string | undefined, locale = 'es-CO'): string => {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(locale, { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export const formatFechaHora = (iso: string | undefined, locale = 'es-CO'): string => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(locale)
}

export const formatEstado = (estado: string): string =>
  estado.replace(/_/g, ' ')

export const extractApiError = (err: unknown, fallback = 'Ocurrió un error.'): string =>
  (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje ?? fallback
