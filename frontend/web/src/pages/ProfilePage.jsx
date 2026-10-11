import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { useAuth } from '../auth.jsx'
import Avatar from '../components/Avatar.jsx'
import ProfilePreview from '../previews/ProfilePreview.jsx'

function memberSince(createdAt) {
  const day = createdAt.slice(0, 10)
  return new Date(`${day}T00:00:00Z`).toLocaleDateString('en-GB', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  })
}

export default function ProfilePage() {
  const { id } = useParams()
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let ignore = false
    setProfile(null)
    setError(null)
    apiFetch(`/screens/profile/${encodeURIComponent(id)}`)
      .then((data) => {
        if (!ignore) setProfile(data)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setError(err)
      })
    return () => {
      ignore = true
    }
  }, [id, attempt])

  if (error?.status === 404) {
    return (
      <div className="state-message">
        <p className="state-title">User not found</p>
        <p className="state-text">There is no user with this id.</p>
        <Link to="/">Back to Home</Link>
      </div>
    )
  }
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
  if (!profile) return <p className="state-text">Loading profile…</p>

  const own = profile.id === user.id

  return (
    <>
      <section className="card profile-header">
        <Avatar name={profile.username} large />
        <div>
          <h1 className="profile-name">{profile.username}</h1>
          <p className="profile-meta">Member since {memberSince(profile.createdAt)}</p>
        </div>
      </section>
      <ProfilePreview own={own} />
    </>
  )
}
