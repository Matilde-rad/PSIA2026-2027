import { useState } from 'react'
import { Link } from 'react-router-dom'
import { prettyTitle } from '../titles.js'

export function hueFor(title) {
  let sum = 0
  for (const ch of title) sum += ch.charCodeAt(0)
  return sum % 360
}

export function Poster({ title, posterUrl }) {
  const [failed, setFailed] = useState(false)
  const name = prettyTitle(title)
  return posterUrl && !failed ? (
    <div className="poster">
      <img src={posterUrl} alt={`${name} poster`} loading="lazy" onError={() => setFailed(true)} />
    </div>
  ) : (
    <div className="poster" style={{ '--poster-hue': hueFor(title) }}>
      <span className="poster-title">{name}</span>
    </div>
  )
}

export default function MovieCard({ movieId, title, year, posterUrl }) {
  return (
    <article className="movie-card">
      <Link className="movie-link" to={`/movie/${movieId}`}>
        <Poster title={title} posterUrl={posterUrl} />
        <h3 className="movie-title">{prettyTitle(title)}</h3>
        {year && <p className="movie-year">{year}</p>}
      </Link>
    </article>
  )
}
