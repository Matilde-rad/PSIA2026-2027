import { useLayoutEffect, useRef } from 'react'
import { useNavigationType, useSearchParams } from 'react-router-dom'
import { loadScreen, saveScreen } from '../screenCache.js'
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

  const navigationType = useNavigationType()

  // A layout effect: the restored content is already rendered in this commit, so there is no flash at
  // the top. A tab switch leaves the scroll alone.
  useLayoutEffect(() => {
    const saved = loadScreen('home-scroll')
    if (navigationType === 'POP' && saved?.tab === tab) window.scrollTo(0, saved.y)
  }, [])

  // Layout-effect cleanups run in the commit that removes Home, before the shorter next page clamps
  // the scroll and fires a scroll event that would overwrite the ref.
  const lastY = useRef(0)
  useLayoutEffect(() => {
    lastY.current = window.scrollY
    const onScroll = () => {
      lastY.current = window.scrollY
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      window.removeEventListener('scroll', onScroll)
      saveScreen('home-scroll', { tab, y: lastY.current })
    }
  }, [tab])

  function select(id) {
    if (id === tab) return
    // replace keeps Home one history entry, so Back from a movie returns to the same tab and search.
    if (id === 'search') {
      // the switch drops q/genre; put the cached query back so Search's text, chips and results stay
      const key = loadScreen('search')?.result?.key
      const next = new URLSearchParams(key)
      next.set('tab', 'search')
      setParams(next, { replace: true })
      return
    }
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
