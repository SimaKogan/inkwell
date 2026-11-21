import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { useAuth } from '../auth'
import Composer from '../components/Composer'
import ItemForm from '../components/ItemForm'
import PostList from '../components/PostList'
import { ErrorNote, Spinner, categoryHue, useFormat } from '../components/ui'
import { useI18n } from '../i18n'
import type { Item } from '../types'

export default function ItemDetail() {
  const { id } = useParams()
  const itemId = Number(id)
  const { t } = useI18n()
  const { user } = useAuth()
  const fmt = useFormat()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [editing, setEditing] = useState(false)

  const q = useQuery({ queryKey: ['item', itemId], queryFn: () => api.get<Item>(`/catalog/${itemId}`) })
  const remove = useMutation({
    mutationFn: () => api.del(`/catalog/${itemId}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['catalog'] }); navigate('/catalog') },
  })

  if (q.isPending) return <div className="container"><Spinner /></div>
  if (q.isError) return <div className="container"><ErrorNote error={q.error} /></div>
  const item = q.data
  const canEdit = user && (user.id === item.owner.id || user.role === 'ADMIN')

  return (
    <div className="container detail">
      <article className="item-card" style={{ ['--tab' as string]: `hsl(${categoryHue(item.category)} 55% 48%)` }}>
        <span className="tile-cat">{item.category}</span>
        <h1 dir="auto">{item.title}</h1>
        {item.authors.length > 0 && <p className="tile-authors" dir="auto">{item.authors.join(', ')}</p>}
        {item.description && <p className="item-desc" dir="auto">{item.description}</p>}
        <dl className="facts">
          <div><dt>{t('form.price')}</dt><dd>{fmt.price(item.price) || '—'}</dd></div>
          <div><dt>{t('form.stock')}</dt><dd>{item.stock}</dd></div>
          {item.publishedYear != null && <div><dt>{t('form.year')}</dt><dd>{item.publishedYear}</dd></div>}
          {item.isbn && <div><dt>{t('form.isbn')}</dt><dd>{item.isbn}</dd></div>}
        </dl>
        {canEdit && (
          <div className="row">
            <button className="btn ghost" onClick={() => setEditing(true)}>{t('item.edit')}</button>
            <button className="btn ghost danger" onClick={() => { if (confirm(t('item.confirmDelete'))) remove.mutate() }}>{t('item.delete')}</button>
          </div>
        )}
      </article>

      <section className="discussion">
        <h2>{t('item.discussion')}</h2>
        {user ? <Composer itemId={itemId} /> : <p className="muted"><Link to="/login">{t('common.signin')}</Link>. {t('common.loginRequired')}</p>}
        <PostList itemId={itemId} emptyText={t('item.noPosts')} />
      </section>
      {editing && <ItemForm item={item} onClose={() => setEditing(false)} />}
    </div>
  )
}
