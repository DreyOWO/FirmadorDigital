import { Link } from 'react-router-dom'
import { Eye, Plus, Search, Filter, FileText } from 'lucide-react'

type DashboardDocument = {
  id: string
  title: string
  date: string
  status: 'Pendiente' | 'En Proceso' | 'Firmado'
  progress: string
  progressValue: number
  signers: string[]
}

const stats = [
  { label: 'Pendientes', value: 12, tone: 'warning' },
  { label: 'En Proceso', value: 8, tone: 'info' },
  { label: 'Firmados', value: 45, tone: 'success' },
  { label: 'Total', value: 65, tone: 'neutral' },
]

const documents: DashboardDocument[] = [
  {
    id: '1',
    title: 'Contrato de Servicios 2026',
    date: '28/04/2026',
    status: 'Pendiente',
    progress: '0/2',
    progressValue: 0,
    signers: ['UA', 'MG'],
  },
  {
    id: '2',
    title: 'Acuerdo de Confidencialidad',
    date: '27/04/2026',
    status: 'En Proceso',
    progress: '1/3',
    progressValue: 33,
    signers: ['UA', 'MG', 'CR'],
  },
  {
    id: '3',
    title: 'Propuesta Comercial Q2',
    date: '26/04/2026',
    status: 'Firmado',
    progress: '2/2',
    progressValue: 100,
    signers: ['UA', 'MG'],
  },
  {
    id: '4',
    title: 'Addendum Contractual',
    date: '25/04/2026',
    status: 'Firmado',
    progress: '2/2',
    progressValue: 100,
    signers: ['CR', 'AL'],
  },
  {
    id: '5',
    title: 'Carta de Intención Proyecto Alpha',
    date: '24/04/2026',
    status: 'En Proceso',
    progress: '2/4',
    progressValue: 50,
    signers: ['UA', 'MG', 'CR', 'AL'],
  },
]

function getStatusClass(status: DashboardDocument['status']) {
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
              <input type="text" placeholder="Buscar documentos..." />
            </label>

            <button type="button" className="documents-filter-button">
              <Filter size={16} />
              <span>Todos los estados</span>
            </button>
          </div>
        </div>

        <div className="documents-table">
          <div className="documents-table__head">
            <span>Documento</span>
            <span>Fecha</span>
            <span>Estado</span>
            <span>Progreso</span>
            <span>Firmantes</span>
            <span>Acciones</span>
          </div>

          {documents.map((document) => (
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
      </section>
    </div>
  )
}
