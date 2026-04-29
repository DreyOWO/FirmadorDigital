import { useState } from 'react'
import { Document, Page, pdfjs } from 'react-pdf'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import 'react-pdf/dist/esm/Page/AnnotationLayer.css'
import 'react-pdf/dist/esm/Page/TextLayer.css'

pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`

interface PDFViewerProps {
  documentUrl: string
  disableDownload?: boolean
}

export default function PDFViewer({ documentUrl, disableDownload = true }: PDFViewerProps) {
  const [numPages, setNumPages] = useState<number>(0)
  const [pageNumber, setPageNumber] = useState<number>(1)

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages)
  }

  const handleContextMenu = (e: React.MouseEvent) => {
    if (disableDownload) {
      e.preventDefault()
    }
  }

  return (
    <div className="flex flex-col items-center h-full bg-gray-100" onContextMenu={handleContextMenu} style={{ userSelect: 'none' }}>
      <div className="flex-1 overflow-auto w-full flex justify-center p-4">
        <Document file={documentUrl} onLoadSuccess={onDocumentLoadSuccess} loading={<div className="flex items-center justify-center h-full"><div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div></div>}>
          <Page pageNumber={pageNumber} renderTextLayer={false} renderAnnotationLayer={false} className="shadow-lg" />
        </Document>
      </div>

      <div className="bg-white border-t p-4 w-full flex items-center justify-center gap-4">
        <button onClick={() => setPageNumber(Math.max(1, pageNumber - 1))} disabled={pageNumber <= 1} className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed">
          <ChevronLeft className="h-5 w-5" />
        </button>

        <span className="text-sm text-gray-700">Página {pageNumber} de {numPages}</span>

        <button onClick={() => setPageNumber(Math.min(numPages, pageNumber + 1))} disabled={pageNumber >= numPages} className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed">
          <ChevronRight className="h-5 w-5" />
        </button>
      </div>
    </div>
  )
}
