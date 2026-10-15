export const SESSION_KEY = 'g2.session'

export function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY))
  } catch {
    return null
  }
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export class ApiError extends Error {
  constructor(status, code, message, fields = []) {
    super(message)
    this.status = status
    this.code = code
    this.fields = fields
  }
}

export async function apiFetch(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' }
  if (body) headers['Content-Type'] = 'application/json'
  const session = loadSession()
  if (auth && session) headers.Authorization = `Bearer ${session.token}`

  let res
  try {
    res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  } catch {
    throw new ApiError(0, 'network', 'Something went wrong, try again.')
  }

  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = null
  }

  if (res.status === 401 && auth) {
    clearSession()
    window.location.assign('/login?expired=1')
    throw new ApiError(401, 'unauthorized', 'Your session expired, please log in again.')
  }
  if (!res.ok) {
    throw new ApiError(
      res.status,
      data?.error ?? 'unknown',
      data?.message ?? 'Something went wrong, try again.',
      data?.fields ?? [],
    )
  }
  return data
}

export function fieldMessages(error) {
  const messages = {}
  for (const { field, message } of error.fields) {
    messages[field] = messages[field] ? `${messages[field]} ${message}` : message
  }
  return messages
}
