import type { AuthResponse } from './types'

const TOKENS_KEY = 'inkwell.tokens'
interface Tokens { accessToken: string; refreshToken: string }

export const tokenStore = {
  get(): Tokens | null {
    try { return JSON.parse(localStorage.getItem(TOKENS_KEY) ?? 'null') as Tokens | null } catch { return null }
  },
  set(t: Tokens) { localStorage.setItem(TOKENS_KEY, JSON.stringify(t)) },
  clear() { localStorage.removeItem(TOKENS_KEY) },
}

export class ApiError extends Error {
  constructor(public status: number, message: string, public fields?: Record<string, string>) { super(message) }
}

let refreshing: Promise<boolean> | null = null

function refresh(): Promise<boolean> {
  const t = tokenStore.get()
  if (!t) return Promise.resolve(false)
  refreshing ??= (async () => {
    try {
      const r = await fetch('/api/auth/refresh', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: t.refreshToken }),
      })
      if (!r.ok) { tokenStore.clear(); return false }
      const data = (await r.json()) as AuthResponse
      tokenStore.set({ accessToken: data.accessToken, refreshToken: data.refreshToken })
      return true
    } catch { return false } finally { setTimeout(() => { refreshing = null }, 0) }
  })()
  return refreshing
}

async function request<T>(method: string, path: string, body?: unknown, retry = true): Promise<T> {
  const headers: Record<string, string> = {}
  const tokens = tokenStore.get()
  if (tokens) headers.Authorization = `Bearer ${tokens.accessToken}`
  let payload: BodyInit | undefined
  if (body instanceof FormData) payload = body
  else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body) }

  const res = await fetch('/api' + path, { method, headers, body: payload })
  if (res.status === 401 && tokens) {
    if (retry && (await refresh())) return request<T>(method, path, body, false)
    window.dispatchEvent(new Event('inkwell:logout'))
  }
  if (!res.ok) {
    let message = res.statusText
    let fields: Record<string, string> | undefined
    try {
      const p = (await res.json()) as { detail?: string; errors?: Record<string, string> }
      message = p.detail ?? message
      fields = p.errors
    } catch { /* non-JSON error body */ }
    throw new ApiError(res.status, message, fields)
  }
  if (res.status === 204) return undefined as T
  const type = res.headers.get('content-type') ?? ''
  return (type.includes('json') ? res.json() : res.text()) as Promise<T>
}

export const api = {
  get: <T,>(path: string) => request<T>('GET', path),
  post: <T,>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T,>(path: string, body?: unknown) => request<T>('PUT', path, body),
  del: <T = void,>(path: string) => request<T>('DELETE', path),
}

export const qs = (params: Record<string, string | number | undefined | null>) => {
  const u = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') u.set(k, String(v)) })
  const s = u.toString()
  return s ? `?${s}` : ''
}
