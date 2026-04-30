import { Outlet, Link, useNavigate } from 'react-router-dom'
import { LogOut, FileText } from 'lucide-react'
import { useAuthStore } from '../stores/authStore'

export default function Layout() {
  const navigate = useNavigate()
  const logout = useAuthStore((state) => state.logout)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-shell min-h-screen">
      <header className="app-header">
        <div className="container app-header__inner">
          <Link to="/dashboard" className="brand-link">
            <span className="brand-link__icon">
              <FileText size={18} />
            </span>
            <span>FirmaDigital</span>
          </Link>

          <nav className="app-nav">
            <Link to="/pending" className="app-nav__link">Pendientes</Link>
            <Link to="/workflows" className="app-nav__link">Workflows</Link>
            <button type="button" className="logout-button" onClick={handleLogout}>
              <LogOut size={16} />
              <span>Log off</span>
            </button>
          </nav>
        </div>
      </header>

      <main className="container app-main">
        <Outlet />
      </main>
    </div>
  )
}
