import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, Navigate } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { TextField } from '../ui/TextField'
import { authApi } from './api'
import { AuthLayout } from './AuthLayout'
import { useAuth } from './useAuth'

export function LoginPage() {
  const { token, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const loginMutation = useMutation({
    mutationFn: () => authApi.login(email, password),
    // Saving the token re-renders this page, and the <Navigate> below sends the user to their groups.
    onSuccess: (response) => login(response.accessToken),
  })

  if (token) {
    return <Navigate to="/" replace />
  }

  return (
    <AuthLayout title="Intră în cont">
      <Card>
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault()
            loginMutation.mutate()
          }}
        >
          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            label="Parolă"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <ErrorText
            message={loginMutation.isError ? errorMessage(loginMutation.error, { 401: 'Email sau parolă greșită.' }) : null}
          />
          <Button type="submit" disabled={loginMutation.isPending}>
            {loginMutation.isPending ? 'Se conectează…' : 'Intră în cont'}
          </Button>
        </form>
      </Card>
      <p className="text-center">
        Nu ai cont?{' '}
        <Link to="/register" className="font-medium text-accent underline-offset-2 hover:underline">
          Creează unul
        </Link>
      </p>
    </AuthLayout>
  )
}
