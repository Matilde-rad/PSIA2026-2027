export default function Logo({ size = 26 }) {
  return (
    <svg className="logo" width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <path d="M3.5 15A5 5 0 0 1 9 9.5A7.5 7.5 0 0 1 22.5 8.5A5.5 5.5 0 0 1 28.5 15Z" fill="var(--accent)" />
      <path
        d="M16 25V18.5M16 19.5L12 15M16 19.5L20 15"
        fill="none"
        stroke="var(--accent)"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <rect x="7" y="24.5" width="18" height="2.5" rx="1.25" fill="var(--saved)" />
    </svg>
  )
}
