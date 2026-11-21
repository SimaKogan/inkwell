import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, tokenStore } from './api'
import type { AuthResponse, User } from './types'

interface AuthState {
  user: User | null
  loading: boolean
  login: (identifier: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string, displayName: string) => Promise<void>
  logout: () => void
  setUser: (u: User) => void
}

const Ctx = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)
  const qc = useQueryClient()

  useEffect(() => {
    if (!tokenStore.get()) return
    api.get<User>('/auth/me').then(setUser).catch(() => tokenStore.clear()).finally(() => setLoading(false))
  }, [])

  const logout = useCallback(() => { tokenStore.clear(); setUser(null); qc.clear() }, [qc])
  useEffect(() => {
    window.addEventListener('inkwell:logout', logout)
    return () => window.removeEventListener('inkwell:logout', logout)
  }, [logout])

  const accept = useCallback((r: AuthResponse) => {
    tokenStore.set({ accessToken: r.accessToken, refreshToken: r.refreshToken })
    setUser(r.user)
    qc.invalidateQueries()
  }, [qc])

  const value = useMemo<AuthState>(() => ({
    user, loading, logout, setUser,
    login: async (identifier, password) => accept(await api.post<AuthResponse>('/auth/login', { identifier, password })),
    register: async (username, email, password, displayName) =>
      accept(await api.post<AuthResponse>('/auth/register', { username, email, password, displayName })),
  }), [user, loading, logout, accept])

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useAuth(): AuthState {
  const v = useContext(Ctx)
  if (!v) throw new Error('useAuth must be used inside AuthProvider')
  return v
}
