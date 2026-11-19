import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { HealthStatus } from './HealthStatus'

function renderWithQueryClient() {
  // retry: false so a failed request shows the error state immediately instead of retrying 3 times
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <HealthStatus />
    </QueryClientProvider>,
  )
}

describe('HealthStatus', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows that the server works when health is UP', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({ status: 'UP' })))

    renderWithQueryClient()

    expect(await screen.findByText('Serverul funcționează')).toBeInTheDocument()
  })

  it('shows an error when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 503 })))

    renderWithQueryClient()

    expect(await screen.findByText('Serverul nu răspunde')).toBeInTheDocument()
  })
})
