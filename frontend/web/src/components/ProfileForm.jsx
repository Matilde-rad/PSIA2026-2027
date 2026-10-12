import { useEffect, useState } from 'react'
import { apiFetch, fieldMessages } from '../api.js'
import MovieInput from './MovieInput.jsx'
import TagInput from './TagInput.jsx'

const BIO_MAX = 500

// Creates or replaces the logged-in user's profile. `secondary` is the button next to Save
// (Skip on the welcome page, Cancel on the profile page). The profile stores favorite movies as
// catalog ids, so the movies to show for them come separately in `initialMovies`.
export default function ProfileForm({ initial, initialMovies, submitLabel, onSaved, secondary }) {
  const [displayName, setDisplayName] = useState(initial?.displayName ?? '')
  const [bio, setBio] = useState(initial?.bio ?? '')
  const [genres, setGenres] = useState(initial?.favoriteGenres ?? [])
  const [directors, setDirectors] = useState(initial?.favoriteDirectors ?? [])
  const [movies, setMovies] = useState(initialMovies ?? [])
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  // The genres the movie catalog has, from edge. null while loading, 'failed' if that did not work.
  const [catalogGenres, setCatalogGenres] = useState(null)

  useEffect(() => {
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

  // Genres already on the profile stay visible (and removable) even if the catalog list is missing.
  const loaded = Array.isArray(catalogGenres) ? catalogGenres : []
  const genreOptions = [...loaded, ...genres.filter((g) => !loaded.includes(g))]

  function toggleGenre(genre) {
    setGenres(genres.includes(genre) ? genres.filter((g) => g !== genre) : [...genres, genre])
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    setFormError(null)
    setFieldErrors({})
    try {
      const saved = await apiFetch('/me/profile', {
        method: 'PUT',
        body: {
          displayName: displayName.trim() || null,
          bio: bio.trim() || null,
          favoriteGenres: genres,
          favoriteDirectors: directors,
          favoriteMovies: movies.map((movie) => movie.movieId),
        },
      })
      onSaved(saved)
    } catch (err) {
      if (err.status === 401) return
      if (err.status === 400 && err.fields.length > 0) {
        setFieldErrors(fieldMessages(err))
      } else {
        setFormError('Something went wrong, try again.')
      }
      setSubmitting(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      {formError && (
        <p className="form-error" role="alert">
          {formError}
        </p>
      )}
      <label className="field">
        <span className="field-label">Display name</span>
        <input
          className="field-input"
          name="displayName"
          maxLength={64}
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
        />
        {fieldErrors.displayName ? (
          <span className="field-error">{fieldErrors.displayName}</span>
        ) : (
          <span className="field-hint">Shown on your profile instead of your username.</span>
        )}
      </label>
      <label className="field">
        <span className="field-label">Bio</span>
        <textarea
          className="field-input field-textarea"
          name="bio"
          rows={3}
          maxLength={BIO_MAX}
          value={bio}
          onChange={(e) => setBio(e.target.value)}
        />
        {fieldErrors.bio ? (
          <span className="field-error">{fieldErrors.bio}</span>
        ) : (
          <span className="field-hint">
            What do you like to watch? {bio.length}/{BIO_MAX}
          </span>
        )}
      </label>
      <div className="field">
        <span className="field-label">Favorite genres</span>
        <div className="chips chips-tight">
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
        {catalogGenres === null && <span className="field-hint">Loading genres…</span>}
        {catalogGenres === 'failed' && (
          <span className="field-error">Could not load the genre list. You can still save the rest.</span>
        )}
      </div>
      <TagInput
        label="Favorite directors"
        placeholder="Type a name and press Enter"
        hint="Press Enter after each name."
        values={directors}
        onChange={setDirectors}
      />
      <MovieInput
        label="Favorite movies"
        hint="Pick from the suggestions. We recommend movies like these, not these ones."
        values={movies}
        onChange={setMovies}
      />
      <div className="form-actions">
        {secondary}
        <button className="btn btn-primary" type="submit" disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
