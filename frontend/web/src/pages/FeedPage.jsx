import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import ActivityList from '../components/ActivityList.jsx'

export default function FeedPage() {
  const [items, setItems] = useState(null)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let ignore = false
    setItems(null)
    setError(false)
    apiFetch('/screens/feed')
      .then((data) => {
        if (!ignore) setItems(data)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setError(true)
      })
    return () => {
      ignore = true
    }
  }, [attempt])

  return (
    <>
      <div className="page-header">
        <h1 className="page-title">Feed</h1>
        <p className="page-subtitle">What the people you follow are watching.</p>
      </div>
      {error ? (
        <div className="state-message">
          <p className="state-title">Something went wrong, try again.</p>
          <button className="btn btn-secondary" onClick={() => setAttempt(attempt + 1)}>
            Try again
          </button>
        </div>
      ) : !items ? (
        <p className="state-text">Loading your feed…</p>
      ) : items.length === 0 ? (
        <div className="state-message">
          <p className="state-title">Nothing here yet</p>
          <p className="state-text">
            When people you follow add a movie to their watchlist or say what they thought of one, it shows up here.
          </p>
          <Link className="btn btn-primary" to="/social">
            Find people to follow
          </Link>
        </div>
      ) : (
        <ActivityList items={items} showUser />
      )}
    </>
  )
}
