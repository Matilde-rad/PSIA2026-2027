import { useState } from 'react'
import { apiFetch } from '../api.js'

export default function FollowButton({ userId, following, onChange, small }) {
  const [busy, setBusy] = useState(false)
  const [failed, setFailed] = useState(false)

  async function toggle() {
    setBusy(true)
    setFailed(false)
    try {
      await apiFetch(`/me/following/${encodeURIComponent(userId)}`, { method: following ? 'DELETE' : 'PUT' })
      onChange(!following)
    } catch (err) {
      if (err.status !== 401) setFailed(true)
    } finally {
      setBusy(false)
    }
  }

  const size = small ? ' btn-sm' : ''
  return (
    <button
      type="button"
      className={(following ? 'btn btn-secondary' : 'btn btn-primary') + size}
      disabled={busy}
      aria-pressed={following}
      title={failed ? 'That did not work, try again.' : undefined}
      onClick={toggle}
    >
      {failed ? 'Try again' : following ? 'Following' : 'Follow'}
    </button>
  )
}
