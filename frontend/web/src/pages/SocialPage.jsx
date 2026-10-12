import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { useAuth } from '../auth.jsx'
import PersonCard from '../components/PersonCard.jsx'

const DEBOUNCE_MS = 250

export default function SocialPage() {
  const { user } = useAuth()
  const [text, setText] = useState('')
  const query = text.trim()
  const searching = query.length >= 2

  return (
    <>
      <div className="page-header">
        <h1 className="page-title">Social</h1>
        <p className="page-subtitle">Find people who like the same movies as you.</p>
      </div>
      <label className="search-box">
        <span className="search-icon" aria-hidden="true">
          ⌕
        </span>
        <input
          className="ask-input"
          type="search"
          value={text}
          maxLength={64}
          placeholder="Search people by name…"
          aria-label="Search people by name"
          onChange={(e) => setText(e.target.value)}
        />
      </label>
      {searching ? <SearchResults query={query} /> : <Suggestions userId={user.id} />}
    </>
  )
}

// keeps the follow button of one person in a list in step with what was just clicked
function withFollow(people, person, now) {
  return people.map((p) => (p.id === person.id ? { ...p, followedByMe: now } : p))
}

function SearchResults({ query }) {
  const [people, setPeople] = useState(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    let ignore = false
    setError(false)
    const timer = setTimeout(() => {
      apiFetch(`/screens/social/search?q=${encodeURIComponent(query)}`)
        .then((data) => {
          if (!ignore) setPeople(data)
        })
        .catch((err) => {
          if (!ignore && err.status !== 401) setError(true)
        })
    }, DEBOUNCE_MS)
    return () => {
      ignore = true
      clearTimeout(timer)
    }
  }, [query])

  if (error) return <p className="state-text">Something went wrong, try again.</p>
  if (!people) return <p className="state-text">Searching…</p>
  if (people.length === 0) {
    return (
      <div className="state-message">
        <p className="state-text">Nobody found with that name.</p>
      </div>
    )
  }
  return (
    <div className="user-grid">
      {people.map((person) => (
        <PersonCard
          key={person.id}
          person={person}
          onFollowChange={(p, now) => setPeople(withFollow(people, p, now))}
        />
      ))}
    </div>
  )
}

function Suggestions({ userId }) {
  const [screen, setScreen] = useState(null)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let ignore = false
    setScreen(null)
    setError(false)
    apiFetch('/screens/social')
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

  if (error) {
    return (
      <div className="state-message">
        <p className="state-title">Something went wrong, try again.</p>
        <button className="btn btn-secondary" onClick={() => setAttempt(attempt + 1)}>
          Try again
        </button>
      </div>
    )
  }
  if (!screen) return <p className="state-text">Finding people…</p>

  return (
    <section className="profile-section">
      <div className="section-header">
        <h2 className="section-title">People with your taste</h2>
      </div>
      {!screen.hasProfile ? (
        <div className="state-message">
          <p className="state-title">Set up your profile first</p>
          <p className="state-text">We match you with people by favorite genres, directors and movies.</p>
          <Link className="btn btn-primary" to={`/profile/${userId}`}>
            Go to your profile
          </Link>
        </div>
      ) : screen.suggestions.length === 0 ? (
        <div className="state-message">
          <p className="state-text">
            Nobody new shares your favorites yet. Search by name above, or add more to your profile.
          </p>
        </div>
      ) : (
        <div className="user-grid">
          {screen.suggestions.map((person) => (
            <PersonCard
              key={person.id}
              person={person}
              onFollowChange={(p, now) =>
                setScreen({ ...screen, suggestions: withFollow(screen.suggestions, p, now) })
              }
            />
          ))}
        </div>
      )}
    </section>
  )
}
