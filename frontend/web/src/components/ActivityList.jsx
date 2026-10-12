import { Link } from 'react-router-dom'
import { prettyTitle } from '../titles.js'
import { timeAgo } from '../time.js'
import Avatar from './Avatar.jsx'
import { Poster } from './MovieCard.jsx'

const VERBS = {
  to_watch: 'wants to watch',
  liked: 'liked',
  disliked: "didn't like",
}

// Recent activity, newest first. With showUser each line names who did it (the feed); without,
// the lines are all one person's (a profile).
export default function ActivityList({ items, showUser }) {
  return (
    <ul className="activity-list">
      {items.map((item) => {
        const verb = VERBS[item.status] ?? item.status
        return (
          <li key={`${item.userId}-${item.movie.movieId}`} className="activity">
            {showUser && <Avatar name={item.username} />}
            <div className="activity-body">
              <p>
                {showUser ? (
                  <>
                    <Link className="activity-link" to={`/profile/${item.userId}`}>
                      {item.displayName || item.username}
                    </Link>{' '}
                    {verb}{' '}
                  </>
                ) : (
                  `${verb[0].toUpperCase()}${verb.slice(1)} `
                )}
                <Link className="activity-link" to={`/movie/${item.movie.movieId}`}>
                  {prettyTitle(item.movie.title)}
                </Link>
              </p>
              <p className="user-meta">{timeAgo(item.at)}</p>
            </div>
            <Link className="thumb" to={`/movie/${item.movie.movieId}`} tabIndex={-1} aria-hidden="true">
              <Poster title={item.movie.title} posterUrl={item.movie.posterUrl} />
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
