import Avatar from '../components/Avatar.jsx'
import PreviewBadge from '../components/PreviewBadge.jsx'

const GENRES = ['Action', 'Comedy', 'Drama', 'Horror', 'Sci-Fi', 'Animation']

const EXAMPLE_USERS = [
  { username: 'marta_reels', since: 'March 2026', genres: ['Drama', 'Animation'] },
  { username: 'tiago-sci', since: 'May 2026', genres: ['Sci-Fi', 'Horror'] },
  { username: 'ines_watches', since: 'August 2026', genres: ['Comedy', 'Drama'] },
]

function UserCard({ username, since, genres }) {
  return (
    <div className="user-card">
      <Avatar name={username} />
      <div>
        <p className="user-name">{username}</p>
        <p className="user-meta">Member since {since}</p>
        <div className="chips">
          {genres.map((genre) => (
            <span key={genre} className="chip chip-sm">
              {genre}
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function SocialPreview() {
  return (
    <>
      <section className="preview-section">
        <div className="preview-header">
          <h2 className="section-title">Search users by name</h2>
          <PreviewBadge />
        </div>
        <div className="preview" inert>
          <div className="search-box">
            <span className="search-icon">⌕</span>
            <span className="fake-input">Search users by name…</span>
          </div>
          <div className="user-grid">
            {EXAMPLE_USERS.map((user) => (
              <UserCard key={user.username} {...user} />
            ))}
          </div>
        </div>
      </section>

      <section className="preview-section">
        <div className="preview-header">
          <h2 className="section-title">Find users who like…</h2>
          <PreviewBadge />
        </div>
        <div className="preview" inert>
          <div className="chips">
            {GENRES.map((genre) => (
              <span key={genre} className={genre === 'Drama' ? 'chip chip-selected' : 'chip'}>
                {genre}
              </span>
            ))}
          </div>
          <div className="user-grid">
            {EXAMPLE_USERS.filter((user) => user.genres.includes('Drama')).map((user) => (
              <UserCard key={user.username} {...user} />
            ))}
          </div>
        </div>
      </section>
    </>
  )
}
