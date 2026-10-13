import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { fieldMessages } from '../api.js'
import { useAuth } from '../auth.jsx'

function validate(username, password) {
  const errors = {}
  if (username && !/^[A-Za-z0-9_-]{3,32}$/.test(username)) {
    errors.username = "Must have 3-32 characters: letters, digits, '_' or '-'"
  }
  if (password && (password.length < 8 || password.length > 72)) {
    errors.password = 'size must be between 8 and 72'
  } else if (new TextEncoder().encode(password).length > 72) {
    errors.password = 'must be at most 72 bytes in UTF-8'
  }
  return errors
}

export default function RegisterPage() {
  const { register, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const live = validate(username, password)
  const usernameError = fieldErrors.username ?? live.username
  const passwordError = fieldErrors.password ?? live.password

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    setFormError(null)
    setFieldErrors({})
    try {
      await register(username, password)
    } catch (err) {
      if (err.status === 400 && err.fields.length > 0) {
        setFieldErrors(fieldMessages(err))
      } else if (err.status === 409) {
        setFieldErrors({ username: 'This username is already taken.' })
      } else {
        setFormError('Something went wrong, try again.')
      }
      setSubmitting(false)
      return
    }
    try {
      await login(username, password)
      navigate('/welcome', { replace: true })
    } catch {
      navigate('/login', { replace: true })
    }
    setSubmitting(false)
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <span className="wordmark">
          Plain<span>Sight</span>
        </span>
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">Pick a username and a password to get started.</p>
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
              onChange={(e) => {
                setUsername(e.target.value)
                setFieldErrors({ ...fieldErrors, username: undefined })
              }}
              required
              autoFocus
            />
            {usernameError ? (
              <span className="field-error">{usernameError}</span>
            ) : (
              <span className="field-hint">3–32 characters: letters, digits, _ or -</span>
            )}
          </label>
          <label className="field">
            <span className="field-label">Password</span>
            <input
              className="field-input"
              name="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                setFieldErrors({ ...fieldErrors, password: undefined })
              }}
              required
            />
            {passwordError ? (
              <span className="field-error">{passwordError}</span>
            ) : (
              <span className="field-hint">8–72 characters</span>
            )}
          </label>
          <button className="btn btn-primary btn-block" type="submit" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <p className="auth-switch">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  )
}
