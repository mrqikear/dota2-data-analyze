const TOKEN_KEY = 'dota2_token'
const USER_KEY = 'dota2_user'

export function getToken() { return localStorage.getItem(TOKEN_KEY) }
export function setToken(t) { localStorage.setItem(TOKEN_KEY, t) }
export function removeToken() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY) }
export function setUser(u) { localStorage.setItem(USER_KEY, JSON.stringify(u)) }
export function getUser() {
  try { const r = localStorage.getItem(USER_KEY); return r ? JSON.parse(r) : null } catch { return null }
}
export function isLoggedIn() { return !!getToken() }
