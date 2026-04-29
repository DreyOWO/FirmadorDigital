import { Outlet, Link } from 'react-router-dom'

export default function Layout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow p-4">
        <div className="container mx-auto flex items-center justify-between">
          <Link to="/dashboard" className="font-bold">Firmador</Link>
          <nav>
            <Link to="/pending" className="mr-4">Pendientes</Link>
            <Link to="/workflows">Workflows</Link>
          </nav>
        </div>
      </header>
      <main className="container mx-auto p-4">
        <Outlet />
      </main>
    </div>
  )
}
