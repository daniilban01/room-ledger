export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

/**
 * Thin wrapper over fetch: parses JSON and turns non-2xx responses into ApiError,
 * so TanStack Query sees them as failed queries (fetch alone only rejects on network errors).
 */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })

  if (!response.ok) {
    throw new ApiError(response.status, `${init?.method ?? 'GET'} ${path} failed with ${response.status}`)
  }

  return (await response.json()) as T
}
