// Matches recs.search.picks in recommendations: a page shorter than this is the last one.
export const PAGE_SIZE = 12

export function ResultsHeader({ title, subtitle, busy, refreshLabel, onRefresh }) {
  return (
    <div className="section-header">
      <div className="section-heading">
        <h2 className="section-title">{title}</h2>
        {subtitle && <p className="section-subtitle">{subtitle}</p>}
      </div>
      <button className="btn btn-secondary" type="button" disabled={busy !== null} onClick={onRefresh}>
        {busy === 'refresh' ? 'Refreshing…' : refreshLabel}
      </button>
    </div>
  )
}

export default function ResultsToolbar({ busy, exhausted, error, endText, onMore }) {
  return (
    <div className="results-toolbar">
      {exhausted ? (
        <span className="results-note">{endText}</span>
      ) : (
        <button className="btn btn-secondary" type="button" disabled={busy !== null} onClick={onMore}>
          {busy === 'more' ? 'Loading…' : 'Show more movies'}
        </button>
      )}
      {error && (
        <p className="field-error" role="alert">
          {error.status === 503 ? 'The AI is unavailable right now, try again.' : 'Something went wrong, try again.'}
        </p>
      )}
    </div>
  )
}
