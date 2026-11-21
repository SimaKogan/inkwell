import { useState, type FormEvent } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { useI18n, type Lang } from '../i18n'
import { Avatar, Logo } from './ui'

export default function Layout() {
  const { t, lang, setLang } = useI18n()
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [q, setQ] = useState('')

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (q.trim()) navigate(`/search?q=${encodeURIComponent(q.trim())}`)
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-inner">
          <Link to="/" className="brand"><Logo /><span>Inkwell</span></Link>
          <nav className="nav" aria-label="Main">
            <NavLink to="/catalog">{t('nav.catalog')}</NavLink>
            <NavLink to="/feed">{t('nav.feed')}</NavLink>
            <NavLink to="/studio">{t('nav.studio')}</NavLink>
          </nav>
          <form className="topsearch" onSubmit={submit} role="search">
            <input className="input" value={q} onChange={(e) => setQ(e.target.value)}
                   placeholder={t('nav.search')} aria-label={t('nav.search')} />
          </form>
          <select className="lang" value={lang} onChange={(e) => setLang(e.target.value as Lang)} aria-label="Language">
            <option value="ru">RU</option><option value="en">EN</option><option value="he">HE</option>
          </select>
          {user ? (
            <div className="account">
              <Link to={`/u/${user.username}`} className="me"><Avatar name={user.displayName} /><span>{user.displayName}</span></Link>
              <button className="btn ghost" onClick={logout}>{t('nav.logout')}</button>
            </div>
          ) : (
            <div className="account">
              <Link className="btn ghost" to="/login">{t('nav.login')}</Link>
              <Link className="btn primary" to="/register">{t('nav.register')}</Link>
            </div>
          )}
        </div>
      </header>
      <main className="main"><Outlet /></main>
      <footer className="footer"><span>Inkwell</span></footer>
    </div>
  )
}
