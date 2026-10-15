import { useEffect, useRef, useState } from 'react'
import { apiFetch } from '../api.js'
import PersonCard from './PersonCard.jsx'

export default function PeopleDialog({ userId, kind, onClose, onFollowChange }) {
  const [people, setPeople] = useState(null)
  const [error, setError] = useState(false)
  const dialogRef = useRef(null)

  useEffect(() => {
    dialogRef.current?.showModal()
  }, [])

  useEffect(() => {
    let ignore = false
    apiFetch(`/screens/profile/${encodeURIComponent(userId)}/${kind}`)
      .then((data) => {
        if (!ignore) setPeople(data)
      })
      .catch((err) => {
        if (!ignore && err.status !== 401) setError(true)
      })
    return () => {
      ignore = true
    }
  }, [userId, kind])

  function handleFollowChange(person, now) {
    setPeople(people.map((p) => (p.id === person.id ? { ...p, followedByMe: now } : p)))
    onFollowChange(person, now)
  }

  return (
    <dialog
      ref={dialogRef}
      className="dialog"
      aria-labelledby="people-dialog-title"
      onClose={onClose}
      // a click on the backdrop lands on the dialog element itself
      onClick={(e) => {
        if (e.target === dialogRef.current) onClose()
      }}
    >
      <div className="dialog-body">
        <div className="section-header">
          <h2 className="section-title" id="people-dialog-title">
            {kind === 'followers' ? 'Followers' : 'Following'}
          </h2>
          <button className="btn btn-ghost btn-sm" type="button" onClick={onClose}>
            Close
          </button>
        </div>
        {error && <p className="state-text">Something went wrong, try again.</p>}
        {!error && !people && <p className="state-text">Loading…</p>}
        {people?.length === 0 && (
          <p className="state-text">{kind === 'followers' ? 'No followers yet.' : 'Not following anyone yet.'}</p>
        )}
        {people?.length > 0 && (
          <div className="people-list">
            {people.map((person) => (
              <PersonCard key={person.id} person={person} onFollowChange={handleFollowChange} onNavigate={onClose} />
            ))}
          </div>
        )}
      </div>
    </dialog>
  )
}
