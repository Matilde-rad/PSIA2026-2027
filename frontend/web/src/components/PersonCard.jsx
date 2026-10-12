import { Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { prettyTitle } from '../titles.js'
import Avatar from './Avatar.jsx'
import FollowButton from './FollowButton.jsx'

// One user in a list: who they are, what you have in common, and a follow button.
// onFollowChange(person, nowFollowing) lets the list update itself.
export default function PersonCard({ person, onFollowChange, onNavigate }) {
  const { user } = useAuth()
  const name = person.displayName || person.username
  const movies = person.sharedMovies.map(prettyTitle)

  return (
    <div className="user-card">
      <Avatar name={person.username} />
      <div className="user-card-body">
        <Link className="user-name" to={`/profile/${person.id}`} onClick={onNavigate}>
          {name}
        </Link>
        {name !== person.username && <p className="user-meta">@{person.username}</p>}
        {movies.length > 0 && (
          <p className="user-meta">
            Also likes {movies.slice(0, 2).join(', ')}
            {movies.length > 2 && ` and ${movies.length - 2} more`}
          </p>
        )}
        {person.sharedDirectors.length > 0 && (
          <p className="user-meta">Also into {person.sharedDirectors.slice(0, 2).join(', ')}</p>
        )}
        {person.sharedGenres.length > 0 && (
          <div className="chips">
            {person.sharedGenres.slice(0, 4).map((genre) => (
              <span key={genre} className="chip chip-sm">
                {genre}
              </span>
            ))}
          </div>
        )}
      </div>
      {person.id !== user.id && (
        <FollowButton
          small
          userId={person.id}
          following={person.followedByMe}
          onChange={(now) => onFollowChange(person, now)}
        />
      )}
    </div>
  )
}
