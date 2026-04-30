import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Eye, Plus, Search, FileText } from 'lucide-react'
import { getPendingDocuments } from '../services/api'
import type { DocumentDTO } from '../types'

type DashboardStatus = 'Pendiente' | 'En Proceso' | 'Firmado'

type DashboardRow = {
  id: string
  title: string
  date: string
  status: DashboardStatus
  progress: string
  progressValue: number
  signers: string[]
}

function normalizeStatus(status?: string): DashboardStatus {
  const value = (status || '').toLowerCase()

  if (value.includes('complete') || value.includes('firmad')) {
    return 'Firmado'
  }

  if (value.includes('progress') || value.includes('proceso')) {
    return 'En Proceso'
  }

  return 'Pendiente'
}

function formatDate(value?: string) {
  if (!value) {
    return 'Sin fecha'
  }

  const parsedDate = new Date(value)
  if (Number.isNaN(parsedDate.getTime())) {
    return 'Sin fecha'
  }

  return new Intl.DateTimeFormat('es-CR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(parsedDate)
}

function buildSignerBadges(document: DocumentDTO) {
  if (document.createdBy) {
    return [document.createdBy.slice(0, 2).toUpperCase()]
  }

  return ['FD']
}

function toDashboardRow(document: DocumentDTO): DashboardRow {
  const status = normalizeStatus(document.status)
  const currentStep = Math.max(document.currentStep ?? 1, 1)
  const totalSteps = Math.max(document.currentStep ?? 1, 1)
  const progressValue = status === 'Firmado' ? 100 : status === 'En Proceso' ? 50 : 15

  return {
    id: document.id,
    title: document.title?.trim() || 'Documento sin título',
    date: formatDate(document.updatedAt || document.createdAt),
    status,
    progress: `${status === 'Firmado' ? totalSteps : Math.min(currentStep, totalSteps)}/${totalSteps}`,
    progressValue,
    signers: buildSignerBadges(document),
  }
}

function getStatusClass(status: DashboardStatus) {
  switch (status) {
    case 'Pendiente':
      return 'dashboard-status dashboard-status--warning'
    case 'En Proceso':
      return 'dashboard-status dashboard-status--info'
    case 'Firmado':
      return 'dashboard-status dashboard-status--success'
    default:
      return 'dashboard-status'
  }
}

function getStatToneClass(tone: string) {
  return `dashboard-stat__badge dashboard-stat__badge--${tone}`
}

export default function Dashboard() {
  const [searchTerm, setSearchTerm] = useState('')

  const {
    data: documents = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['pending-documents-dashboard'],
    queryFn: getPendingDocuments,
  })

  const dashboardRows = useMemo(() => documents.map(toDashboardRow), [documents])

  const filteredRows = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase()

    if (!normalizedSearch) {
      return dashboardRows
    }

    return dashboardRows.filter((document) =>
      document.title.toLowerCase().includes(normalizedSearch)
    )
  }, [dashboardRows, searchTerm])

  const stats = useMemo(() => {
    const pending = dashboardRows.filter((item) => item.status === 'Pendiente').length
    const inProgress = dashboardRows.filter((item) => item.status === 'En Proceso').length
    const signed = dashboardRows.filter((item) => item.status === 'Firmado').length
    const total = dashboardRows.length

    return [
      { label: 'Pendientes', value: pending, tone: 'warning' },
      { label: 'En Proceso', value: inProgress, tone: 'info' },
      { label: 'Firmados', value: signed, tone: 'success' },
      { label: 'Total', value: total, tone: 'neutral' },
    ]
  }, [dashboardRows])

  return (
    <div className="documents-page">
      <div className="documents-page__header">
        <div>
          <h1 className="documents-page__title">Documentos</h1>
          <p className="documents-page__subtitle">Gestiona todos tus documentos para firma digital</p>
        </div>

        <Link to="/subir-documento" className="documents-page__upload-button">
          <Plus size={16} />
          <span>Subir Documento</span>
        </Link>
      </div>

      <section className="dashboard-stats">
        {stats.map((stat) => (
          <article key={stat.label} className="dashboard-stat">
            <div>
              <p className="dashboard-stat__label">{stat.label}</p>
              <p className="dashboard-stat__value">{stat.value}</p>
            </div>
            <div className={getStatToneClass(stat.tone)}>{stat.value}</div>
          </article>
        ))}
      </section>

      <section className="documents-table-card">
        <div className="documents-table-card__header">
          <h2>Lista de Documentos</h2>

          <div className="documents-table-card__controls">
            <label className="documents-search">
              <Search size={16} />
              <input
                type="text"
                placeholder="Buscar documentos..."
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </label>
          </div>
        </div>

        {isLoading ? (
          <div className="documents-empty-state">
            <p className="documents-empty-state__title">Cargando documentos...</p>
            <p className="documents-empty-state__subtitle">Espera un momento mientras obtenemos la información.</p>
          </div>
        ) : isError ? (
          <div className="documents-empty-state">
            <p className="documents-empty-state__title">No fue posible cargar los documentos</p>
            <p className="documents-empty-state__subtitle">Verifica la conexión con el sistema e inténtalo de nuevo.</p>
          </div>
        ) : filteredRows.length === 0 ? (
          <div className="documents-empty-state">
            <p className="documents-empty-state__title">No hay documentos para trabajar</p>
            <p className="documents-empty-state__subtitle">
              {dashboardRows.length === 0
                ? 'Todavía no tienes documentos asignados o pendientes de firma.'
                : 'No encontramos documentos que coincidan con tu búsqueda.'}
            </p>
          </div>
        ) : (
          <div className="documents-table">
            <div className="documents-table__head">
              <span>Documento</span>
              <span>Fecha</span>
              <span>Estado</span>
              <span>Progreso</span>
              <span>Firmantes</span>
              <span>Acciones</span>
            </div>

            {filteredRows.map((document) => (
              <div key={document.id} className="documents-table__row">
                <div className="documents-table__document">
                  <span className="documents-table__document-icon">
                    <FileText size={18} />
                  </span>
                  <span>{document.title}</span>
                </div>

                <span>{document.date}</span>
                <span>
                  <span className={getStatusClass(document.status)}>{document.status}</span>
                </span>

                <div className="documents-progress">
                  <div className="documents-progress__bar">
                    <span style={{ width: `${document.progressValue}%` }} />
                  </div>
                  <span>{document.progress}</span>
                </div>

                <div className="documents-signers">
                  {document.signers.map((signer) => (
                    <span key={`${document.id}-${signer}`} className="documents-signers__badge">
                      {signer}
                    </span>
                  ))}
                </div>

                <Link to={`/documents/${document.id}/view`} className="documents-view-link">
                  <Eye size={16} />
                  <span>Ver</span>
                </Link>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

// Made with Bob
