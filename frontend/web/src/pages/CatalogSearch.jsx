import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiFetch } from '../api.js'
import MovieCard from '../components/MovieCard.jsx'
import ResultsToolbar, { PAGE_SIZE } from '../components/ResultsToolbar.jsx'
import { saveScreen, useSavedScreen } from '../screenCache.js'

const DEBOUNCE_MS = 300
// A placeholder and a screen format, not genres; hidden here only, the profile form still lists them.
const HIDDEN_GENRES = ['(no genres listed)', 'IMAX']

function queryKey(text, genres) {
  const query = new URLSearchParams()
  if (text.trim()) query.set('q', text.trim())
  genres.forEach((genre) => query.append('genre', genre))
  return query.toString()
}

export default function CatalogSearch() {
  const [params, setParams] = useSearchParams()
  const saved = useSavedScreen('search')
  // BrowserRouter updates the URL in a transition, so a URL-bound input lags; the URL only mirrors this state.
  const [text, setText] = useState(() => params.get('q') ?? '')
  const [genres, setGenres] = useState(() => params.getAll('genre'))
  const [debounced, setDebounced] = useState(text)
  const [catalogGenres, setCatalogGenres] = useState(saved?.genres ?? null)
  // The URL wins: a shared or edited link never shows another query's cached results.
  const [result, setResult] = useState(() =>
    saved?.result?.key === queryKey(params.get('q') ?? '', params.getAll('genre')) ? saved.result : null,
  )
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(null)
  const [moreError, setMoreError] = useState(null)

  const key = queryKey(debounced, genres)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(text), DEBOUNCE_MS)
    return () => clearTimeout(timer)
  }, [text])

  useEffect(() => {
    // restored with the results, so the chips are in the first commit
    if (Array.isArray(catalogGenres)) return
    let ignore = false
    apiFetch('/screens/genres')
      .then((data) => {
        if (!ignore) setCatalogGenres(data)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setCatalogGenres('failed')
      })
    return () => {
      ignore = true
    }
  }, [])

  // A response for an older key is dropped. The previous results stay on screen meanwhile, so typing
  // does not flash the grid.
  useEffect(() => {
    if (!key) {
      setResult(null)
      setError(null)
      setMoreError(null)
      return
    }
    if (result?.key === key) {
      setError(null)
      setMoreError(null)
      return
    }
    let ignore = false
    apiFetch(`/screens/browse?${key}&offset=0`)
      .then((page) => {
        if (ignore) return
        setResult({ key, movies: page, exhausted: page.length < PAGE_SIZE })
        setError(null)
        setMoreError(null)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setError(err)
      })
    return () => {
      ignore = true
    }
  }, [key])

  useEffect(() => {
    saveScreen('search', { result, genres: Array.isArray(catalogGenres) ? catalogGenres : null })
  }, [result, catalogGenres])

  function mirror(nextText, nextGenres) {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        next.delete('q')
        next.delete('genre')
        if (nextText) next.set('q', nextText)
        nextGenres.forEach((genre) => next.append('genre', genre))
        return next
      },
      { replace: true },
    )
  }

  function changeText(value) {
    setText(value)
    mirror(value, genres)
  }

  function toggleGenre(genre) {
    const next = genres.includes(genre) ? genres.filter((g) => g !== genre) : [...genres, genre]
    setGenres(next)
    mirror(text, next)
  }

  async function more() {
    const forKey = result.key
    setBusy('more')
    setMoreError(null)
    try {
      const page = await apiFetch(`/screens/browse?${forKey}&offset=${result.movies.length}`)
      // a newer search replaced the results while this page loaded
      setResult((current) =>
        current.key === forKey
          ? { ...current, movies: [...current.movies, ...page], exhausted: page.length < PAGE_SIZE }
          : current,
      )
    } catch (err) {
      if (err.status !== 401) setMoreError(err)
    } finally {
      setBusy(null)
    }
  }

  const loaded = Array.isArray(catalogGenres) ? catalogGenres.filter((g) => !HIDDEN_GENRES.includes(g)) : []
  const genreOptions = [...loaded, ...genres.filter((g) => !loaded.includes(g))]

  return (
    <>
      <div className="ask-bar">
        <input
          className="ask-input"
          type="search"
          value={text}
          onChange={(e) => changeText(e.target.value)}
          placeholder="Search movies by title"
          aria-label="Search movies by title"
          maxLength={200}
          autoFocus
        />
      </div>
      <div className="chips" role="group" aria-label="Genres">
        {genreOptions.map((genre) => (
          <button
            key={genre}
            type="button"
            aria-pressed={genres.includes(genre)}
            className={genres.includes(genre) ? 'chip chip-toggle chip-selected' : 'chip chip-toggle'}
            onClick={() => toggleGenre(genre)}
          >
            {genre}
          </button>
        ))}
      </div>
      {catalogGenres === 'failed' && <p className="state-text">Could not load the genres.</p>}
      {!key ? (
        <p className="state-text">Type a title or pick genres.</p>
      ) : error ? (
        <div className="state-message" role="alert">
          <p className="state-title">Something went wrong.</p>
          <p className="state-text">Try again in a moment.</p>
        </div>
      ) : !result ? (
        <p className="state-text">Searching…</p>
      ) : result.movies.length === 0 ? (
        <div className="state-message">
          <p className="state-title">No movies match.</p>
          <p className="state-text">Try other words or fewer genres.</p>
        </div>
      ) : (
        <>
          <div className="movie-grid">
            {result.movies.map((movie) => (
              <MovieCard key={movie.movieId} movieId={movie.movieId} title={movie.title} posterUrl={movie.posterUrl} />
            ))}
          </div>
          {result.key === key && (
            <ResultsToolbar
              busy={busy}
              exhausted={result.exhausted}
              error={moreError}
              endText="No more movies match."
              onMore={more}
            />
          )}
        </>
      )}
    </>
  )
}
