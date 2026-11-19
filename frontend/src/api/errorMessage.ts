import { ApiError } from './client'

/** Turns any request error into a Romanian message; callers map the statuses they expect. */
export function errorMessage(error: unknown, byStatus: Partial<Record<number, string>> = {}): string {
  if (error instanceof ApiError) {
    if (error.status === 400) {
      return byStatus[400] ?? 'Verifică datele introduse.'
    }
    return byStatus[error.status] ?? 'Ceva n-a mers. Încearcă din nou.'
  }
  return 'Nu ne putem conecta la server. Verifică conexiunea.'
}
