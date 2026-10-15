import { createContext, useContext, useState } from 'react'
import { apiFetch, clearSession, loadSession, saveSession } from './api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(loadSession)
  const [isNew, setIsNew] = useState(false)

  async function login(username, password) {
    const data = await apiFetch('/auth/login', {
      method: 'POST',
      body: { username, password },
      auth: false,
    })
    const next = { token: data.token, user: data.user }
    saveSession(next)
    setSession(next)
  }

  async function register(username, password) {
    const user = await apiFetch('/auth/register', {
      method: 'POST',
      body: { username, password },
      auth: false,
    })
    setIsNew(true)
    return user
  }

  function finishOnboarding() {
    setIsNew(false)
  }

  function logout() {
    clearSession()
    setSession(null)
    setIsNew(false)
  }

  return (
    <AuthContext.Provider value={{ user: session?.user ?? null, isNew, login, register, logout, finishOnboarding }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
