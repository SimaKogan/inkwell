import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api'
import ItemTile from '../components/ItemTile'
import PostCard from '../components/PostCard'
import { Empty, Spinner } from '../components/ui'
import { useI18n } from '../i18n'
import type { Item, PageResult, Post } from '../types'

export default function Home() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const items = useQuery({ queryKey: ['catalog', 'latest'], queryFn: () => api.get<PageResult<Item>>('/catalog?size=8') })
  const posts = useQuery({ queryKey: ['posts', 'latest'], queryFn: () => api.get<PageResult<Post>>('/posts?size=3') })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (q.trim()) navigate(`/search?q=${encodeURIComponent(q.trim())}`)
  }

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <h1>{t('home.title')}</h1>
          <p className="hero-sub">{t('home.sub')}</p>
          <form className="hero-search" onSubmit={submit} role="search">
            <input className="input big" value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('search.placeholder')}
                   aria-label={t('search.placeholder')} dir="auto" autoFocus />
            <button className="btn primary big">{t('search.go')}</button>
          </form>
          <p className="hint">{t('home.hint')}</p>
        </div>
      </section>

      <div className="container">
        <section className="section">
          <div className="section-head">
            <h2>{t('home.new')}</h2>
            <Link to="/catalog" className="more">{t('home.all')}</Link>
          </div>
          {items.isPending ? <Spinner /> : items.data && items.data.content.length > 0
            ? <div className="tiles">{items.data.content.map((i) => <ItemTile key={i.id} item={i} />)}</div>
            : <Empty>{t('catalog.empty')}</Empty>}
        </section>

        <section className="section">
          <div className="section-head">
            <h2>{t('home.discuss')}</h2>
            <Link to="/feed" className="more">{t('home.all')}</Link>
          </div>
          {posts.isPending ? <Spinner /> : posts.data && posts.data.content.length > 0
            ? <div className="post-list narrow">{posts.data.content.map((p) => <PostCard key={p.id} post={p} />)}</div>
            : <Empty>{t('feed.empty')}</Empty>}
        </section>
      </div>
    </>
  )
}
