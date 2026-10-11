import PreviewBadge from '../components/PreviewBadge.jsx'

const SAMPLE_BIO = 'Late-night sci-fi fan. Will watch anything with a great soundtrack.'
const FAVORITE_GENRES = ['Sci-Fi', 'Drama', 'Animation']
const GENRES = ['Action', 'Comedy', 'Drama', 'Horror', 'Sci-Fi', 'Animation']

export default function ProfilePreview({ own }) {
  return (
    <>
      <section className="preview-section">
        <div className="preview-header">
          <h2 className="section-title">About</h2>
          <PreviewBadge />
        </div>
        <div className="preview" inert>
          <div className="card profile-preview">
            <p className="profile-bio">{SAMPLE_BIO}</p>
            <p className="fake-label">Favorite genres</p>
            <div className="chips">
              {FAVORITE_GENRES.map((genre) => (
                <span key={genre} className="chip">
                  {genre}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      {own && (
        <section className="preview-section">
          <div className="preview-header">
            <h2 className="section-title">Edit profile</h2>
            <PreviewBadge />
          </div>
          <div className="preview" inert>
            <div className="card profile-preview">
              <p className="fake-label">Bio</p>
              <div className="fake-textarea">{SAMPLE_BIO}</div>
              <p className="fake-label">Favorite genres</p>
              <div className="chips">
                {GENRES.map((genre) => (
                  <span
                    key={genre}
                    className={FAVORITE_GENRES.includes(genre) ? 'chip chip-selected' : 'chip'}
                  >
                    {genre}
                  </span>
                ))}
              </div>
              <div className="preview-actions">
                <span className="btn btn-primary">Save changes</span>
              </div>
            </div>
          </div>
        </section>
      )}
    </>
  )
}
