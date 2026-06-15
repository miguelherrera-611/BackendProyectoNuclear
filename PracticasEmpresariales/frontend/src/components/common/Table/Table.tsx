import { ReactNode } from 'react'

interface TableProps {
  headers: (string | ReactNode)[]
  loading?: boolean
  empty?: boolean
  emptyMessage?: string
  emptyIcon?: string
  children: ReactNode
}

export function Table({
  headers,
  loading = false,
  empty = false,
  emptyMessage = 'No hay datos para mostrar.',
  emptyIcon = '📋',
  children,
}: TableProps) {
  return (
    <div className="card overflow-x-auto p-0">
      <table className="w-full text-sm">
        <thead className="bg-gray-50 border-b border-gray-200">
          <tr>
            {headers.map((h, i) => (
              <th key={i} className="text-left px-4 py-3 text-gray-600 font-semibold whitespace-nowrap">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={headers.length} className="py-12">
                <div className="flex justify-center">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cue-primary" />
                </div>
              </td>
            </tr>
          ) : empty ? (
            <tr>
              <td colSpan={headers.length} className="py-16 text-center">
                <div className="text-gray-300 text-4xl mb-3">{emptyIcon}</div>
                <p className="text-gray-400 text-sm">{emptyMessage}</p>
              </td>
            </tr>
          ) : (
            children
          )}
        </tbody>
      </table>
    </div>
  )
}
