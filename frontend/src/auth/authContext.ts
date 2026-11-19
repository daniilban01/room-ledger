import { createContext } from 'react'

export type AuthState = {
  token: string | null
  login: (token: string) => void
  logout: () => void
}

// Kept in its own file: the React Fast Refresh lint rule wants component files to export only components.
export const AuthContext = createContext<AuthState | null>(null)
