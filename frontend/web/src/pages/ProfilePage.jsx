import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { useAuth } from '../auth.jsx'
import ActivityList from '../components/ActivityList.jsx'
import Avatar from '../components/Avatar.jsx'
import FollowButton from '../components/FollowButton.jsx'
import MovieCard from '../components/MovieCard.jsx'
import PeopleDialog from '../components/PeopleDialog.jsx'
import ProfileForm from '../components/ProfileForm.jsx'

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
  const [screen, setScreen] = useState(null)
  const [error, setError] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [editing, setEditing] = useState(false)
  const [list, setList] = useState(null) // 'followers' | 'following' | null

  useEffect(() => {
    let ignore = false
    setScreen(null)
    setError(null)
    setEditing(false)
    setList(null)
    apiFetch(`/screens/profile/${encodeURIComponent(id)}`)
      .then((data) => {
        if (!ignore) setScreen(data)
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
  if (!screen) return <p className="state-text">Loading profile…</p>

  const own = screen.id === user.id
  const profile = screen.profile
  const name = profile?.displayName || screen.username

  // A follow button inside the followers or following dialog changes this page's numbers only
  // when it is about the page's owner or happens on your own page.
  function handleDialogFollow(person, now) {
    const delta = now ? 1 : -1
    if (own) {
      setScreen({ ...screen, following: screen.following + delta })
    } else if (person.id === screen.id) {
      setScreen({ ...screen, followedByMe: now, followers: screen.followers + delta })
    }
  }

  return (
    <>
      <section className="card profile-header">
        <Avatar name={screen.username} large />
        <div className="profile-header-main">
          <h1 className="profile-name">{name}</h1>
          <p className="profile-meta">
            {name !== screen.username && `@${screen.username} · `}
            Member since {memberSince(screen.createdAt)}
          </p>
          <div className="profile-counts">
            <button className="count-button" type="button" onClick={() => setList('followers')}>
              <strong>{screen.followers}</strong> {screen.followers === 1 ? 'follower' : 'followers'}
            </button>
            <button className="count-button" type="button" onClick={() => setList('following')}>
              <strong>{screen.following}</strong> following
            </button>
          </div>
        </div>
        {!own && (
          <FollowButton
            userId={screen.id}
            following={screen.followedByMe}
            onChange={(now) =>
              setScreen({ ...screen, followedByMe: now, followers: screen.followers + (now ? 1 : -1) })
            }
          />
        )}
      </section>

      {list && (
        <PeopleDialog
          key={list}
          userId={screen.id}
          kind={list}
          onClose={() => setList(null)}
          onFollowChange={handleDialogFollow}
        />
      )}

      <section className="profile-section">
        <div className="section-header">
          <h2 className="section-title">{editing ? 'Edit profile' : 'About'}</h2>
          {own && profile && !editing && (
            <button className="btn btn-secondary" type="button" onClick={() => setEditing(true)}>
              Edit profile
            </button>
          )}
        </div>
        {editing ? (
          <div className="card">
            <ProfileForm
              initial={profile}
              initialMovies={screen.favoriteMovies}
              submitLabel="Save changes"
              // reload the screen: the saved profile holds movie ids, the page shows their posters
              onSaved={() => setAttempt(attempt + 1)}
              secondary={
                <button className="btn btn-ghost" type="button" onClick={() => setEditing(false)}>
                  Cancel
                </button>
              }
            />
          </div>
        ) : profile ? (
          <About profile={profile} />
        ) : (
          <div className="state-message">
            {own ? (
              <>
                <p className="state-title">You haven't set up your profile yet</p>
                <p className="state-text">Your recommendations are made from it.</p>
                <button className="btn btn-primary" type="button" onClick={() => setEditing(true)}>
                  Set up profile
                </button>
              </>
            ) : (
              <p className="state-text">{screen.username} hasn't set up a profile yet.</p>
            )}
          </div>
        )}
      </section>

      {!editing && screen.favoriteMovies.length > 0 && (
        <section className="profile-section">
          <div className="section-header">
            <h2 className="section-title">Favorite movies</h2>
          </div>
          <div className="movie-grid">
            {screen.favoriteMovies.map((movie) => (
              <MovieCard key={movie.movieId} movieId={movie.movieId} title={movie.title} posterUrl={movie.posterUrl} />
            ))}
          </div>
        </section>
      )}

      {!editing && <RecentActivity userId={screen.id} own={own} name={name} />}
    </>
  )
}

// Loaded on its own, after the profile: a profile is worth showing even if this fails.
function RecentActivity({ userId, own, name }) {
  const [items, setItems] = useState(null)

  useEffect(() => {
    let ignore = false
    setItems(null)
    apiFetch(`/screens/profile/${encodeURIComponent(userId)}/activity`)
      .then((data) => {
        if (!ignore) setItems(data)
      })
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [userId])

  if (!items) return null
  return (
    <section className="profile-section">
      <div className="section-header">
        <h2 className="section-title">Recent activity</h2>
      </div>
      {items.length === 0 ? (
        <div className="state-message">
          <p className="state-text">
            {own
              ? 'Movies you add to your watchlist, like or dislike show up here.'
              : `${name} hasn't added or rated any movies yet.`}
          </p>
        </div>
      ) : (
        <ActivityList items={items} />
      )}
    </section>
  )
}

function About({ profile }) {
  const lists = [
    ['Favorite genres', profile.favoriteGenres],
    ['Favorite directors', profile.favoriteDirectors],
  ].filter(([, values]) => values.length > 0)

  if (!profile.bio && lists.length === 0) {
    return (
      <div className="card">
        <p className="state-text">No bio or favorite genres yet.</p>
      </div>
    )
  }
  return (
    <div className="card profile-about">
      {profile.bio && <p className="profile-bio">{profile.bio}</p>}
      {lists.map(([label, values]) => (
        <div key={label}>
          <p className="detail-label">{label}</p>
          <div className="chips chips-tight">
            {values.map((value) => (
              <span key={value} className="chip">
                {value}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
