import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { fieldMessages } from '../api.js'
import { useAuth } from '../auth.jsx'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    setFormError(null)
    setFieldErrors({})
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch (err) {
      if (err.status === 400 && err.fields.length > 0) {
        setFieldErrors(fieldMessages(err))
      } else {
        setFormError(
          err.status === 401 ? 'Wrong username or password.' : 'Something went wrong, try again.',
        )
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <span className="wordmark">
          cine<span>mate</span>
        </span>
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">Log in to see what to watch next.</p>
        {params.get('expired') === '1' && (
          <p className="notice">Your session expired, please log in again.</p>
        )}
        {formError && (
          <p className="form-error" role="alert">
            {formError}
          </p>
        )}
        <form className="form" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field-label">Username</span>
            <input
              className="field-input"
              name="username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
            />
            {fieldErrors.username && <span className="field-error">{fieldErrors.username}</span>}
          </label>
          <label className="field">
            <span className="field-label">Password</span>
            <input
              className="field-input"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
          </label>
          <button className="btn btn-primary btn-block" type="submit" disabled={submitting}>
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>
        <p className="auth-switch">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  )
}
