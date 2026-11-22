import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import { useI18n } from '../i18n'
import type { Post } from '../types'
import { Avatar, useFormat } from './ui'

export default function PostCard({ post, full = false }: { post: Post; full?: boolean }) {
  const { t } = useI18n()
  const { user } = useAuth()
  const fmt = useFormat()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const refresh = () => { qc.invalidateQueries({ queryKey: ['posts'] }); qc.invalidateQueries({ queryKey: ['post', post.id] }) }

  const like = useMutation({
    mutationFn: () => api.post(`/posts/${post.id}/like`),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: () => api.del(`/posts/${post.id}`),
    onSuccess: () => { refresh(); if (full) navigate('/feed') },
  })
  const canDelete = user && (user.id === post.author.id || user.role === 'ADMIN')

  return (
    <article className="post">
      <header className="post-head">
        <Avatar name={post.author.displayName} />
        <div>
          <Link to={`/u/${post.author.username}`} className="author">{post.author.displayName}</Link>
          <time className="muted" dateTime={post.createdAt}>{fmt.date(post.createdAt)}</time>
        </div>
        {post.source === 'HANDWRITTEN' && <span className="chip hand">{t('post.handwritten')}</span>}
      </header>
      {post.title && <h3 className="post-title" dir="auto">{full ? post.title : <Link to={`/posts/${post.id}`}>{post.title}</Link>}</h3>}
      <p className="post-body" dir="auto">{full || post.body.length < 420 ? post.body : post.body.slice(0, 420) + '…'}</p>
      {post.item && <Link to={`/catalog/${post.item.id}`} className="chip">{t('post.about')}: {post.item.title}</Link>}
      <footer className="post-actions">
        <button className={`act${post.likedByMe ? ' on' : ''}`} aria-pressed={post.likedByMe}
                onClick={() => (user ? like.mutate() : navigate('/login'))}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill={post.likedByMe ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21.2l7.8-7.7 1-1.1a5.5 5.5 0 0 0 0-7.8z" />
          </svg>
          <span>{post.likeCount}</span><span className="sr">{t('post.like')}</span>
        </button>
        <Link className="act" to={`/posts/${post.id}`}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="M21 12a8 8 0 0 1-11.5 7.2L3 21l1.8-6.2A8 8 0 1 1 21 12z" /></svg>
          <span>{post.commentCount}</span><span className="sr">{t('post.comments')}</span>
        </Link>
        {canDelete && (
          <button className="act danger" onClick={() => { if (confirm(t('post.confirmDelete'))) remove.mutate() }}>
            {t('post.delete')}
          </button>
        )}
      </footer>
    </article>
  )
}
