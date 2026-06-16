interface PaginationProps {
  page: number
  totalPages: number
  totalElements?: number
  onPageChange: (page: number) => void
  disabled?: boolean
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
  disabled = false,
}: PaginationProps) {
  const canBack = page > 0
  const canNext = totalPages > 0 && page < totalPages - 1

  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm text-gray-500">
        Página <span className="font-medium">{page + 1}</span> de{' '}
        <span className="font-medium">{totalPages || 1}</span>
        {totalElements !== undefined && (
          <> · <span className="font-medium">{totalElements}</span> registros</>
        )}
      </p>
      <div className="flex gap-2">
        <button
          onClick={() => onPageChange(Math.max(0, page - 1))}
          disabled={!canBack || disabled}
          className="px-3 py-1.5 text-sm font-medium rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          ← Anterior
        </button>
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={!canNext || disabled}
          className="px-3 py-1.5 text-sm font-medium rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          Siguiente →
        </button>
      </div>
    </div>
  )
}
