import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth'
import Composer from '../components/Composer'
import PostList from '../components/PostList'
import { useI18n } from '../i18n'

export default function Feed() {
  const { t } = useI18n()
  const { user } = useAuth()
  const [tab, setTab] = useState<'all' | 'following'>('all')

  return (
    <div className="container narrow">
      <div className="page-head"><h1>{t('feed.title')}</h1></div>
      {user
        ? <Composer />
        : <p className="muted"><Link to="/login">{t('common.signin')}</Link>. {t('common.loginRequired')}</p>}
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === 'all'} className={`tab${tab === 'all' ? ' on' : ''}`} onClick={() => setTab('all')}>{t('feed.all')}</button>
        {user && <button role="tab" aria-selected={tab === 'following'} className={`tab${tab === 'following' ? ' on' : ''}`} onClick={() => setTab('following')}>{t('feed.following')}</button>}
      </div>
      {tab === 'following' && user
        ? <PostList feed emptyText={t('feed.followingEmpty')} />
        : <PostList />}
    </div>
  )
}
