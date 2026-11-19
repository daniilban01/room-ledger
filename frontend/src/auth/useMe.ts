import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'

export type Me = { id: string; email: string; displayName: string }

/** The logged-in user. Cached, and cleared together with everything else on logout. */
export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: () => apiFetch<Me>('/api/me'),
    staleTime: Infinity, // does not change during a session
  })
}
