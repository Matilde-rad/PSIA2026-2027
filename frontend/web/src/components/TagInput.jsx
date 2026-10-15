import { useState } from 'react'

export default function TagInput({ label, hint, placeholder, values, onChange, max = 10 }) {
  const [text, setText] = useState('')
  const full = values.length >= max

  function add() {
    const value = text.trim().replace(/\s+/g, ' ')
    setText('')
    if (!value || full) return
    if (values.some((v) => v.toLowerCase() === value.toLowerCase())) return
    onChange([...values, value])
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      add()
    } else if (e.key === 'Backspace' && text === '' && values.length > 0) {
      onChange(values.slice(0, -1))
    }
  }

  return (
    <label className="field">
      <span className="field-label">{label}</span>
      <div className="tag-input">
        {values.map((value) => (
          <span key={value} className="chip tag">
            {value}
            <button
              type="button"
              className="tag-remove"
              aria-label={`Remove ${value}`}
              onClick={() => onChange(values.filter((v) => v !== value))}
            >
              ×
            </button>
          </span>
        ))}
        <input
          className="tag-text"
          value={text}
          maxLength={80}
          placeholder={full ? '' : placeholder}
          disabled={full}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={add}
        />
      </div>
      <span className="field-hint">{full ? `That's the maximum of ${max}.` : hint}</span>
    </label>
  )
}
