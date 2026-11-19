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

export function RegisterPage() {
  const { token, login } = useAuth()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const registerMutation = useMutation({
    mutationFn: () => authApi.register(email, password, displayName),
    onSuccess: (response) => login(response.accessToken),
  })

  if (token) {
    return <Navigate to="/" replace />
  }

  return (
    <AuthLayout title="Creează cont">
      <Card>
        {/* The HTML constraints (required, minLength, maxLength) mirror the backend validation,
            so most mistakes are caught instantly, with browser messages in the user's language. */}
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault()
            registerMutation.mutate()
          }}
        >
          <TextField
            label="Nume"
            autoComplete="name"
            required
            maxLength={100}
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
          />
          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            required
            maxLength={255}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            label="Parolă (minim 8 caractere)"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <ErrorText
            message={
              registerMutation.isError
                ? errorMessage(registerMutation.error, { 409: 'Există deja un cont cu acest email.' })
                : null
            }
          />
          <Button type="submit" disabled={registerMutation.isPending}>
            {registerMutation.isPending ? 'Se creează contul…' : 'Creează cont'}
          </Button>
        </form>
      </Card>
      <p className="text-center">
        Ai deja cont?{' '}
        <Link to="/login" className="font-medium text-accent underline-offset-2 hover:underline">
          Intră în cont
        </Link>
      </p>
    </AuthLayout>
  )
}
