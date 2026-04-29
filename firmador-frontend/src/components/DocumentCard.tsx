import React from 'react'

export default function DocumentCard({ document, onClick }: { document: any; onClick?: () => void }) {
  return (
    <div className="border rounded-lg p-4 bg-white shadow-sm cursor-pointer" onClick={onClick}>
      <h3 className="font-semibold text-lg">{document.title || 'Documento sin título'}</h3>
      <p className="text-sm text-gray-600 truncate">{document.description}</p>
      <div className="mt-4 flex items-center justify-between">
        <div className="text-xs text-gray-500">{document.status || 'pendiente'}</div>
        <button className="text-sm px-3 py-1 bg-blue-600 text-white rounded">Ver</button>
      </div>
    </div>
  )
}
