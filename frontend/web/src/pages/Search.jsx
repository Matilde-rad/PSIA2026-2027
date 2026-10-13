import { useRef, useState } from 'react'
import { apiFetch } from '../api.js'
import MovieCard from '../components/MovieCard.jsx'
import ResultsToolbar, { PAGE_SIZE } from '../components/ResultsToolbar.jsx'

export default function Search() {
  const [text, setText] = useState('')
  const [asking, setAsking] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  // the submitted text and every movie id shown for it, so Get more and Refresh only bring unseen ones
  const submitted = useRef('')
  const seen = useRef(new Set())
  const [busy, setBusy] = useState(null)
  const [exhausted, setExhausted] = useState(false)
  const [moreError, setMoreError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    const request = text.trim()
    if (!request) return
    setAsking(true)
    setResult(null)
    setError(null)
    setMoreError(null)
    setExhausted(false)
    submitted.current = request
    seen.current = new Set()
    try {
      const data = await apiFetch('/screens/search', { method: 'POST', body: { text: request } })
      data.results.forEach((rec) => seen.current.add(rec.movieId))
      if (data.results.length < PAGE_SIZE) setExhausted(true)
      setResult(data)
    } catch (err) {
      if (err.status !== 401) setError(err)
    } finally {
      setAsking(false)
    }
  }

  async function load(mode) {
    setBusy(mode)
    setMoreError(null)
    try {
      const data = await apiFetch('/screens/search', {
        method: 'POST',
        body: { text: submitted.current, exclude: [...seen.current] },
      })
      data.results.forEach((rec) => seen.current.add(rec.movieId))
      if (data.results.length < PAGE_SIZE) setExhausted(true)
      if (mode === 'more') setResult((current) => ({ results: [...current.results, ...data.results] }))
      else if (data.results.length > 0) setResult(data)
    } catch (err) {
      if (err.status !== 401) setMoreError(err)
    } finally {
      setBusy(null)
    }
  }

  return (
    <>
      <form className="ask-bar" onSubmit={handleSubmit}>
        <input
          className="ask-input"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder='Ask for anything, e.g. "cozy sci-fi from the 90s"'
          aria-label="Ask the AI for movies"
          maxLength={200}
          autoFocus
        />
        <button className="btn btn-primary" type="submit" disabled={asking || !text.trim()}>
          {asking ? 'Asking…' : 'Ask AI'}
        </button>
      </form>
      {asking && <p className="state-text">Finding movies…</p>}
      {error && <SearchError error={error} />}
      {result && <Results result={result} />}
      {result && result.results.length > 0 && !asking && (
        <ResultsToolbar
          busy={busy}
          exhausted={exhausted}
          error={moreError}
          endText="No more results for this request."
          onRefresh={() => load('refresh')}
          onMore={() => load('more')}
        />
      )}
    </>
  )
}

function SearchError({ error }) {
  return (
    <div className="state-message" role="alert">
      <p className="state-title">
        {error.status === 503 ? 'The AI is unavailable right now.' : 'Something went wrong.'}
      </p>
      <p className="state-text">Try again in a moment.</p>
    </div>
  )
}

function Results({ result }) {
  if (result.results.length === 0) {
    return (
      <div className="state-message">
        <p className="state-text">No movies matched, try another request.</p>
      </div>
    )
  }
  return (
    <div className="movie-grid">
      {result.results.map((rec) => (
        <MovieCard key={rec.movieId} movieId={rec.movieId} title={rec.title} posterUrl={rec.posterUrl} />
      ))}
    </div>
  )
}

