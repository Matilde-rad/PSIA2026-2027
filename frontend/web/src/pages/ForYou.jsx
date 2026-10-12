import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { useAuth } from '../auth.jsx'
import MovieCard, { hueFor } from '../components/MovieCard.jsx'
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
          <MovieCard key={rec.movieId} title={rec.title} posterUrl={rec.posterUrl} />
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

  const current = items[index]
  return (
    <section className="carousel" style={{ '--slide-ms': `${SLIDE_MS}ms` }}>
      <Banner key={current.movieId} title={current.title} bannerUrl={current.bannerUrl} />
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

function Banner({ title, bannerUrl }) {
  const [failed, setFailed] = useState(false)
  return (
    <article className="banner" style={{ '--poster-hue': hueFor(title) }}>
      {bannerUrl && !failed && <img src={bannerUrl} alt="" loading="lazy" onError={() => setFailed(true)} />}
      <span className="banner-title">{prettyTitle(title)}</span>
    </article>
  )
}
