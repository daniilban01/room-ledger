// localStorage keeps the user logged in across reloads. Trade-off: any injected script (XSS)
// could read it. An httpOnly cookie would avoid that but brings back CSRF and server-side work.
const TOKEN_KEY = 'roomledger.token'

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null // storage blocked (e.g. some private modes)
  }
}

export function saveToken(token: string): void {
  try {
    localStorage.setItem(TOKEN_KEY, token)
  } catch {
    // storage blocked: the user stays logged in only until the page is reloaded
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(TOKEN_KEY)
  } catch {
    // nothing stored, nothing to clear
  }
}
