import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { Poster } from '../components/MovieCard.jsx'
import WatchButtons from '../components/WatchButtons.jsx'
import { prettyTitle } from '../titles.js'

export default function WatchlistPage() {
  const [screen, setScreen] = useState(null)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let ignore = false
    setScreen(null)
    setError(false)
    apiFetch('/screens/watchlist')
      .then((data) => {
        if (!ignore) setScreen(data)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setError(true)
      })
    return () => {
      ignore = true
    }
  }, [attempt])

  function handleChange(item, status) {
    const without = (list) => list.filter((m) => m.movieId !== item.movieId)
    const moved = { ...item, status }
    setScreen({
      toWatch: status === 'to_watch' ? [moved, ...without(screen.toWatch)] : without(screen.toWatch),
      watched: status && status !== 'to_watch' ? [moved, ...without(screen.watched)] : without(screen.watched),
    })
  }

  const header = (
    <div className="page-header">
      <h1 className="page-title">Watchlist</h1>
      <p className="page-subtitle">Movies you want to watch, and what you thought of the ones you did.</p>
    </div>
  )

  if (error) {
    return (
      <>
        {header}
        <div className="state-message">
          <p className="state-title">Something went wrong, try again.</p>
          <button className="btn btn-secondary" onClick={() => setAttempt(attempt + 1)}>
            Try again
          </button>
        </div>
      </>
    )
  }
  if (!screen) {
    return (
      <>
        {header}
        <p className="state-text">Loading your watchlist…</p>
      </>
    )
  }

  return (
    <>
      {header}
      <section className="profile-section">
        <div className="section-header">
          <h2 className="section-title">To watch</h2>
        </div>
        {screen.toWatch.length === 0 ? (
          <div className="state-message">
            <p className="state-title">Nothing to watch yet</p>
            <p className="state-text">Open a movie and press “Add to watchlist” to keep it here.</p>
            <Link className="btn btn-primary" to="/">
              Find movies
            </Link>
          </div>
        ) : (
          <Rows items={screen.toWatch} onChange={handleChange} />
        )}
      </section>
      {screen.watched.length > 0 && (
        <section className="profile-section">
          <div className="section-header">
            <h2 className="section-title">Watched</h2>
          </div>
          <Rows items={screen.watched} onChange={handleChange} />
        </section>
      )}
    </>
  )
}

function Rows({ items, onChange }) {
  return (
    <ul className="watch-list">
      {items.map((item) => (
        <li key={item.movieId} className="watch-row">
          <Link className="thumb" to={`/movie/${item.movieId}`} tabIndex={-1} aria-hidden="true">
            <Poster title={item.title} posterUrl={item.posterUrl} />
          </Link>
          <div className="watch-row-body">
            <Link className="activity-link" to={`/movie/${item.movieId}`}>
              {prettyTitle(item.title)}
            </Link>
            {item.status === 'to_watch' && <p className="user-meta">Watched it? Say how it was.</p>}
          </div>
          <WatchButtons small movieId={item.movieId} status={item.status} onChange={(s) => onChange(item, s)} />
        </li>
      ))}
    </ul>
  )
}
