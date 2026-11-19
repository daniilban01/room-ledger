import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, setUnauthorizedHandler } from './client'

describe('apiFetch', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
    localStorage.clear()
    setUnauthorizedHandler(null)
  })

  it('sends the stored token as a Bearer header', async () => {
    localStorage.setItem('roomledger.token', 'abc')
    fetchMock.mockResolvedValue(Response.json({ ok: true }))

    await apiFetch('/api/me')

    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer abc')
  })

  it('turns a ProblemDetail response into an ApiError', async () => {
    fetchMock.mockResolvedValue(
      Response.json({ detail: 'Validation failed', errors: { name: 'must not be blank' } }, { status: 400 }),
    )

    const error = await apiFetch('/api/groups').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Validation failed', fieldErrors: { name: 'must not be blank' } })
  })

  it('logs out on 401 when a token was sent', async () => {
    localStorage.setItem('roomledger.token', 'expired')
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }))

    await expect(apiFetch('/api/me')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('does not log out on 401 without a token (wrong password on login)', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }))

    await expect(apiFetch('/api/auth/login', { method: 'POST' })).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).not.toHaveBeenCalled()
  })
})
