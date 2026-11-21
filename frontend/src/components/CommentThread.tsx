import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import { useI18n } from '../i18n'
import type { Comment } from '../types'
import { Avatar, Empty, ErrorNote, Spinner, useFormat } from './ui'

interface Node extends Comment { children: Node[] }

function buildTree(flat: Comment[]): Node[] {
  const byId = new Map<number, Node>(flat.map((c) => [c.id, { ...c, children: [] }]))
  const roots: Node[] = []
  byId.forEach((n) => {
    const parent = n.parentId ? byId.get(n.parentId) : undefined
    if (parent) parent.children.push(n); else roots.push(n)
  })
  return roots
}

function CommentForm({ postId, parentId, onDone, autoFocus }: { postId: number; parentId?: number; onDone?: () => void; autoFocus?: boolean }) {
  const { t } = useI18n()
  const qc = useQueryClient()
  const [body, setBody] = useState('')
  const send = useMutation({
    mutationFn: () => api.post(`/posts/${postId}/comments`, { body, parentId }),
    onSuccess: () => {
      setBody('')
      qc.invalidateQueries({ queryKey: ['comments', postId] })
      qc.invalidateQueries({ queryKey: ['posts'] })
      qc.invalidateQueries({ queryKey: ['post', postId] })
      onDone?.()
    },
  })
  const submit = (e: FormEvent) => { e.preventDefault(); if (body.trim()) send.mutate() }
  return (
    <form className="comment-form" onSubmit={submit}>
      <textarea className="textarea" rows={2} value={body} dir="auto" autoFocus={autoFocus} maxLength={4000}
                onChange={(e) => setBody(e.target.value)} placeholder={t('comments.placeholder')} required />
      {send.isError && <ErrorNote error={send.error} />}
      <button className="btn primary small" disabled={send.isPending || !body.trim()}>{t('comments.send')}</button>
    </form>
  )
}

function CommentNode({ node, postId, depth }: { node: Node; postId: number; depth: number }) {
  const { t } = useI18n()
  const { user } = useAuth()
  const fmt = useFormat()
  const qc = useQueryClient()
  const [replying, setReplying] = useState(false)
  const remove = useMutation({
    mutationFn: () => api.del(`/comments/${node.id}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['comments', postId] }); qc.invalidateQueries({ queryKey: ['posts'] }) },
  })
  return (
    <li className="comment">
      <div className="comment-head">
        <Avatar name={node.author.displayName} />
        <Link to={`/u/${node.author.username}`} className="author">{node.author.displayName}</Link>
        <time className="muted" dateTime={node.createdAt}>{fmt.date(node.createdAt)}</time>
      </div>
      <p className="comment-body">{node.body}</p>
      <div className="comment-actions">
        {user && <button className="link" onClick={() => setReplying((v) => !v)}>{t('comments.reply')}</button>}
        {user && (user.id === node.author.id || user.role === 'ADMIN') && (
          <button className="link danger" onClick={() => remove.mutate()}>{t('comments.delete')}</button>
        )}
      </div>
      {replying && <CommentForm postId={postId} parentId={node.id} autoFocus onDone={() => setReplying(false)} />}
      {node.children.length > 0 && (
        <ul className={`replies${depth >= 3 ? ' flat' : ''}`}>
          {node.children.map((c) => <CommentNode key={c.id} node={c} postId={postId} depth={depth + 1} />)}
        </ul>
      )}
    </li>
  )
}

export default function CommentThread({ postId }: { postId: number }) {
  const { t } = useI18n()
  const { user } = useAuth()
  const q = useQuery({ queryKey: ['comments', postId], queryFn: () => api.get<Comment[]>(`/posts/${postId}/comments`) })
  const tree = useMemo(() => buildTree(q.data ?? []), [q.data])

  return (
    <section className="comments" aria-label={t('post.comments')}>
      <h2>{t('post.comments')}{q.data ? ` · ${q.data.length}` : ''}</h2>
      {user ? <CommentForm postId={postId} /> : <p className="muted"><Link to="/login">{t('common.signin')}</Link>. {t('comments.loginToComment')}</p>}
      {q.isPending && <Spinner />}
      {q.isError && <ErrorNote error={q.error} />}
      {q.data && tree.length === 0 && <Empty>{t('comments.empty')}</Empty>}
      <ul className="comment-list">{tree.map((n) => <CommentNode key={n.id} node={n} postId={postId} depth={0} />)}</ul>
    </section>
  )
}
