import { getToken } from '../auth/tokenStorage'

export type FieldErrors = Record<string, string>

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldErrors

  constructor(status: number, message: string, fieldErrors: FieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

// Shape of the backend's RFC 7807 ProblemDetail responses (only the fields we use).
type Problem = { detail?: string; errors?: FieldErrors }

let unauthorizedHandler: (() => void) | null = null

/** AuthProvider registers its logout here, so an expired token logs the user out from any request. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

/**
 * Thin wrapper over fetch: adds the JWT, parses JSON and turns non-2xx responses into ApiError,
 * so TanStack Query sees them as failures (fetch alone only rejects on network errors).
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  // Only JSON bodies (strings) get this header. For FormData (file uploads) the browser must set
  // Content-Type itself, because it includes the multipart "boundary" that separates the parts.
  if (typeof init.body === 'string') {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, { ...init, headers })

  if (!response.ok) {
    const problem = await readProblem(response)
    // Only when we sent a token: a 401 from the login form just means a wrong password.
    if (response.status === 401 && token) {
      unauthorizedHandler?.()
    }
    throw new ApiError(response.status, problem?.detail ?? `Request failed with ${response.status}`, problem?.errors)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function postJson<T>(path: string, body?: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

async function readProblem(response: Response): Promise<Problem | null> {
  try {
    return (await response.json()) as Problem
  } catch {
    return null // empty or non-JSON error body
  }
}
