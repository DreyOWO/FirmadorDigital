import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { FileText } from 'lucide-react'
import { getPendingDocuments } from '../services/api'
import DocumentCard from '../components/DocumentCard'

export default function PendingDocuments() {
  const navigate = useNavigate()
  const { data: documents, isLoading } = useQuery({
    queryKey: ['pending-documents'],
    queryFn: getPendingDocuments,
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Documentos Pendientes de Firma
        </h1>
        <p className="text-gray-600">Tienes {documents?.length || 0} documentos esperando tu firma</p>
      </div>

      {documents?.length === 0 ? (
        <div className="text-center py-12">
          <FileText className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">No hay documentos pendientes</h3>
          <p className="mt-1 text-sm text-gray-500">Todos tus documentos han sido firmados</p>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {documents?.map((doc: any) => (
            <DocumentCard key={doc.id} document={doc} onClick={() => navigate(`/documents/${doc.id}/view`)} />
          ))}
        </div>
      )}
    </div>
  )
}
