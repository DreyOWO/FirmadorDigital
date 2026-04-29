import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './stores/authStore'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import PendingDocuments from './pages/PendingDocuments'
import DocumentViewer from './pages/DocumentViewer'
import AdminPanel from './pages/AdminPanel'
import WorkflowManager from './pages/WorkflowManager'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  const { isAuthenticated } = useAuthStore()

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" /> : <Login />}
      />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/pending" element={<PendingDocuments />} />
        <Route path="/documents/:id/view" element={<DocumentViewer />} />
        <Route path="/admin" element={<AdminPanel />} />
        <Route path="/workflows" element={<WorkflowManager />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" />} />
    </Routes>
  )
}

export default App
