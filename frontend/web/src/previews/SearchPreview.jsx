import MovieCard from '../components/MovieCard.jsx'
import PreviewBadge from '../components/PreviewBadge.jsx'

const EXAMPLE_MOVIES = [
  { title: 'Interstellar', year: 2014 },
  { title: 'Spirited Away', year: 2001 },
  { title: 'The Grand Budapest Hotel', year: 2014 },
  { title: 'Mad Max: Fury Road', year: 2015 },
  { title: 'Parasite', year: 2019 },
  { title: 'Arrival', year: 2016 },
  { title: 'Whiplash', year: 2014 },
  { title: 'Get Out', year: 2017 },
]

export default function SearchPreview() {
  return (
    <section className="preview-section">
      <div className="preview-header">
        <h2 className="section-title">Search movies</h2>
        <PreviewBadge />
      </div>
      <div className="preview" inert>
        <div className="ask-bar">
          <span className="fake-input">Ask for a genre, e.g. "cozy sci-fi"</span>
          <span className="btn btn-primary">Ask</span>
        </div>
        <div className="movie-grid">
          {EXAMPLE_MOVIES.map((movie) => (
            <MovieCard key={movie.title} title={movie.title} year={movie.year} />
          ))}
        </div>
      </div>
    </section>
  )
}
