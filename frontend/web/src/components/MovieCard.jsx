import { useState } from 'react'
import { prettyTitle } from '../titles.js'

export function hueFor(title) {
  let sum = 0
  for (const ch of title) sum += ch.charCodeAt(0)
  return sum % 360
}

// Falls back to the placeholder when the image is missing (MetaHub answers 404).
export default function MovieCard({ title, year, posterUrl }) {
  const [failed, setFailed] = useState(false)
  const name = prettyTitle(title)
  return (
    <article className="movie-card">
      {posterUrl && !failed ? (
        <div className="poster">
          <img src={posterUrl} alt={`${name} poster`} loading="lazy" onError={() => setFailed(true)} />
        </div>
      ) : (
        <div className="poster" style={{ '--poster-hue': hueFor(title) }}>
          <span className="poster-title">{name}</span>
        </div>
      )}
      <h3 className="movie-title">{name}</h3>
      {year && <p className="movie-year">{year}</p>}
    </article>
  )
}
