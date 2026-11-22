import { postJson } from '../api/client'

type TokenResponse = { accessToken: string }

export const authApi = {
  login: (email: string, password: string) =>
    postJson<TokenResponse>('/api/auth/login', { email, password }),

  demo: () => postJson<TokenResponse>('/api/auth/demo'),

  register: (email: string, password: string, displayName: string) =>
    postJson<TokenResponse>('/api/auth/register', { email, password, displayName }),
}
