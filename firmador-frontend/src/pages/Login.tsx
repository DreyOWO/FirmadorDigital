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

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__icon">📄</div>
        <h2 className="auth-card__title">FirmaDigital</h2>
        <p className="auth-card__subtitle">
          Ingresa tus credenciales para acceder al sistema
        </p>

        <form onSubmit={handleSubmit} className="auth-form">
          <label className="auth-label" htmlFor="email">Usuario</label>
          <input
            id="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="email"
            autoComplete="email"
            placeholder="usuario@empresa.com"
            className="auth-input"
          />

          <label className="auth-label" htmlFor="password">Contraseña</label>
          <input
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            className="auth-input"
          />

          {errorMessage ? (
            <div className="auth-error" role="alert">
              {errorMessage}
            </div>
          ) : null}

          <button disabled={loading} className="auth-primary-button">
            {loading ? 'Ingresando...' : 'Iniciar sesión'}
          </button>

        </form>
      </div>
    </div>
  )
}
