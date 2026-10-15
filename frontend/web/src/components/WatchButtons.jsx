import { useState } from 'react'
import { apiFetch } from '../api.js'

function Thumb({ down }) {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      style={down ? { transform: 'rotate(180deg)' } : undefined}
    >
      <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" />
    </svg>
  )
}

function Bookmark() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
    </svg>
  )
}

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
      className={status === value ? 'segment segment-selected' : 'segment'}
      aria-pressed={status === value}
      disabled={busy}
      // pressing the current verdict again changes nothing
      onClick={() => status !== value && change(value)}
    >
      <Thumb down={value === 'disliked'} />
      {label}
    </button>
  )

  return (
    <div className="watch-buttons">
      {status ? (
        <>
          <div className={small ? 'segmented segmented-sm' : 'segmented'} role="group" aria-label="Your verdict">
            {verdict('liked', 'Liked')}
            {verdict('disliked', "Didn't like")}
          </div>
          <button
            type="button"
            className={'btn btn-ghost btn-icon' + size}
            disabled={busy}
            aria-label="Remove from watchlist"
            title="Remove from watchlist"
            onClick={() => change(null)}
          >
            ✕
          </button>
        </>
      ) : (
        <div className={small ? 'segmented segmented-sm' : 'segmented'}>
          <button type="button" className="segment segment-add" disabled={busy} onClick={() => change('to_watch')}>
            <Bookmark />
            Add to watchlist
          </button>
        </div>
      )}
      {failed && (
        <span className="field-error" role="alert">
          That did not work, try again.
        </span>
      )}
    </div>
  )
}
