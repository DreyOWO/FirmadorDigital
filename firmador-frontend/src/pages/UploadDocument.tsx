import { ChangeEvent, FormEvent, useMemo, useState } from 'react'
import { ArrowLeft, Upload, FileText } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { uploadDocument } from '../services/api'

export default function UploadDocument() {
  const navigate = useNavigate()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [workflowId, setWorkflowId] = useState('default-workflow')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const fileLabel = useMemo(() => {
    if (!selectedFile) {
      return 'Arrastra tu archivo PDF aquí o haz clic para seleccionar'
    }

    return `${selectedFile.name} · ${(selectedFile.size / 1024 / 1024).toFixed(2)} MB`
  }, [selectedFile])

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null
    setSelectedFile(file)

    if (file && !title.trim()) {
      const normalizedName = file.name.replace(/\.pdf$/i, '')
      setTitle(normalizedName)
    }
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setErrorMessage('')

    if (!selectedFile) {
      setErrorMessage('Selecciona un archivo PDF antes de continuar.')
      return
    }

    if (!title.trim()) {
      setErrorMessage('Ingresa el nombre del documento.')
      return
    }

    try {
      setIsSubmitting(true)
      await uploadDocument(selectedFile, workflowId, title.trim(), description.trim() || undefined)
      navigate('/dashboard')
    } catch (error) {
      console.error(error)
      setErrorMessage('No fue posible subir el documento con la configuración actual.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="upload-page">
      <Link to="/dashboard" className="upload-page__back-link">
        <ArrowLeft size={16} />
        <span>Volver a Documentos</span>
      </Link>

      <div className="upload-page__heading">
        <h1>Subir Documento</h1>
        <p>Carga un documento PDF y asigna los firmantes</p>
      </div>

      <form className="upload-form" onSubmit={handleSubmit}>
        <section className="upload-card">
          <div className="upload-card__header">
            <h2>Archivo del Documento</h2>
            <p>Arrastra y suelta un archivo PDF o haz clic para seleccionar</p>
          </div>

          <label className="upload-dropzone">
            <input
              type="file"
              accept="application/pdf,.pdf"
              onChange={handleFileChange}
              className="upload-dropzone__input"
            />

            <div className="upload-dropzone__icon">
              {selectedFile ? <FileText size={34} /> : <Upload size={34} />}
            </div>

            <p className="upload-dropzone__title">{fileLabel}</p>
            <p className="upload-dropzone__hint">Solo archivos PDF hasta 10MB</p>
          </label>
        </section>

        <section className="upload-card">
          <div className="upload-card__header">
            <h2>Información del Documento</h2>
            <p>Proporciona los detalles del documento</p>
          </div>

          <div className="upload-form__fields">
            <label className="upload-field">
              <span>Nombre del Documento *</span>
              <input
                type="text"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="Ej: Contrato de Servicios 2026"
              />
            </label>

            <label className="upload-field">
              <span>Descripción (opcional)</span>
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Breve descripción del documento..."
                rows={4}
              />
            </label>

            <label className="upload-field">
              <span>Workflow</span>
              <select value={workflowId} onChange={(event) => setWorkflowId(event.target.value)}>
                <option value="default-workflow">Workflow Predeterminado</option>
                <option value="legal-review">Revisión Legal</option>
                <option value="commercial-approval">Aprobación Comercial</option>
              </select>
            </label>
          </div>

          {errorMessage ? <div className="upload-form__error">{errorMessage}</div> : null}

          <div className="upload-form__actions">
            <Link to="/dashboard" className="upload-form__secondary-action">
              Cancelar
            </Link>
            <button type="submit" className="upload-form__primary-action" disabled={isSubmitting}>
              {isSubmitting ? 'Subiendo...' : 'Subir Documento'}
            </button>
          </div>
        </section>
      </form>
    </div>
  )
}

// Made with Bob
