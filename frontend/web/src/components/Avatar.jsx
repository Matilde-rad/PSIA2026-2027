import { useState } from 'react'

// notionists is black line art on a transparent background, so the tints keep it visible on the dark theme
const DICEBEAR = 'https://api.dicebear.com/9.x/notionists/svg'
const TINTS = 'd7ecd4,c3e2bf,e8f0e9'

export function initials(name) {
  return name
    .split(/[_-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
}

export default function Avatar({ name, large }) {
  // the name whose image failed, so moving to another profile tries the image again
  const [failedName, setFailedName] = useState(null)
  return (
    <div className={large ? 'avatar avatar-lg' : 'avatar'} aria-hidden="true">
      {failedName === name ? (
        initials(name)
      ) : (
        <img
          src={`${DICEBEAR}?seed=${encodeURIComponent(name)}&backgroundColor=${TINTS}`}
          alt=""
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setFailedName(name)}
        />
      )}
    </div>
  )
}
