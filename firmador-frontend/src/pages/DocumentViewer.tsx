import { useParams } from 'react-router-dom'
import PDFViewer from '../components/PDFViewer'
import { getDocumentViewUrl } from '../services/api'
import { useMemo } from 'react'

export default function DocumentViewer() {
  const { id } = useParams()
  const url = useMemo(() => (id ? getDocumentViewUrl(id) : ''), [id])

  return (
    <div className="h-[80vh]">
      {id ? <PDFViewer documentUrl={url as unknown as string} /> : <div>Documento no encontrado</div>}
    </div>
  )
}
