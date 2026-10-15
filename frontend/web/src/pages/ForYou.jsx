import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import { useAuth } from '../auth.jsx'
import MovieCard, { hueFor } from '../components/MovieCard.jsx'
import ResultsToolbar, { PAGE_SIZE } from '../components/ResultsToolbar.jsx'
import { runtime } from '../time.js'
import { prettyTitle } from '../titles.js'

export default function ForYou() {
  const { user } = useAuth()
  const [recommendations, setRecommendations] = useState(null)
  const [error, setError] = useState(null)
  const [attempt, setAttempt] = useState(0)
  // every movie id shown since the load, so Get more and Refresh only bring unseen ones
  const seen = useRef(new Set())
  const [busy, setBusy] = useState(null)
  const [exhausted, setExhausted] = useState(false)
  const [moreError, setMoreError] = useState(null)

  useEffect(() => {
    let ignore = false
    setRecommendations(null)
    setError(null)
    setBusy(null)
    setExhausted(false)
    setMoreError(null)
    seen.current = new Set()
    apiFetch('/screens/home')
      .then((data) => {
        if (ignore) return
        data.recommendations.forEach((rec) => seen.current.add(rec.movieId))
        setRecommendations(data.recommendations)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setError(err)
      })
    return () => {
      ignore = true
    }
  }, [attempt])

  async function load(mode) {
    setBusy(mode)
    setMoreError(null)
    try {
      const data = await apiFetch('/screens/home/more', { method: 'POST', body: { exclude: [...seen.current] } })
      const page = data.recommendations
      page.forEach((rec) => seen.current.add(rec.movieId))
      if (page.length < PAGE_SIZE) setExhausted(true)
      if (mode === 'more') setRecommendations((current) => [...current, ...page])
      else if (page.length > 0) setRecommendations(page)
    } catch (err) {
      if (err.status !== 401) setMoreError(err)
    } finally {
      setBusy(null)
    }
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
      <Carousel key={recommendations[0].movieId} items={recommendations} />
      <div className="movie-grid">
        {recommendations.map((rec) => (
          <MovieCard key={rec.movieId} movieId={rec.movieId} title={rec.title} posterUrl={rec.posterUrl} />
        ))}
      </div>
      <ResultsToolbar
        busy={busy}
        exhausted={exhausted}
        error={moreError}
        endText="No more recommendations for your profile."
        onRefresh={() => load('refresh')}
        onMore={() => load('more')}
      />
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
