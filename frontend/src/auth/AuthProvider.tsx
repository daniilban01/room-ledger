import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setUnauthorizedHandler } from '../api/client'
import { AuthContext } from './authContext'
import { clearToken, getToken, saveToken } from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [token, setToken] = useState<string | null>(() => getToken())

  const login = useCallback((newToken: string) => {
    saveToken(newToken)
    setToken(newToken)
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setToken(null)
    // Drop cached server data so the next user never sees the previous user's groups.
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  const value = useMemo(() => ({ token, login, logout }), [token, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
