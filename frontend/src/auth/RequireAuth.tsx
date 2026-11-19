import { Navigate, Outlet } from 'react-router'
import { useAuth } from './useAuth'

/** Layout route: renders the protected child routes only when logged in. */
export function RequireAuth() {
  const { token } = useAuth()
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}
