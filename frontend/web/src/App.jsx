import { useEffect } from 'react'
import { Link, NavLink, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom'
import { apiFetch } from './api.js'
import { useAuth } from './auth.jsx'
import Avatar from './components/Avatar.jsx'
import Logo from './components/Logo.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import HomePage from './pages/HomePage.jsx'
import SocialPage from './pages/SocialPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import WelcomePage from './pages/WelcomePage.jsx'
import MoviePage from './pages/MoviePage.jsx'
import WatchlistPage from './pages/WatchlistPage.jsx'
import FeedPage from './pages/FeedPage.jsx'

function RequireAuth() {
  const { user } = useAuth()
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

function PublicOnly() {
  const { user, isNew } = useAuth()
  return user ? <Navigate to={isNew ? '/welcome' : '/'} replace /> : <Outlet />
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
            <Logo />
            <span>Plain</span>Sight
          </Link>
          <nav className="nav-tabs">
            <NavLink className="nav-tab" to="/" end>Home</NavLink>
            <NavLink className="nav-tab" to="/feed">Feed</NavLink>
            <NavLink className="nav-tab" to="/watchlist">Watchlist</NavLink>
            <NavLink className="nav-tab" to="/social">Social</NavLink>
          </nav>
          <div className="nav-end">
            <button className="btn btn-ghost" type="button" onClick={handleLogout}>
              Log out
            </button>
            <NavLink className="nav-avatar" to={`/profile/${user.id}`} aria-label="Your profile" title={user.username}>
              <Avatar name={user.username} />
            </NavLink>
          </div>
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
        <Route path="/welcome" element={<WelcomePage />} />
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/feed" element={<FeedPage />} />
          <Route path="/watchlist" element={<WatchlistPage />} />
          <Route path="/movie/:id" element={<MoviePage />} />
          <Route path="/social" element={<SocialPage />} />
          <Route path="/profile/:id" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
