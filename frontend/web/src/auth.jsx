import { createContext, useContext, useState } from 'react'
import { apiFetch, clearSession, loadSession, saveSession } from './api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(loadSession)

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

  function register(username, password) {
    return apiFetch('/auth/register', {
      method: 'POST',
      body: { username, password },
      auth: false,
    })
  }

  function logout() {
    clearSession()
    setSession(null)
  }

  return (
    <AuthContext.Provider value={{ user: session?.user ?? null, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
