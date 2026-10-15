import { useState } from 'react'
import { useNavigationType } from 'react-router-dom'

const screens = new Map()

export function saveScreen(id, state) {
  screens.set(id, state)
}

export function loadScreen(id) {
  return screens.get(id) ?? null
}

export function clearScreens() {
  screens.clear()
}


export function useSavedScreen(id) {
  const navigationType = useNavigationType()
  const [saved] = useState(() => (navigationType === 'PUSH' ? null : loadScreen(id)))
  return saved
}
