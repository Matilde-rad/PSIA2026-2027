import { useState } from 'react'
import { apiFetch } from '../api.js'

// The logged-in user's status for one movie: null (not on their list), 'to_watch', 'liked' or
// 'disliked'. onChange(next) runs after edge confirmed the change; next is null after a removal.
export default function WatchButtons({ movieId, status, onChange, small }) {
  const [busy, setBusy] = useState(false)
  const [failed, setFailed] = useState(false)

  async function change(next) {
    setBusy(true)
    setFailed(false)
    try {
      const path = `/me/watchlist/${encodeURIComponent(movieId)}`
      if (next) await apiFetch(path, { method: 'PUT', body: { status: next } })
      else await apiFetch(path, { method: 'DELETE' })
      onChange(next)
    } catch (err) {
      if (err.status !== 401) setFailed(true)
    } finally {
      setBusy(false)
    }
  }

  const size = small ? ' btn-sm' : ''
  const verdict = (value, label) => (
    <button
      type="button"
      className={(status === value ? `btn btn-verdict btn-${value}` : 'btn btn-secondary') + size}
      aria-pressed={status === value}
      disabled={busy}
      // pressing the current verdict again changes nothing
      onClick={() => status !== value && change(value)}
    >
      {label}
    </button>
  )

  return (
    <div className="watch-buttons">
      {status ? (
        <>
          {verdict('liked', 'Liked it')}
          {verdict('disliked', "Didn't like it")}
          <button type="button" className={'btn btn-ghost' + size} disabled={busy} onClick={() => change(null)}>
            Remove
          </button>
        </>
      ) : (
        <button type="button" className={'btn btn-primary' + size} disabled={busy} onClick={() => change('to_watch')}>
          + To watch
        </button>
      )}
      {failed && (
        <span className="field-error" role="alert">
          That did not work, try again.
        </span>
      )}
    </div>
  )
}
