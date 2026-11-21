import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { ErrorNote } from '../components/ui'
import { useI18n } from '../i18n'

export default function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { t } = useI18n()
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const [identifier, setIdentifier] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true); setError(null)
    try {
      if (mode === 'login') await login(identifier, password)
      else await register(username, email, password, displayName)
      navigate('/')
    } catch (err) { setError(err) } finally { setBusy(false) }
  }

  return (
    <div className="container auth">
      <form className="auth-card" onSubmit={submit}>
        <h1>{mode === 'login' ? t('auth.login') : t('auth.register')}</h1>
        {mode === 'login' ? (
          <label className="field">{t('auth.identifier')}
            <input className="input" value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="username" required autoFocus /></label>
        ) : (
          <>
            <label className="field">{t('auth.username')}
              <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} minLength={3} maxLength={32} autoComplete="username" required autoFocus /></label>
            <label className="field">{t('auth.email')}
              <input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required /></label>
            <label className="field">{t('auth.displayName')}
              <input className="input" value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={80} /></label>
          </>
        )}
        <label className="field">{t('auth.password')}
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={mode === 'register' ? 8 : undefined}
                 autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required />
          {mode === 'register' && <span className="muted small">{t('auth.passwordHint')}</span>}</label>
        {error != null && <ErrorNote error={error} />}
        <button className="btn primary wide" disabled={busy}>{mode === 'login' ? t('auth.login') : t('auth.register')}</button>
        <p className="muted center">
          {mode === 'login' ? t('auth.noAccount') : t('auth.haveAccount')}{' '}
          <Link to={mode === 'login' ? '/register' : '/login'}>{mode === 'login' ? t('nav.register') : t('nav.login')}</Link>
        </p>
      </form>
    </div>
  )
}
