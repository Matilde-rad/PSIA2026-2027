import { useSearchParams } from 'react-router-dom'
import AskAi from './AskAi.jsx'
import CatalogSearch from './CatalogSearch.jsx'
import ForYou from './ForYou.jsx'

const TABS = [
  { id: 'for-you', label: 'For you' },
  { id: 'search', label: 'Search' },
  { id: 'ask-ai', label: 'Ask AI' },
]

export default function HomePage() {
  const [params, setParams] = useSearchParams()
  const requested = params.get('tab')
  const tab = TABS.some(({ id }) => id === requested) ? requested : 'for-you'

  function select(id) {
    if (id === tab) return
    // replace keeps Home one history entry, so Back from a movie returns to the same tab and search.
    setParams(id === 'for-you' ? {} : { tab: id }, { replace: true })
  }

  return (
    <>
      <div className="tabs" role="tablist">
        {TABS.map(({ id, label }) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={tab === id}
            className={tab === id ? 'tab tab-active' : 'tab'}
            onClick={() => select(id)}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === 'for-you' && <ForYou />}
      {tab === 'search' && <CatalogSearch />}
      {tab === 'ask-ai' && <AskAi />}
    </>
  )
}
