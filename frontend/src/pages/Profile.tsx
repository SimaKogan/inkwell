import { useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import PostList from '../components/PostList'
import { Avatar, ErrorNote, Spinner, useFormat } from '../components/ui'
import { useI18n } from '../i18n'
import type { Profile as ProfileData, User } from '../types'

export default function Profile() {
  const { username = '' } = useParams()
  const { t } = useI18n()
  const { user, setUser } = useAuth()
  const fmt = useFormat()
  const qc = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [displayName, setDisplayName] = useState('')
  const [bio, setBio] = useState('')

  const q = useQuery({ queryKey: ['profile', username], queryFn: () => api.get<ProfileData>(`/users/${username}`) })
  const follow = useMutation({
    mutationFn: () => api.post(`/users/${username}/follow`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['profile', username] }); qc.invalidateQueries({ queryKey: ['posts'] }) },
  })
  const save = useMutation({
    mutationFn: () => api.put<User>('/users/me', { displayName, bio }),
    onSuccess: (u) => { setUser(u); setEditing(false); qc.invalidateQueries({ queryKey: ['profile', username] }) },
  })

  if (q.isPending) return <div className="container"><Spinner /></div>
  if (q.isError) return <div className="container"><ErrorNote error={q.error} /></div>
  const p = q.data
  const mine = user?.id === p.user.id

  const startEdit = () => { setDisplayName(p.user.displayName); setBio(p.user.bio ?? ''); setEditing(true) }
  const submit = (e: FormEvent) => { e.preventDefault(); save.mutate() }

  return (
    <div className="container narrow">
      <header className="profile-head">
        <Avatar name={p.user.displayName} />
        <div className="grow">
          <h1 dir="auto">{p.user.displayName}</h1>
          <p className="muted">@{p.user.username} · {fmt.date(p.user.createdAt)}</p>
          {p.user.bio && <p dir="auto">{p.user.bio}</p>}
          <p className="counts">
            <span><strong>{p.posts}</strong> {t('profile.posts')}</span>
            <span><strong>{p.followers}</strong> {t('profile.followers')}</span>
            <span><strong>{p.following}</strong> {t('profile.following')}</span>
          </p>
        </div>
        {user && !mine && (
          <button className={`btn ${p.followedByMe ? 'ghost' : 'primary'}`} onClick={() => follow.mutate()} disabled={follow.isPending}>
            {p.followedByMe ? t('profile.unfollow') : t('profile.follow')}
          </button>
        )}
        {mine && !editing && <button className="btn ghost" onClick={startEdit}>{t('profile.edit')}</button>}
      </header>

      {editing && (
        <form className="card-form" onSubmit={submit}>
          <label className="field">{t('profile.displayName')}
            <input className="input" value={displayName} maxLength={80} required onChange={(e) => setDisplayName(e.target.value)} /></label>
          <label className="field">{t('profile.bio')}
            <textarea className="textarea" rows={3} value={bio} maxLength={500} onChange={(e) => setBio(e.target.value)} /></label>
          {save.isError && <ErrorNote error={save.error} />}
          <div className="row end">
            <button type="button" className="btn ghost" onClick={() => setEditing(false)}>{t('common.cancel')}</button>
            <button className="btn primary" disabled={save.isPending}>{t('profile.save')}</button>
          </div>
        </form>
      )}
      <PostList authorId={p.user.id} emptyText={t('profile.noPosts')} />
    </div>
  )
}
