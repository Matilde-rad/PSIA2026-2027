import { useEffect } from 'react'
import { Link, NavLink, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom'
import { apiFetch } from './api.js'
import { useAuth } from './auth.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import HomePage from './pages/HomePage.jsx'
import SocialPage from './pages/SocialPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'

function RequireAuth() {
  const { user } = useAuth()
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

function PublicOnly() {
  const { user } = useAuth()
  return user ? <Navigate to="/" replace /> : <Outlet />
}

function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    apiFetch('/me').catch(() => {})
  }, [])

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app">
      <header className="nav">
        <div className="nav-inner">
          <Link className="wordmark" to="/">
            cine<span>mate</span>
          </Link>
          <nav className="nav-tabs">
            <NavLink className="nav-tab" to="/" end>Home</NavLink>
            <NavLink className="nav-tab" to="/social">Social</NavLink>
            <NavLink className="nav-tab" to={`/profile/${user.id}`}>Profile</NavLink>
          </nav>
          <button className="btn btn-ghost nav-logout" type="button" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route element={<PublicOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/social" element={<SocialPage />} />
          <Route path="/profile/:id" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
