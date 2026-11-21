import type { ReactNode } from 'react'
import { useI18n } from '../i18n'
import { ApiError } from '../api'

export function Spinner() {
  const { t } = useI18n()
  return <p className="muted" role="status">{t('common.loading')}</p>
}

export function ErrorNote({ error }: { error: unknown }) {
  const { t } = useI18n()
  const message = error instanceof ApiError ? error.message : t('common.error')
  return <p className="notice error" role="alert">{message}</p>
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="empty">{children}</p>
}

export function Avatar({ name }: { name: string }) {
  const initial = [...name.trim()][0]?.toUpperCase() ?? '?'
  let h = 0
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) % 360
  return <span className="avatar" style={{ background: `hsl(${h} 45% 42%)` }} aria-hidden="true">{initial}</span>
}

export function categoryHue(category: string): number {
  let h = 7
  for (const ch of category) h = (h * 33 + ch.charCodeAt(0)) % 360
  return h
}

export function useFormat() {
  const { locale } = useI18n()
  return {
    date: (iso: string) => new Date(iso).toLocaleDateString(locale, { day: 'numeric', month: 'short', year: 'numeric' }),
    price: (n?: number) => n == null ? '' : new Intl.NumberFormat(locale, {
      style: 'currency', currency: import.meta.env.VITE_CURRENCY ?? 'EUR',
    }).format(n),
  }
}

export function Logo({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="var(--accent)" />
      <path d="M16 5.5 22.5 16 16 26.5 9.5 16z" fill="var(--on-accent)" />
      <circle cx="16" cy="15" r="1.7" fill="var(--accent)" />
      <path d="M16 16.5v5" stroke="var(--accent)" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}
