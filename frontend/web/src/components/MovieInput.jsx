import { useEffect, useId, useRef, useState } from 'react'
import { apiFetch } from '../api.js'
import { prettyTitle } from '../titles.js'

const DEBOUNCE_MS = 200

// Favorite movies, picked from the catalog: typing suggests titles, and only a suggestion can be
// added. Each value is a movie from edge ({ movieId, title, posterUrl, ... }).
export default function MovieInput({ label, hint, values, onChange, max = 10 }) {
  const [text, setText] = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [active, setActive] = useState(0)
  const [open, setOpen] = useState(false)
  const [failed, setFailed] = useState(false)
  const listId = useId()
  const inputRef = useRef(null)
  const full = values.length >= max
  const query = text.trim()

  useEffect(() => {
    if (query.length < 2) {
      setSuggestions([])
      return
    }
    let ignore = false
    const timer = setTimeout(() => {
      apiFetch(`/screens/movies/search?q=${encodeURIComponent(query)}`)
        .then((found) => {
          if (ignore) return
          setSuggestions(found)
          setActive(0)
          setFailed(false)
        })
        .catch((err) => {
          if (!ignore && err.status !== 401) setFailed(true)
        })
    }, DEBOUNCE_MS)
    return () => {
      ignore = true
      clearTimeout(timer)
    }
  }, [query])

  const options = suggestions.filter((s) => !values.some((v) => v.movieId === s.movieId))
  const showList = open && query.length >= 2

  function pick(movie) {
    onChange([...values, movie])
    setText('')
    setSuggestions([])
    inputRef.current?.focus()
  }

  function handleKeyDown(e) {
    if (e.key === 'ArrowDown' && options.length > 0) {
      e.preventDefault()
      setOpen(true)
      setActive((active + 1) % options.length)
    } else if (e.key === 'ArrowUp' && options.length > 0) {
      e.preventDefault()
      setActive((active - 1 + options.length) % options.length)
    } else if (e.key === 'Enter') {
      // never submits the form from here: Enter means "take the highlighted suggestion"
      e.preventDefault()
      if (showList && options[active]) pick(options[active])
    } else if (e.key === 'Escape') {
      setOpen(false)
    } else if (e.key === 'Backspace' && text === '' && values.length > 0) {
      onChange(values.slice(0, -1))
    }
  }

  return (
    <div className="field">
      <label className="field-label" htmlFor={`${listId}-input`}>
        {label}
      </label>
      <div className="autocomplete">
        <div className="tag-input">
          {values.map((movie) => (
            <span key={movie.movieId} className="chip tag">
              {prettyTitle(movie.title)}
              <button
                type="button"
                className="tag-remove"
                aria-label={`Remove ${prettyTitle(movie.title)}`}
                onClick={() => onChange(values.filter((v) => v.movieId !== movie.movieId))}
              >
                ×
              </button>
            </span>
          ))}
          <input
            ref={inputRef}
            id={`${listId}-input`}
            className="tag-text"
            role="combobox"
            aria-expanded={showList}
            aria-controls={listId}
            aria-autocomplete="list"
            aria-activedescendant={showList && options[active] ? `${listId}-${options[active].movieId}` : undefined}
            autoComplete="off"
            value={text}
            maxLength={80}
            placeholder={full ? '' : 'Start typing a title…'}
            disabled={full}
            onChange={(e) => {
              setText(e.target.value)
              setOpen(true)
            }}
            onFocus={() => setOpen(true)}
            onBlur={() => setOpen(false)}
            onKeyDown={handleKeyDown}
          />
        </div>
        {showList && (
          <ul className="autocomplete-list" id={listId} role="listbox">
            {options.map((movie, i) => (
              <li
                key={movie.movieId}
                id={`${listId}-${movie.movieId}`}
                role="option"
                aria-selected={i === active}
                className={i === active ? 'autocomplete-option autocomplete-option-active' : 'autocomplete-option'}
                // mousedown, not click: the input's blur would close the list before a click lands
                onMouseDown={(e) => {
                  e.preventDefault()
                  pick(movie)
                }}
                onMouseEnter={() => setActive(i)}
              >
                <Thumb movie={movie} />
                <span>{prettyTitle(movie.title)}</span>
              </li>
            ))}
            {options.length === 0 && (
              <li className="autocomplete-empty">
                {failed ? 'Could not search the catalog, try again.' : 'No movie with that title.'}
              </li>
            )}
          </ul>
        )}
      </div>
      <span className="field-hint">{full ? `That's the maximum of ${max}.` : hint}</span>
    </div>
  )
}

function Thumb({ movie }) {
  const [failed, setFailed] = useState(false)
  if (!movie.posterUrl || failed) return <span className="autocomplete-thumb" />
  return (
    <img className="autocomplete-thumb" src={movie.posterUrl} alt="" loading="lazy" onError={() => setFailed(true)} />
  )
}
