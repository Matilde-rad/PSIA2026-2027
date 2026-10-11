import { useState } from 'react'
import ForYou from './ForYou.jsx'
import Search from './Search.jsx'

const TABS = [
  { id: 'for-you', label: 'For you' },
  { id: 'search', label: 'Search' },
]

export default function HomePage() {
  const [tab, setTab] = useState('for-you')
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
            onClick={() => setTab(id)}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === 'for-you' ? <ForYou /> : <Search />}
    </>
  )
}
