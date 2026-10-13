// Matches recs.search.picks in recommendations: a page shorter than this is the last one.
export const PAGE_SIZE = 12

export default function ResultsToolbar({ busy, exhausted, error, endText, onRefresh, onMore }) {
  return (
    <>
      <div className="results-toolbar">
        <button className="btn btn-secondary btn-sm" type="button" disabled={busy !== null} onClick={onRefresh}>
          {busy === 'refresh' ? 'Refreshing…' : 'Refresh'}
        </button>
        {exhausted ? (
          <span className="results-note">{endText}</span>
        ) : (
          <button className="btn btn-secondary btn-sm" type="button" disabled={busy !== null} onClick={onMore}>
            {busy === 'more' ? 'Loading…' : 'Get more'}
          </button>
        )}
      </div>
      {error && (
        <p className="field-error" role="alert">
          {error.status === 503 ? 'The AI is unavailable right now, try again.' : 'Something went wrong, try again.'}
        </p>
      )}
    </>
  )
}
