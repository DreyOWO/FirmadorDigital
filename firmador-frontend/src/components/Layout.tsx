import { useEffect, useState } from 'react'
import { Outlet, Link, useNavigate } from 'react-router-dom'
import { LogOut, FileText, Moon, Sun } from 'lucide-react'
import { useAuthStore } from '../stores/authStore'

const THEME_STORAGE_KEY = 'ui-theme'

export default function Layout() {
  const navigate = useNavigate()
  const logout = useAuthStore((state) => state.logout)
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const savedTheme = localStorage.getItem(THEME_STORAGE_KEY)
    return savedTheme === 'dark' ? 'dark' : 'light'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(THEME_STORAGE_KEY, theme)
  }, [theme])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === 'light' ? 'dark' : 'light'))
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
            <Link to="/dashboard" className="app-nav__link">Documentos</Link>
            <Link to="/pending" className="app-nav__link">Pendientes</Link>
            <Link to="/workflows" className="app-nav__link">Workflows</Link>
            <button type="button" className="theme-toggle-button" onClick={toggleTheme}>
              {theme === 'light' ? <Moon size={16} /> : <Sun size={16} />}
              <span>{theme === 'light' ? 'Modo noche' : 'Modo día'}</span>
            </button>
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
