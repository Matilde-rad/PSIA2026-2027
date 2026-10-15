import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useNavigationType, useParams } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { hueFor, Poster } from '../components/MovieCard.jsx'
import WatchButtons from '../components/WatchButtons.jsx'
import { prettyTitle } from '../titles.js'
import { runtime } from '../time.js'

export default function MoviePage() {
  const { id } = useParams()
  const [movie, setMovie] = useState(null)
  const [error, setError] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [bannerFailed, setBannerFailed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const navigationType = useNavigationType()

  // A movie opened from far down a list would otherwise open scrolled down; Back/Forward leaves it alone.
  useEffect(() => {
    if (navigationType !== 'POP') window.scrollTo(0, 0)
  }, [id])

  // A page opened directly (shared link, new tab) has no in-app history, so Back goes Home instead of leaving the site.
  const goBack = () => (location.key === 'default' ? navigate('/') : navigate(-1))

  useEffect(() => {
    let ignore = false
    setMovie(null)
    setError(null)
    setBannerFailed(false)
    apiFetch(`/screens/movie/${encodeURIComponent(id)}`)
      .then((data) => {
        if (!ignore) setMovie(data)
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
        <p className="state-title">Movie not found</p>
        <p className="state-text">There is no movie with this id in the catalog.</p>
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
  if (!movie) return <p className="state-text">Loading movie…</p>

  const facts = [
    movie.year,
    movie.runtimeMinutes && runtime(movie.runtimeMinutes),
    movie.imdbRating && `★ ${movie.imdbRating.toFixed(1)} IMDb`,
  ].filter(Boolean)

  return (
    <>
      <button type="button" className="btn btn-ghost btn-sm movie-back" onClick={goBack}>
        ← Back
      </button>
      <section className="movie-hero" style={{ '--poster-hue': hueFor(movie.title) }}>
        {movie.bannerUrl && !bannerFailed && (
          <img className="movie-hero-bg" src={movie.bannerUrl} alt="" onError={() => setBannerFailed(true)} />
        )}
        <div className="movie-hero-content">
          <div className="movie-hero-poster">
            <Poster title={movie.title} posterUrl={movie.posterUrl} />
          </div>
          <div className="movie-hero-main">
            <h1 className="page-title">{prettyTitle(movie.title)}</h1>
            {facts.length > 0 && <p className="movie-facts">{facts.join(' · ')}</p>}
            {movie.genres.length > 0 && (
              <div className="chips chips-tight">
                {movie.genres.map((genre) => (
                  <span key={genre} className="chip chip-sm">
                    {genre}
                  </span>
                ))}
              </div>
            )}
            <div className="movie-actions">
              <WatchButtons
                movieId={movie.movieId}
                status={movie.status}
                onChange={(status) => setMovie({ ...movie, status })}
              />
            </div>
          </div>
        </div>
      </section>

      <section className="card movie-about">
        <div>
          <p className="detail-label">Synopsis</p>
          <p className="profile-bio">{movie.description ?? 'No synopsis available for this movie.'}</p>
        </div>
        <People label={movie.directors.length === 1 ? 'Director' : 'Directors'} names={movie.directors} />
        <People label="Cast" names={movie.cast} />
      </section>
    </>
  )
}

function People({ label, names }) {
  if (names.length === 0) return null
  return (
    <div>
      <p className="detail-label">{label}</p>
      <div className="chips chips-tight">
        {names.map((name) => (
          <span key={name} className="chip">
            {name}
          </span>
        ))}
      </div>
    </div>
  )
}
