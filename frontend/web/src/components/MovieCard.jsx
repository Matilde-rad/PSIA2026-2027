export function hueFor(title) {
  let sum = 0
  for (const ch of title) sum += ch.charCodeAt(0)
  return sum % 360
}

export default function MovieCard({ title, year, posterUrl }) {
  return (
    <article className="movie-card">
      {posterUrl ? (
        <div className="poster">
          <img src={posterUrl} alt={`${title} poster`} loading="lazy" />
        </div>
      ) : (
        <div className="poster" style={{ '--poster-hue': hueFor(title) }}>
          <span className="poster-title">{title}</span>
        </div>
      )}
      <h3 className="movie-title">{title}</h3>
      {year && <p className="movie-year">{year}</p>}
    </article>
  )
}
