import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { useAuth } from '../auth.jsx'
import MovieCard, { hueFor } from '../components/MovieCard.jsx'
import { runtime } from '../time.js'
import { prettyTitle } from '../titles.js'

export default function ForYou() {
  const { user } = useAuth()
  const [recommendations, setRecommendations] = useState(null)
  const [error, setError] = useState(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let ignore = false
    setRecommendations(null)
    setError(null)
    apiFetch('/screens/home')
      .then((data) => {
        if (!ignore) setRecommendations(data.recommendations)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setError(err)
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
  if (!recommendations) return <p className="state-text">Loading recommendations…</p>
  if (recommendations.length === 0) {
    return (
      <div className="state-message">
        <p className="state-title">No recommendations yet</p>
        <p className="state-text">
          They are made from your profile: add your favorite genres, directors and movies.
        </p>
        <Link className="btn btn-primary" to={`/profile/${user.id}`}>
          Go to your profile
        </Link>
      </div>
    )
  }

  return (
    <>
      <Carousel items={recommendations} />
      <div className="movie-grid">
        {recommendations.map((rec) => (
          <MovieCard key={rec.movieId} movieId={rec.movieId} title={rec.title} posterUrl={rec.posterUrl} />
        ))}
      </div>
    </>
  )
}

const SLIDE_MS = 6000

function Carousel({ items }) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (items.length < 2) return
    const timer = setTimeout(() => setIndex((index + 1) % items.length), SLIDE_MS)
    return () => clearTimeout(timer)
  }, [index, items.length])

  // running time, rating and synopsis of each movie, loaded once and keyed by movie id;
  // a banner without them still shows its title
  const [details, setDetails] = useState({})
  useEffect(() => {
    let ignore = false
    for (const rec of items) {
      apiFetch(`/screens/movie/${rec.movieId}`)
        .then((data) => {
          if (!ignore) setDetails((known) => ({ ...known, [rec.movieId]: data }))
        })
        .catch(() => {})
    }
    return () => {
      ignore = true
    }
  }, [items])

  const current = items[index]
  return (
    <section className="carousel" style={{ '--slide-ms': `${SLIDE_MS}ms` }}>
      <Banner
        key={current.movieId}
        movieId={current.movieId}
        title={current.title}
        bannerUrl={current.bannerUrl}
        details={details[current.movieId]}
      />
      {items.length > 1 && (
        <div className="carousel-lines">
          {items.map((rec, i) => (
            <button
              key={rec.movieId}
              type="button"
              className={i === index ? 'carousel-line carousel-line-active' : 'carousel-line'}
              aria-label={`Show ${prettyTitle(rec.title)}`}
              aria-current={i === index}
              onClick={() => setIndex(i)}
            />
          ))}
        </div>
      )}
    </section>
  )
}

function Banner({ movieId, title, bannerUrl, details }) {
  const [failed, setFailed] = useState(false)
  const facts = [
    details?.runtimeMinutes && runtime(details.runtimeMinutes),
    details?.imdbRating && `★ ${details.imdbRating.toFixed(1)} IMDb`,
  ].filter(Boolean)
  return (
    <Link className="banner" to={`/movie/${movieId}`} style={{ '--poster-hue': hueFor(title) }}>
      {bannerUrl && !failed && <img src={bannerUrl} alt="" loading="lazy" onError={() => setFailed(true)} />}
      <span className="banner-text">
        <span className="banner-title">{prettyTitle(title)}</span>
        {facts.length > 0 && <span className="banner-facts">{facts.join(' · ')}</span>}
        {details?.description && <span className="banner-synopsis">{details.description}</span>}
      </span>
    </Link>
  )
}
