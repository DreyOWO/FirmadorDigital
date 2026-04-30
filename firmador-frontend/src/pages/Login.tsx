import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login, getErrorMessage } from '../services/api'
import { ACCESS_TOKEN_KEY, useAuthStore } from '../stores/authStore'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuthenticated)
  const enableGuestMode = useAuthStore((s) => s.enableGuestMode)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setErrorMessage('')

    try {
      const data = await login(email.trim(), password)
      const token = data?.accessToken || data?.access_token

      if (!token) {
        setErrorMessage('La respuesta de autenticación no incluyó un token válido.')
        return
      }

      localStorage.setItem(ACCESS_TOKEN_KEY, token)
      setAuth(true)
      navigate('/dashboard')
    } catch (err) {
      console.error(err)
      setErrorMessage(getErrorMessage(err, 'No fue posible iniciar sesión.'))
    } finally {
      setLoading(false)
    }
  }

  const handleGuestAccess = () => {
    setErrorMessage('')
    enableGuestMode()
    navigate('/dashboard')
  }

  return (
    <div className="max-w-md mx-auto mt-20 bg-white p-6 rounded shadow">
      <h2 className="text-2xl font-semibold mb-4">Iniciar sesión</h2>
      <p className="text-gray-600 mb-4">
        Usa tu cuenta si el backend está disponible, o entra en modo invitado para navegar la interfaz.
      </p>

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <input
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          type="email"
          autoComplete="email"
          placeholder="email"
          className="p-2 border rounded"
        />
        <input
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          type="password"
          autoComplete="current-password"
          placeholder="password"
          className="p-2 border rounded"
        />

        {errorMessage ? (
          <div className="text-sm text-red-600" role="alert">
            {errorMessage}
          </div>
        ) : null}

        <button disabled={loading} className="mt-2 bg-blue-600 text-white p-2 rounded">
          {loading ? 'Ingresando...' : 'Ingresar'}
        </button>

        <button type="button" onClick={handleGuestAccess} className="p-2 border rounded">
          Continuar como invitado
        </button>
      </form>
    </div>
  )
}
