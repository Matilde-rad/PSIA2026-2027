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
  return (
    <div className={large ? 'avatar avatar-lg' : 'avatar'} aria-hidden="true">
      {initials(name)}
    </div>
  )
}
