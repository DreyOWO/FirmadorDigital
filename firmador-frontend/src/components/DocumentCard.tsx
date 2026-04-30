import type { KeyboardEvent } from 'react'
import type { DocumentDTO } from '../types'

type DocumentCardProps = {
  document: DocumentDTO
  onClick?: () => void
}

export default function DocumentCard({ document, onClick }: DocumentCardProps) {
  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (!onClick) {
      return
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onClick()
    }
  }

  return (
    <div
      className="border rounded-lg p-4 bg-white shadow-sm cursor-pointer"
      onClick={onClick}
      onKeyDown={handleKeyDown}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : -1}
    >
      <h3 className="font-semibold text-lg">{document.title || 'Documento sin título'}</h3>
      <p className="text-sm text-gray-600 truncate">{document.description || 'Sin descripción'}</p>
      <div className="mt-4 flex items-center justify-between">
        <div className="text-xs text-gray-500">{document.status || 'pendiente'}</div>
        <button type="button" className="text-sm px-3 py-1 bg-blue-600 text-white rounded">
          Ver
        </button>
      </div>
    </div>
  )
}
