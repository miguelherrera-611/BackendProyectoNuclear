import { ReactNode } from 'react'
import { Input } from '../Input/Input'

interface SearchConfig {
  label?: string
  placeholder?: string
  value: string
  onChange: (value: string) => void
}

interface ListFiltersProps {
  search?: SearchConfig
  children?: ReactNode
  summary?: ReactNode
  onClear?: () => void
  clearLabel?: string
  className?: string
}

export function ListFilters({
  search,
  children,
  summary,
  onClear,
  clearLabel = 'Limpiar filtros',
  className = '',
}: ListFiltersProps) {
  return (
    <div className={`card py-3 flex flex-col gap-3 lg:flex-row lg:flex-wrap lg:items-end ${className}`}>
      {search && (
        <div className="w-full lg:max-w-sm">
          <Input
            label={search.label}
            placeholder={search.placeholder}
            value={search.value}
            onChange={e => search.onChange(e.target.value)}
          />
        </div>
      )}

      {children && <div className="flex flex-col sm:flex-row gap-3 flex-wrap items-end">{children}</div>}

      {(summary || onClear) && (
        <div className="flex items-center gap-3 lg:ml-auto">
          {summary && <div className="text-sm text-gray-500 whitespace-nowrap">{summary}</div>}
          {onClear && (
            <button className="text-sm text-cue-primary hover:underline whitespace-nowrap" onClick={onClear} type="button">
              {clearLabel}
            </button>
          )}
        </div>
      )}
    </div>
  )
}

