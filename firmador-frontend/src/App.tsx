import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { ACCESS_TOKEN_KEY, DEV_GUEST_TOKEN, useAuthStore } from './stores/authStore'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import PendingDocuments from './pages/PendingDocuments'
import DocumentViewer from './pages/DocumentViewer'
import AdminPanel from './pages/AdminPanel'
import WorkflowManager from './pages/WorkflowManager'
import UploadDocument from './pages/UploadDocument'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  const { isAuthenticated } = useAuthStore()

  useEffect(() => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (token === DEV_GUEST_TOKEN) {
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      window.location.replace('/login')
    }
  }, [])

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" /> : <Login />}
      />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/dashboard" element={isAuthenticated ? <Dashboard /> : <Navigate to="/login" />} />
        <Route path="/pending" element={<PendingDocuments />} />
        <Route path="/documents/:id/view" element={<DocumentViewer />} />
        <Route path="/admin" element={<AdminPanel />} />
        <Route path="/workflows" element={<WorkflowManager />} />
        <Route path="/subir-documento" element={<UploadDocument />} />
      </Route>

      <Route path="/" element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} />} />
    </Routes>
  )
}

export default App
